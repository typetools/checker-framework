package org.checkerframework.common.basetype;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

/**
 * For methods annotated with {@link SideEffectsOnly}, computes expressions that are side-effected
 * but not permitted by the annotation.
 */
public class SideEffectsOnlyChecker {

  /** Do not instantiate. */
  private SideEffectsOnlyChecker() {
    throw new Error("Do not instantiate");
  }

  /**
   * Returns the computed {@code ExtraSideEffects}.
   *
   * @param statement The statement to check
   * @param annoProvider The annotation provider
   * @param sideEffectsOnlyExpressions List of JavaExpressions that are provided as annotation
   *     values to {@link SideEffectsOnly}
   * @param processingEnv The processing environment
   * @param checker The checker to use
   * @return a ExtraSideEffects
   */
  public static ExtraSideEffects checkSideEffectsOnly(
      TreePath statement,
      AnnotationProvider annoProvider,
      List<JavaExpression> sideEffectsOnlyExpressions,
      ProcessingEnvironment processingEnv,
      BaseTypeChecker checker) {
    SideEffectsOnlyCheckerHelper helper =
        new SideEffectsOnlyCheckerHelper(
            annoProvider, sideEffectsOnlyExpressions, processingEnv, checker);
    helper.scan(statement, null);
    return helper.extraSideEffects;
  }

  /**
   * The set of expressions a method side-effects, beyond those specified its {@link
   * SideEffectsOnly} annotation.
   */
  public static class ExtraSideEffects {

    /** Creates an empty ExtraSideEffects. */
    public ExtraSideEffects() {}

    /**
     * List of expressions a method side-effects that are not specified in the list of arguments to
     * {@link SideEffectsOnly}.
     */
    protected final List<IPair<Tree, JavaExpression>> exprs = new ArrayList<>(1);

    /**
     * Adds {@code t} and {@code javaExpr} as a pair to this.
     *
     * @param t the expression that is mutated
     * @param javaExpr the corresponding Java expression
     */
    public void addExpr(Tree t, JavaExpression javaExpr) {
      exprs.add(IPair.of(t, javaExpr));
    }

    /**
     * Returns a list of expressions a method side-effects that are not specified in the list of
     * arguments to {@link SideEffectsOnly}.
     *
     * @return side-effected expressions, beyond what is in {@code @SideEffectsOnly}.
     */
    public List<IPair<Tree, JavaExpression>> getExprs() {
      return exprs;
    }
  }

  /**
   * Class that visits that visits various nodes and computes mutated expressions that are not
   * specified as annotation values to {@link SideEffectsOnly}.
   */
  protected static class SideEffectsOnlyCheckerHelper extends TreePathScanner<Void, Void> {
    /** Result computed by SideEffectsOnlyCheckerHelper. */
    ExtraSideEffects extraSideEffects = new ExtraSideEffects();

    /**
     * List of expressions specified as annotation arguments in {@link SideEffectsOnly} annotation.
     */
    List<JavaExpression> sideEffectsOnlyExpressions;

    /** The annotation provider. */
    protected final AnnotationProvider annoProvider;

    /** The processing environment. */
    ProcessingEnvironment processingEnv;

    /** The checker to use. */
    BaseTypeChecker checker;

    /**
     * Constructor for SideEffectsOnlyCheckerHelper.
     *
     * @param annoProvider The annotation provider
     * @param sideEffectsOnlyExpressions List of JavaExpressions that are provided as annotation
     *     values to {@link SideEffectsOnly}
     * @param processingEnv The processing environment
     * @param checker The checker to use
     */
    public SideEffectsOnlyCheckerHelper(
        AnnotationProvider annoProvider,
        List<JavaExpression> sideEffectsOnlyExpressions,
        ProcessingEnvironment processingEnv,
        BaseTypeChecker checker) {
      this.annoProvider = annoProvider;
      this.sideEffectsOnlyExpressions = sideEffectsOnlyExpressions;
      this.processingEnv = processingEnv;
      this.checker = checker;
    }

    @Override
    public Void visitCatch(CatchTree node, Void aVoid) {
      return super.visitCatch(node, aVoid);
    }

