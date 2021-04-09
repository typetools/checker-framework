package main.java.org.checkerframework.dataflow.util;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.javacutil.AnnotationProvider;

public class SideEffectsOnlyAnnoChecker {
  public static SideEffectsOnlyResult checkSideEffectsOnly(
      TreePath statement, AnnotationProvider annoProvider) {
    SideEffectsOnlyCheckerHelper helper = new SideEffectsOnlyCheckerHelper(annoProvider);
    helper.scan(statement, null);
    return helper.sideEffectsOnlyResult;
  }

  public static class SideEffectsOnlyResult {}

  protected static class SideEffectsOnlyCheckerHelper extends TreePathScanner<Void, Void> {

    SideEffectsOnlyResult sideEffectsOnlyResult = new SideEffectsOnlyResult();

    protected final AnnotationProvider annoProvider;

    public SideEffectsOnlyCheckerHelper(AnnotationProvider annoProvider) {
      this.annoProvider = annoProvider;
    }

    @Override
    public Void visitCatch(CatchTree node, Void aVoid) {
      return super.visitCatch(node, aVoid);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      return super.visitMethodInvocation(node, aVoid);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void aVoid) {
      return super.visitNewClass(node, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
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
