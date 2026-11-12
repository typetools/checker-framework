package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The transfer function for the Path Shape Checker. If {@code Files.isRegularFile(p)} or {@code
 * f.isFile()} returns true, then {@code p} or {@code f} is {@code @HasName}, because a root and the
 * empty path always denote a directory.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public class PathShapeTransfer extends CFTransfer {

  /** The type factory. */
  private final PathShapeAnnotatedTypeFactory factory;

  /**
   * Creates a new PathShapeTransfer.
   *
   * @param analysis the analysis
   */
  public PathShapeTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    this.factory = (PathShapeAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    Node regularFile = regularFileTestSubject(n);
    if (regularFile == null) {
      return result;
    }
    CFStore thenStore = result.getThenStore().copy();
    CFStore elseStore = result.getElseStore();
    thenStore.insertValue(JavaExpression.fromNode(regularFile), factory.HAS_NAME);
    return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
  }

  /**
   * If the given method invocation is {@code Files.isRegularFile(p, ...)} or {@code f.isFile()},
   * returns {@code p} or {@code f}. Otherwise, returns null.
   *
   * @param n a method invocation
   * @return the path that the invocation tests, or null
   */
  private @Nullable Node regularFileTestSubject(MethodInvocationNode n) {
    ExecutableElement method = n.getTarget().getMethod();
    if (method.equals(factory.filesIsRegularFile)) {
      // A later argument might reassign the tested variable, in which case the refinement would
      // apply to a value that was not tested.  Refine only if no later argument has side effects.
      MethodInvocationTree tree = n.getTree();
      if (tree == null) {
        return null;
      }
      List<? extends ExpressionTree> args = tree.getArguments();
      for (ExpressionTree arg : args.subList(1, args.size())) {
        if (!isSideEffectFree(arg)) {
          return null;
        }
      }
      return n.getArgument(0);
    } else if (method.equals(factory.fileIsFile)) {
      return n.getTarget().getReceiver();
    } else {
      return null;
    }
  }

  /**
   * Returns true if the given expression is syntactically free of side effects: it is a literal, a
   * variable, or a field access, possibly parenthesized. This is conservative; it returns false for
   * many expressions that have no side effects.
   *
   * @param tree an expression
   * @return true if evaluating {@code tree} cannot reassign a variable
   */
  private static boolean isSideEffectFree(ExpressionTree tree) {
    ExpressionTree expr = TreeUtils.withoutParens(tree);
    return expr instanceof LiteralTree
        || expr instanceof IdentifierTree
        || (expr instanceof MemberSelectTree mst && isSideEffectFree(mst.getExpression()));
  }
}
