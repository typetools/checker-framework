package org.checkerframework.common.basetype;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.javacutil.AnnotationProvider;
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
   * Class that visits various nodes and computes mutated expressions that are not specified as
   * annotation values to {@link SideEffectsOnly}.
   */
  protected static class SideEffectsOnlyCheckerHelper extends TreePathScanner<Void, Void> {
    /** Result computed by SideEffectsOnlyCheckerHelper. */
    ExtraSideEffects extraSideEffects = new ExtraSideEffects();

    /**
     * List of expressions specified as annotation arguments in {@link SideEffectsOnly} annotation.
     */
    List<JavaExpression> sideEffectsOnlyExpressionsFromAnnotation;

    /** Map of expressions that are aliased to other expressions. */
    Map<JavaExpression, JavaExpression> aliasedExpressions;

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
      this.aliasedExpressions = new HashMap<>();
      this.annoProvider = annoProvider;
      this.sideEffectsOnlyExpressionsFromAnnotation = sideEffectsOnlyExpressions;
      this.processingEnv = processingEnv;
      this.checker = checker;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      Element invokedElem = TreeUtils.elementFromUse(node);
      boolean isMarkedPure = annoProvider.getDeclAnnotation(invokedElem, Pure.class) != null;
      boolean isMarkedSideEffectFree =
          annoProvider.getDeclAnnotation(invokedElem, SideEffectFree.class) != null;
      if (isMarkedPure || isMarkedSideEffectFree) {
        return super.visitMethodInvocation(node, aVoid);
      }

      AnnotationMirror sideEffectsOnlyAnnotationOnEnclosingMethod =
          getSideEffectsOnlyAnnotationOnEnclosingMethod(node);
      assert sideEffectsOnlyAnnotationOnEnclosingMethod != null
          : "This method should only be invoked when the @SideEffectsOnly annotation is not null";

      boolean isInvokedMethodMarkedWithSideEffectsOnly =
          annoProvider.getDeclAnnotation(invokedElem, SideEffectsOnly.class) != null;

      List<JavaExpression> actualSideEffectedExprs =
          this.getJavaExpressionsFromMethodInvocation(node);

      // If the invoked method is NOT marked with @SideEffectsOnly, it may modify anything
      if (!isInvokedMethodMarkedWithSideEffectsOnly) {
        // What does it modify? Check the arguments for the method invocation
        if (actualSideEffectedExprs.isEmpty()) {
          // If the args are empty, it might be modifying anything
          checker.reportError(
              node,
              "purity.incorrect.sideeffectsonly",
              invokedElem.getSimpleName(),
              sideEffectsOnlyExpressionsFromAnnotation);
        }
      }
      actualSideEffectedExprs.stream()
          .filter(this::isAdditionalSideEffectedExpression)
          .forEach(expr -> extraSideEffects.addExpr(node, expr));
      return super.visitMethodInvocation(node, aVoid);
    }

    /**
     * Returns the {@link SideEffectsOnly} annotation (if it exists, else null) on the enclosing
     * method of a given method invocation.
     *
     * @param tree the method invocation
     * @return the {@link SideEffectsOnly} annotation on the enclosing method of a given method
     *     invocation
     */
    private AnnotationMirror getSideEffectsOnlyAnnotationOnEnclosingMethod(
        MethodInvocationTree tree) {
      MethodTree enclosingMethod =
          TreePathUtil.enclosingMethod(checker.getTypeFactory().getPath(tree));
      if (enclosingMethod == null) {
        return null;
      }
      return annoProvider.getDeclAnnotation(
          TreeUtils.elementFromDeclaration(enclosingMethod), SideEffectsOnly.class);
    }

    /**
     * Returns the arguments to a method invocation, including the receiver.
     *
     * @param methodInvok a method invocation
     * @return the arguments to a method invocation, including the receiver
     */
    private List<JavaExpression> getJavaExpressionsFromMethodInvocation(
        MethodInvocationTree methodInvok) {
      // TODO: collect all subexpressions of the given expression.  For now it just considers the
      // actual arguments, which is incomplete.
      List<? extends ExpressionTree> args = methodInvok.getArguments();
      ExpressionTree receiver = TreeUtils.getReceiverTree(methodInvok);
      List<ExpressionTree> exprs;
      if (receiver == null) {
        exprs = new ArrayList<>(args);
      } else {
        exprs = new ArrayList<>(args.size() + 1);
        exprs.add(receiver);
        exprs.addAll(args);
      }
      return exprs.stream().map(JavaExpression::fromTree).collect(Collectors.toList());
    }

    /**
     * Returns true if the given expression is a side-effected expression beyond what is described
     * in the {@link SideEffectsOnly} annotation.
     *
     * <p>The following criteria must be met in determining whether a given expression is
     * side-effected beyond the expressions provided in the annotation:
     *
     * <ul>
     *   <li>The expression does <b>not</b> appear in the expressions passed as arguments to the
     *       {@link SideEffectsOnly} annotation
     *   <li>The expression is modifiable by other code
     *   <li>The expression is <b>not</b> aliased by other variables in the method body
     * </ul>
     *
     * @param expr the expression to check for side-effecting
     * @return true if the given expression is a side-effected expressio beyond what is described in
     *     the {@link SideEffectsOnly} annotation
     */
    private boolean isAdditionalSideEffectedExpression(JavaExpression expr) {
      return !sideEffectsOnlyExpressionsFromAnnotation.contains(expr)
          && expr.isModifiableByOtherCode()
          && !aliasedExpressions.containsKey(expr);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void aVoid) {
      return super.visitNewClass(node, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
      JavaExpression javaExpr = JavaExpression.fromTree(node.getVariable());
      if (!sideEffectsOnlyExpressionsFromAnnotation.contains(javaExpr)) {
        extraSideEffects.addExpr(node, javaExpr);
      }
      return super.visitAssignment(node, aVoid);
    }

    @Override
    public Void visitVariable(VariableTree node, Void aVoid) {
      JavaExpression name = JavaExpression.fromVariableTree(node);
      JavaExpression expr = JavaExpression.fromTree(node.getInitializer());
      aliasedExpressions.put(name, expr);
      return super.visitVariable(node, aVoid);
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
