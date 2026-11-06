package org.checkerframework.checker.nullness;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * The transfer function for the Path Shape Checker. If {@code Files.isRegularFile(p)} or {@code
 * f.isFile()} returns true, then {@code p} or {@code f} is {@code @HasName}, because a root and the
 * empty path always denote a directory.
 *
 * <p>When it processes the declaration of a local variable, it also discards the type factory's
 * record of the variable's initializer; see {@link
 * PathShapeAnnotatedTypeFactory#forgetInitializerShapes}.
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
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode n, TransferInput<CFValue, CFStore> in) {
    if (n.getTarget() instanceof LocalVariableNode target) {
      // The type of the initializer might differ from when the analysis last processed the
      // declaration, such as in a loop.
      factory.forgetInitializerShapes(target.getElement());
    }
    return super.visitAssignment(n, in);
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
      return n.getArgument(0);
    } else if (method.equals(factory.fileIsFile)) {
      return n.getTarget().getReceiver();
    } else {
      return null;
    }
  }
}
