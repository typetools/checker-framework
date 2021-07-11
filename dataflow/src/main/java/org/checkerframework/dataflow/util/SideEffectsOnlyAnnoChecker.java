package main.java.org.checkerframework.dataflow.util;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

public class SideEffectsOnlyAnnoChecker {
  public static SideEffectsOnlyResult checkSideEffectsOnly(
      TreePath statement,
      AnnotationProvider annoProvider,
      List<JavaExpression> sideEffectsOnlyExpressions) {
    SideEffectsOnlyCheckerHelper helper =
        new SideEffectsOnlyCheckerHelper(annoProvider, sideEffectsOnlyExpressions);
    helper.scan(statement, null);
    return helper.sideEffectsOnlyResult;
  }

  public static class SideEffectsOnlyResult {
    protected final List<Pair<Tree, JavaExpression>> seOnlyIncorrectExprs = new ArrayList<>(1);

    public void addNotSEOnlyExpr(Tree t, JavaExpression javaExpr) {
      seOnlyIncorrectExprs.add(Pair.of(t, javaExpr));
    }

    public List<Pair<Tree, JavaExpression>> getSeOnlyResult() {
      return seOnlyIncorrectExprs;
    }
  }

  protected static class SideEffectsOnlyCheckerHelper extends TreePathScanner<Void, Void> {

    SideEffectsOnlyResult sideEffectsOnlyResult = new SideEffectsOnlyResult();
    List<JavaExpression> sideEffectsOnlyExpressions;

    protected final AnnotationProvider annoProvider;

    public SideEffectsOnlyCheckerHelper(
        AnnotationProvider annoProvider, List<JavaExpression> sideEffectsOnlyExpressions) {
      this.annoProvider = annoProvider;
      this.sideEffectsOnlyExpressions = sideEffectsOnlyExpressions;
    }

    @Override
    public Void visitCatch(CatchTree node, Void aVoid) {
      return super.visitCatch(node, aVoid);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      Element treeElem = TreeUtils.elementFromTree(node);
      AnnotationMirror pureAnno = annoProvider.getDeclAnnotation(treeElem, Pure.class);
      AnnotationMirror sideEffectFreeAnno =
          annoProvider.getDeclAnnotation(treeElem, SideEffectFree.class);
      if (pureAnno != null || sideEffectFreeAnno != null) {
        return super.visitMethodInvocation(node, aVoid);
      }

      AnnotationMirror sideEffectsOnlyAnno =
          annoProvider.getDeclAnnotation(treeElem, SideEffectsOnly.class);
      if (sideEffectsOnlyAnno != null) {
        JavaExpression receiverExpr = JavaExpression.getReceiver(node);
        sideEffectsOnlyResult.addNotSEOnlyExpr(node, receiverExpr);
        List<JavaExpression> paramsAsLocals =
            JavaExpression.getParametersAsLocalVariables((ExecutableElement) treeElem);
        for (JavaExpression expr : paramsAsLocals) {
          sideEffectsOnlyResult.addNotSEOnlyExpr(node, expr);
        }
      } else {

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
        sideEffectsOnlyResult.addNotSEOnlyExpr(node, javaExpr);
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
