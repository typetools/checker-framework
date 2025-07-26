package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.Tree;
import java.util.HashSet;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.collectionownership.CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType;
import org.checkerframework.checker.collectionownership.qual.CollectionFieldDestructor;
import org.checkerframework.checker.collectionownership.qual.CreatesCollectionObligation;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory.PotentiallyFulfillingLoop;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the collection ownership type system. Its primary purpose is to create
 * temporary variables for expressions (which allow those expressions to have refined information in
 * the store, which the consistency checker can use).
 */
public class CollectionOwnershipTransfer
    extends CFAbstractTransfer<CFValue, CollectionOwnershipStore, CollectionOwnershipTransfer> {

  /** The type factory. */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /** The checker. */
  private final CollectionOwnershipChecker checker;

  /** The MustCall type factory to manage temp vars. */
  private final MustCallAnnotatedTypeFactory mcAtf;

  /**
   * Create a CollectionOwnershipTransfer.
   *
   * @param analysis the analysis
   * @param checker the checker
   */
  public CollectionOwnershipTransfer(
      CollectionOwnershipAnalysis analysis, CollectionOwnershipChecker checker) {
    super(analysis);
    atypeFactory = (CollectionOwnershipAnnotatedTypeFactory) analysis.getTypeFactory();
    mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(checker);
    this.checker = checker;
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitAssignment(
      AssignmentNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitAssignment(node, in);

    Node lhs = node.getTarget();
    lhs = getNodeOrTempVar(lhs);
    JavaExpression lhsJx = JavaExpression.fromNode(lhs);

    Node rhs = node.getExpression();
    rhs = getNodeOrTempVar(rhs);
    CollectionOwnershipStore coStore = atypeFactory.getStoreBefore(node);
    CollectionOwnershipType rhsType = atypeFactory.getCoType(rhs, coStore);

    // ownership transfer from rhs into lhs usually.
    // special case desugared assignments of a temporary array variable
    // and rhs being owning resource collection field
    if (rhsType != null) {
      switch (rhsType) {
        case OwningCollection:
        case OwningCollectionWithoutObligation:
          JavaExpression rhsJx = JavaExpression.fromNode(rhs);
          if (node.isDesugaredFromEnhancedArrayForLoop()
              || atypeFactory.isOwningCollectionField(
                  TreeUtils.elementFromTree(node.getExpression().getTree()))) {
            replaceInStores(res, lhsJx, atypeFactory.NOTOWNINGCOLLECTION);
          } else {
            replaceInStores(res, rhsJx, atypeFactory.NOTOWNINGCOLLECTION);
          }
          break;
        default:
      }
    }
    // boolean assignmentOfOwningCollectionArrayElement =
    //     lhsIsOwningCollection && lhs.getTree().getKind() == Tree.Kind.ARRAY_ACCESS;

    // if (assignmentOfOwningCollectionArrayElement) {
    //   ExpressionTree arrayExpression = ((ArrayAccessTree) lhs.getTree()).getExpression();
    //   JavaExpression arrayJx = JavaExpression.fromTree(arrayExpression);

    //   boolean inAssigningLoop =
    //       MustCallOnElementsAnnotatedTypeFactory.doesAssignmentCreateArrayObligation(
    //           (AssignmentTree) node.getTree());

    //   // transformation of assigning loop is handled at the loop condition node,
    //   // not the assignment node. So, only transform if not in an assigning loop.
    //   if (!inAssigningLoop) {
    //     store =
    //         transformWriteToOwningCollection(arrayJx, arrayExpression, node.getExpression(),
    // store);
    //   }
    return res;
  }

  /**
   * Checks if the given AST tree is the condition for a collection-obligation-fulfilling and
   * whether this loop calls all methods in the MustCall type of the elements of some collection. In
   * this case, refine the type of the collection to @OwningCollectionWithoutObligation in the else
   * store of the incoming transfer result.
   *
   * @param res the incoming transfer result
   * @param tree the AST tree that is possibly the loop condition for a
   *     collection-obligation-fulfilling loop
   * @return the resulting transfer result
   */
  private TransferResult<CFValue, CollectionOwnershipStore> transformPotentiallyFulfillingLoop(
      TransferResult<CFValue, CollectionOwnershipStore> res, Tree tree) {
    PotentiallyFulfillingLoop loop =
        CollectionOwnershipAnnotatedTypeFactory.getFulfillingLoopForCondition(tree);
    if (loop != null) {
      CollectionOwnershipStore elseStore = res.getElseStore();
      JavaExpression collectionJx = JavaExpression.fromTree(loop.collectionTree);

      CollectionOwnershipType collectionCoType = atypeFactory.getCoType(loop.collectionTree);
      if (collectionCoType == CollectionOwnershipType.OwningCollection) {
        List<String> mustCallValuesOfElements =
            atypeFactory.getMustCallValuesOfResourceCollectionComponent(loop.collectionTree);
        if (loop.getMethods().containsAll(mustCallValuesOfElements)) {
          elseStore.clearValue(collectionJx);
          elseStore.insertValue(collectionJx, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
          return new ConditionalTransferResult<>(
              res.getResultValue(), res.getThenStore(), elseStore);
        }
      }
    }
    return res;
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitLessThan(node, in);
    return transformPotentiallyFulfillingLoop(res, node.getTree());
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitMethodInvocation(node, in);

    updateStoreWithTempVar(res, node);

    ExecutableElement method = node.getTarget().getMethod();
    List<Node> args = node.getArguments();
    res = transferOwnershipForMethodInvocation(method, node, args, res);
    res = transformPotentiallyFulfillingLoop(res, node.getTree());

    // check whether the method is annotated @CreatesCollectionObligation
    ExecutableElement methodElement = TreeUtils.elementFromUse(node.getTree());
    boolean hasCreatesCollectionObligation =
        atypeFactory.getDeclAnnotation(methodElement, CreatesCollectionObligation.class) != null;
    boolean hasCollectionFieldDestructor =
        atypeFactory.getDeclAnnotation(methodElement, CollectionFieldDestructor.class) != null;
    if (hasCreatesCollectionObligation) {
      Node receiverNode = node.getTarget().getReceiver();
      JavaExpression receiverJx = JavaExpression.fromNode(receiverNode);
      CollectionOwnershipStore coStore = atypeFactory.getStoreBefore(node);
      if (atypeFactory.getCoType(receiverNode, coStore)
          == CollectionOwnershipType.OwningCollectionWithoutObligation) {
        replaceInStores(res, receiverJx, atypeFactory.OWNINGCOLLECTION);
      }
    }
    if (hasCollectionFieldDestructor) {
      List<String> destructedFields =
          atypeFactory.getCollectionFieldDestructorAnnoFields(methodElement);
      for (String destructedFieldName : destructedFields) {
        JavaExpression fieldExpr =
            atypeFactory.stringToJavaExpression(destructedFieldName, methodElement);
        if (fieldExpr != null) {
          replaceInStores(res, fieldExpr, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
        }
      }
    }
    return res;
  }

  /**
   * Executes collection ownership transfer in method invocations. Owning arguments to an owning
   * parameter lose ownership.
   *
   * @param method the method whose parameters are checked
   * @param node the node of the invocation
   * @param args the list of method arguments
   * @param res the transfer result so far
   * @return the updated transfer result
   */
  private TransferResult<CFValue, CollectionOwnershipStore> transferOwnershipForMethodInvocation(
      ExecutableElement method,
      Node node,
      List<Node> args,
      TransferResult<CFValue, CollectionOwnershipStore> res) {
    List<? extends VariableElement> params = method.getParameters();

    for (int i = 0; i < Math.min(args.size(), params.size()); i++) {
      VariableElement param = params.get(i);
      Node arg = args.get(i);
      arg = getNodeOrTempVar(arg);
      JavaExpression argJx = JavaExpression.fromNode(arg);
      CollectionOwnershipStore coStore = atypeFactory.getStoreBefore(node);
      CollectionOwnershipType argType = atypeFactory.getCoType(arg, coStore);
      CollectionOwnershipType paramType =
          atypeFactory.getCoType(new HashSet<>(param.asType().getAnnotationMirrors()));
      if (paramType == null) {
        continue;
      }

      Element argElem = TreeUtils.elementFromTree(arg.getTree());
      boolean transferOwnership = false;
      switch (paramType) {
        case OwningCollection:
          switch (argType) {
            case OwningCollection:
            case OwningCollectionWithoutObligation:
              transferOwnership = true;
              break;
            default:
          }
          break;
        case OwningCollectionWithoutObligation:
          switch (argType) {
            case OwningCollectionWithoutObligation:
              transferOwnership = true;
              break;
            default:
          }
          break;
        default:
      }
      if (transferOwnership) {
        if (argElem.getKind().isField()) {
          checker.reportError(
              arg.getTree(), "transfer.owningcollection.field.ownership", arg.getTree().toString());
        } else {
          replaceInStores(res, argJx, atypeFactory.NOTOWNINGCOLLECTION);
        }
      }
    }
    return res;
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<CFValue, CollectionOwnershipStore> input) {
    TransferResult<CFValue, CollectionOwnershipStore> result =
        super.visitObjectCreation(node, input);
    updateStoreWithTempVar(result, node);

    ExecutableElement constructor = TreeUtils.elementFromUse(node.getTree());
    List<Node> args = node.getArguments();
    result = transferOwnershipForMethodInvocation(constructor, node, args, result);

    // the return value defaulting logic cannot recognize that a diamond constructed collection
    // is a resource collection, as it runs before the type variable is inferred:
    // List<Socket> = new ArrayList<>();
    // Thus, the following checks object creation expressions again on whether they are
    // resource collections with no type variables, and if they are @Bottom, they are
    // unrefined to @OwningCollection. Change the type of both the type var and the computed
    // expression itself.
    CollectionOwnershipStore store = result.getRegularStore();
    CFValue resultValue = result.getResultValue();
    Node tempVarNode = getNodeOrTempVar(node);
    JavaExpression tempVarJx = JavaExpression.fromNode(tempVarNode);

    CollectionOwnershipStore coStore = atypeFactory.getStoreBefore(node);
    CollectionOwnershipType resolvedType = atypeFactory.getCoType(tempVarNode, coStore);
    if (atypeFactory.isResourceCollection(node.getTree())) {
      boolean isDiamond = node.getTree().getTypeArguments().size() == 0;
      if (isDiamond && resolvedType == CollectionOwnershipType.OwningCollectionBottom) {
        store.clearValue(tempVarJx);
        store.insertValue(tempVarJx, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
        resultValue =
            analysis.createSingleAnnotationValue(
                atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION, node.getType());
      }
    }

    return new RegularTransferResult<CFValue, CollectionOwnershipStore>(resultValue, store);
  }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as {@code node}.
   *
   * @param node the node to be assigned to a temporary variable
   * @param result the transfer result containing the store to be modified
   */
  public void updateStoreWithTempVar(
      TransferResult<CFValue, CollectionOwnershipStore> result, Node node) {
    // Must-call obligations on primitives are not supported.
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      LocalVariableNode temp = mcAtf.getTempVar(node);
      if (temp != null) {
        JavaExpression localExp = JavaExpression.fromNode(temp);
        AnnotationMirror anm =
            atypeFactory
                .getAnnotatedType(node.getTree())
                .getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
        insertInStores(result, localExp, anm == null ? atypeFactory.TOP : anm);
      }
    }
  }

  /**
   * Inserts newAnno as the value into all stores (conditional or not) in the result for node.
   *
   * @param result the TransferResult holding the stores to modify
   * @param target the receiver whose value should be modified
   * @param newAnno the new value
   */
  protected static void insertInStores(
      TransferResult<CFValue, CollectionOwnershipStore> result,
      JavaExpression target,
      AnnotationMirror newAnno) {
    if (result.containsTwoStores()) {
      result.getThenStore().insertValue(target, newAnno);
      result.getElseStore().insertValue(target, newAnno);
    } else {
      result.getRegularStore().insertValue(target, newAnno);
    }
  }

  /**
   * Inserts newAnno as the value into all stores (conditional or not) in the result for node,
   * clearing the previous value first.
   *
   * @param result the TransferResult holding the stores to modify
   * @param target the receiver whose value should be modified
   * @param newAnno the new value
   */
  protected static void replaceInStores(
      TransferResult<CFValue, CollectionOwnershipStore> result,
      JavaExpression target,
      AnnotationMirror newAnno) {
    if (result.containsTwoStores()) {
      result.getThenStore().clearValue(target);
      result.getElseStore().clearValue(target);
      result.getThenStore().insertValue(target, newAnno);
      result.getElseStore().insertValue(target, newAnno);
    } else {
      result.getRegularStore().clearValue(target);
      result.getRegularStore().insertValue(target, newAnno);
    }
  }

  /**
   * Removes casts from {@code node} and returns the temp-var corresponding to it if it exists or
   * else {@code node} with removed casts.
   *
   * @param node the node
   * @return the temp-var corresponding to {@code node} with casts removed if it exists or else
   *     {@code node} with casts removed
   */
  protected Node getNodeOrTempVar(Node node) {
    node = NodeUtils.removeCasts(node);
    Node tempVarForNode =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory)
            .getTempVarForNode(node);
    if (tempVarForNode != null) {
      return tempVarForNode;
    }
    return node;
  }
}
