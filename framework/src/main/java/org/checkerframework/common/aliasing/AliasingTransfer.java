package org.checkerframework.common.aliasing;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.aliasing.qual.LeakedToResult;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Type refinement is treated in the usual way, except that at (pseudo-)assignments the RHS may lose
 * its type refinement, before the LHS is type-refined.
 *
 * <p>The RHS always loses its type refinement (it is widened to {@literal @}MaybeAliased, and its
 * declared type must have been {@literal @}MaybeAliased) except in the following cases:
 *
 * <ol>
 *   <li>The RHS is a fresh expression.
 *   <li>The LHS is a {@literal @}NonLeaked formal parameter and the RHS is an argument in a method
 *       call or constructor invocation.
 *   <li>The LHS is a {@literal @}LeakedToResult formal parameter, the RHS is an argument in a
 *       method call or constructor invocation, and the method's return value is discarded.
 * </ol>
 */
public class AliasingTransfer extends CFTransfer {

  private AnnotatedTypeFactory factory;

  public AliasingTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    factory = analysis.getTypeFactory();
  }

  /**
   * Case 1: For every assignment, the LHS is refined if the RHS has type {@literal @}Unique and is
   * a method invocation or a new class instance.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode n, TransferInput<CFValue, CFStore> in) {
    Node rhs = n.getExpression();
    Tree treeRhs = rhs.getTree();
    AnnotatedTypeMirror rhsType = factory.getAnnotatedType(treeRhs);

    if (rhsType.hasAnnotation(Unique.class)
        && (rhs instanceof MethodInvocationNode || rhs instanceof ObjectCreationNode)) {
      return super.visitAssignment(n, in); // Do normal refinement.
    }
    // Widen the type of the rhs if the RHS's declared type wasn't @Unique.
    JavaExpression rhsExpr = JavaExpression.fromNode(rhs);
    in.getRegularStore().clearValue(rhsExpr);
    return new RegularTransferResult<>(null, in.getRegularStore());
  }

  /**
   * Handling pseudo-assignments. Called by {@code CFAbstractTransfer.visitMethodInvocation()}.
   *
   * <p>Case 2: Given a method call, traverses all formal parameters of the method declaration, and
   * if it doesn't have the {@literal @}NonLeaked or {@literal @}LeakedToResult annotations, we
   * remove the node of the respective argument in the method call from the store. If parameter has
   * {@literal @}LeakedToResult, {@code visitMethodInvocation()} handles it.
   */
  @Override
  protected void processPostconditions(
      MethodInvocationNode n, CFStore store, ExecutableElement methodElement, Tree tree) {
    super.processPostconditions(n, store, methodElement, tree);
    if (TreeUtils.isEnumSuper(n.getTree())) {
      // Skipping the init() method for enums.
      return;
    }
    List<Node> args = n.getArguments();
    List<? extends VariableElement> params = methodElement.getParameters();
    assert (args.size() == params.size())
        : "Number of arguments in "
            + "the method call "
            + n
            + " is different from the"
            + " number of parameters for the method declaration: "
            + methodElement.getSimpleName();

    AnnotatedExecutableType annotatedType = factory.getAnnotatedType(methodElement);
    List<AnnotatedTypeMirror> paramTypes = annotatedType.getParameterTypes();
    for (int i = 0; i < args.size(); i++) {
      Node arg = args.get(i);
      AnnotatedTypeMirror paramType = paramTypes.get(i);
      if (!paramType.hasAnnotation(NonLeaked.class)
          && !paramType.hasAnnotation(LeakedToResult.class)) {
        store.clearValue(JavaExpression.fromNode(arg));
      }
    }

    // Now, doing the same as above for the receiver parameter
    Node receiver = n.getTarget().getReceiver();
    AnnotatedDeclaredType receiverType = annotatedType.getReceiverType();
    if (receiverType != null
        && !receiverType.hasAnnotation(LeakedToResult.class)
        && !receiverType.hasAnnotation(NonLeaked.class)) {
      store.clearValue(JavaExpression.fromNode(receiver));
    }
  }

  /**
   * Case 3: Given a method invocation expression, if the parent of the expression is not a
   * statement, check if there are any arguments of the method call annotated as
   * {@literal @}LeakedToResult and remove it from the store, since it might be leaked.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    Tree parent = n.getTreePath().getParentPath().getLeaf();
    boolean parentIsStatement = parent.getKind() == Kind.EXPRESSION_STATEMENT;

    if (!parentIsStatement) {

      ExecutableElement methodElement = TreeUtils.elementFromUse(n.getTree());
      List<Node> args = n.getArguments();
      List<? extends VariableElement> params = methodElement.getParameters();
      assert (args.size() == params.size())
          : "Number of arguments in "
              + "the method call "
              + n
              + " is different from the"
              + " number of parameters for the method declaration: "
              + methodElement.getSimpleName();
      CFStore store = in.getRegularStore();

      for (int i = 0; i < args.size(); i++) {
        Node arg = args.get(i);
        VariableElement param = params.get(i);
        if (factory.getAnnotatedType(param).hasAnnotation(LeakedToResult.class)) {
          // If argument can leak to result, and parent is not a
          // single statement, remove that node from store.
          store.clearValue(JavaExpression.fromNode(arg));
        }
      }

      // Now, doing the same as above for the receiver parameter
      Node receiver = n.getTarget().getReceiver();
      AnnotatedExecutableType annotatedType = factory.getAnnotatedType(methodElement);
      AnnotatedDeclaredType receiverType = annotatedType.getReceiverType();
      if (receiverType != null && receiverType.hasAnnotation(LeakedToResult.class)) {
        store.clearValue(JavaExpression.fromNode(receiver));
      }
    }
    // If parent is a statement, processPostconditions will handle the pseudo-assignments.
    return super.visitMethodInvocation(n, in);
  }
}