    @Override
    // TODO: Similar logic for NewClassTree?
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      Element invokedElem = TreeUtils.elementFromUse(node);
      AnnotationMirror pureAnno = annoProvider.getDeclAnnotation(invokedElem, Pure.class);
      AnnotationMirror sideEffectFreeAnno =
          annoProvider.getDeclAnnotation(invokedElem, SideEffectFree.class);
      // If the invoked method has no side effects, there is, nothing to do.
      if (pureAnno != null || sideEffectFreeAnno != null) {
        return super.visitMethodInvocation(node, aVoid);
      }

      MethodTree enclosingMethod =
          TreePathUtil.enclosingMethod(checker.getTypeFactory().getPath(node));
      ExecutableElement enclosingMethodElement = null;
      if (enclosingMethod != null) {
        enclosingMethodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
      }
      AnnotationMirror sideEffectsOnlyAnno = null;
      if (enclosingMethodElement != null) {
        annoProvider.getDeclAnnotation(enclosingMethodElement, SideEffectsOnly.class);
      }
      System.out.printf(
          "invokedElem = %s, sideEffectsOnlyAnno = %s%n", invokedElem, sideEffectsOnlyAnno);

      // The arguments to @SideEffectsOnly, or an empty list if there is no @SideEffectsOnly.
      List<String> sideEffectsOnlyExpressionStrings;
      if (sideEffectsOnlyAnno == null) {
        sideEffectsOnlyExpressionStrings = Collections.emptyList();
      } else {
        ExecutableElement sideEffectsOnlyValueElement =
            TreeUtils.getMethod(SideEffectsOnly.class, "value", 0, processingEnv);
        sideEffectsOnlyExpressionStrings =
            AnnotationUtils.getElementValueArray(
                sideEffectsOnlyAnno, sideEffectsOnlyValueElement, String.class);
      }

      // TODO: This needs to collect all subexpressions of the given expression.  For now it just
      // considers the actual arguments, which is incomplete.
      List<? extends ExpressionTree> args = node.getArguments();
      ExpressionTree receiver = TreeUtils.getReceiverTree(node);
      List<ExpressionTree> subexpressions;
      if (receiver == null) {
        subexpressions = new ArrayList<>(args);
      } else {
        subexpressions = new ArrayList<>(args.size() + 1);
        subexpressions.add(receiver);
        subexpressions.addAll(args);
      }

      if (sideEffectsOnlyAnno == null) {
        System.out.printf("Error 1%n");
        // For each expression in `node`:
        checker.reportError(node, "purity.incorrect.sideeffectsonly", node);
      } else {
        // The invoked method is annotated with @SideEffectsOnly.
        // Add annotation values to seOnlyIncorrectExprs
        // that are not present in sideEffectsOnlyExpressions.
        /* TODO
        ExecutableElement sideEffectsOnlyValueElement =
            TreeUtils.getMethod(SideEffectsOnly.class, "value", 0, processingEnv);
        */
        List<JavaExpression> sideEffectsOnlyExprInv = new ArrayList<>();
        for (String st : sideEffectsOnlyExpressionStrings) {
          try {
            JavaExpression exprJe = StringToJavaExpression.atMethodInvocation(st, node, checker);
            sideEffectsOnlyExprInv.add(exprJe);
          } catch (JavaExpressionParseUtil.JavaExpressionParseException ex) {
            checker.report(st, ex.getDiagMessage());
          }
        }

        for (JavaExpression expr : sideEffectsOnlyExprInv) {
          if (!sideEffectsOnlyExpressions.contains(expr)) {
            extraSideEffects.addExpr(node, expr);
          }
        }
      }
      return super.visitMethodInvocation(node, aVoid);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void aVoid) {
      return super.visitNewClass(node, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
      JavaExpression javaExpr = JavaExpression.fromTree(node.getVariable());
      if (!sideEffectsOnlyExpressions.contains(javaExpr)) {
        extraSideEffects.addExpr(node, javaExpr);
      }
      return super.visitAssignment(node, aVoid);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void aVoid) {
      return super.visitUnary(node, aVoid);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void aVoid) {
      return super.visitCompoundAssignment(node, aVoid);
    }
  }
}
