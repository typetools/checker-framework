package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.collectionownership.CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType;
import org.checkerframework.checker.collectionownership.qual.CollectionFieldDestructor;
import org.checkerframework.checker.collectionownership.qual.CreatesCollectionObligation;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the collection ownership type system.
 *
 * <p>This class implements the transfer rules for collection ownership. In particular, it handles
 * ownership transfer through assignments, method invocations, and object creation. It refines
 * collections to {@code @OwningCollectionWithoutObligation} when a disposal loop discharges the
 * required must-call obligations of the iterated elements.
 *
 * <p>This class also creates temporary variables for expressions so their collection ownership
 * types can be refined in the store and later queried by the consistency checker.
 */
public class CollectionOwnershipTransfer
    extends CFAbstractTransfer<CFValue, CollectionOwnershipStore, CollectionOwnershipTransfer> {

  /** The type factory. */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /** The checker. */
  private final CollectionOwnershipChecker checker;

  /** The MustCall type factory to manage temporary variables. */
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
    this.atypeFactory = (CollectionOwnershipAnnotatedTypeFactory) analysis.getTypeFactory();
    this.mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(checker);
    this.checker = checker;
  }

  /**
   * Returns the initial store, in which each owning collection field of {@code this} has a
   * collection ownership type and parameters are initialized to their declared qualifier type.
   *
   * <p>Initializer expressions and blocks start each owning collection field declared in the class
   * at {@code @OwningCollectionBottom} (not yet assigned), and each constructor starts from the
   * state the initializers leave ({@code @OwningCollectionBottom} if there are none). Inherited
   * owning collection fields start at {@code @OwningCollection}, since the superclass constructor
   * may already have given them an obligation. Other instance methods start with the declared type
   * of the class's own fields (see {@link CFAbstractTransfer#initialStore}).
   *
   * @param underlyingAST the AST of the CFG being analyzed: a method, a lambda, a field
   *     initializer, or an initializer block
   * @param parameters a list of local variable nodes representing formal parameters (if any)
   * @return the initial store for {@code underlyingAST}
   */
  @Override
  public CollectionOwnershipStore initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    CollectionOwnershipStore store = super.initialStore(underlyingAST, parameters);
    ClassTree classTree;
    boolean ownFieldsMayBeUnassigned;
    if (underlyingAST instanceof CFGStatement initializer) {
      if (isStaticInitializer(initializer.getCode())) {
        return store;
      }
      // For initializer code, super.initialStore returns the store that the previous initializer
      // left, not a copy.
      store = analysis.createCopiedStore(store);
      classTree = initializer.getClassTree();
      ownFieldsMayBeUnassigned = true;
    } else if (underlyingAST instanceof CFGMethod method) {
      MethodTree methodTree = method.getMethod();
      if (methodTree.getModifiers().getFlags().contains(Modifier.STATIC)) {
        return store;
      }
      classTree = method.getClassTree();
      ownFieldsMayBeUnassigned = TreeUtils.isConstructor(methodTree);
    } else {
      // A lambda starts from the store of its enclosing code.
      return store;
    }

    TypeElement classElement = TreeUtils.elementFromDeclaration(classTree);
    if (classElement == null) {
      throw new BugInCF("Class " + classTree.getSimpleName() + " has no element");
    }
    ThisReference thisReference = new ThisReference(classElement.asType());
    for (VariableElement field :
        ElementUtils.getAllFieldsIn(classElement, atypeFactory.getElementUtils())) {
      if (ElementUtils.isStatic(field) || !atypeFactory.isOwningCollectionField(field)) {
        continue;
      }
      FieldAccess fieldAccess = new FieldAccess(thisReference, field);
      if (store.getValue(fieldAccess) != null) {
        continue;
      }
      if (!field.getEnclosingElement().equals(classElement)) {
        // Inherited fields start at @OwningCollection
        store.insertValue(
            fieldAccess,
            analysis.createSingleAnnotationValue(atypeFactory.OWNINGCOLLECTION, field.asType()));
      } else if (ownFieldsMayBeUnassigned) {
        // Fields declared in the class start at @OwningCollectionBottom
        store.insertValue(
            fieldAccess, analysis.createSingleAnnotationValue(atypeFactory.BOTTOM, field.asType()));
      }
    }
    return store;
  }

  /**
   * Returns true if the given initializer is a static field initializer or a static initializer
   * block.
   *
   * @param initializer a field declaration or an initializer block
   * @return true if {@code initializer} is static
   */
  private static boolean isStaticInitializer(Tree initializer) {
    if (initializer instanceof BlockTree block) {
      return block.isStatic();
    }
    return initializer instanceof VariableTree field
        && field.getModifiers().getFlags().contains(Modifier.STATIC);
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitAssignment(
      AssignmentNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    // Transfer ownership from the rhs to the lhs.
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitAssignment(node, in);

    Node lhs = getNodeOrTempVar(node.getTarget());
    JavaExpression lhsJE = JavaExpression.fromNode(lhs);

    Node rhs = getNodeOrTempVar(node.getExpression());
    CollectionOwnershipType rhsType =
        atypeFactory.getCoType(rhs, atypeFactory.getStoreBefore(node));

    // Ownership usually transfers from the rhs into the lhs.
    // Some assignments instead create a non-owning alias at the lhs:
    //   1. desugared enhanced-for array temporaries
    //   2. reads from owning collection fields
    //   3. explicit @NotOwningCollection declarations
    // In those cases the rhs keeps its existing ownership information.
    if (rhsType != null) {
      switch (rhsType) {
        case OwningCollection, OwningCollectionWithoutObligation -> {
          JavaExpression rhsJE = JavaExpression.fromNode(rhs);
          if (node.isDesugaredFromEnhancedArrayForLoop()
              || (node.getExpression() instanceof FieldAccessNode rhsFieldAccess
                  && atypeFactory.isOwningCollectionField(rhsFieldAccess.getElement()))) {
            replaceInStores(res, lhsJE, atypeFactory.NOTOWNINGCOLLECTION);
          } else {
            // Make the RHS @NotOwningCollection to reflect the ownership transfer, unless there is
            // a @NotOwningCollection annotation, in which case there is no transfer and the lhs
            // should be @NotOwningCollection
            replaceInStores(
                res,
                hasExplicitNotOwningCollectionDeclaration(node) ? lhsJE : rhsJE,
                atypeFactory.NOTOWNINGCOLLECTION);
          }
        }
        default -> {}
      }
    }
    return res;
  }

  /**
   * May refine the type of the collection to @OwningCollectionWithoutObligation in the else store
   * of the incoming transfer result. Does so if the given AST tree is the condition for a disposal
   * loop whose MCCA called-methods cover the MustCall type of the collection's elements.
   *
   * @param res the incoming transfer result
   * @param tree the AST tree that may represent a disposal-loop condition
   * @return the resulting transfer result
   */
  private TransferResult<CFValue, CollectionOwnershipStore> updateStoreForDisposalLoop(
      TransferResult<CFValue, CollectionOwnershipStore> res, Tree tree) {
    DisposalLoopInfo disposalLoopInfo = atypeFactory.getDisposalLoopInfoForConditionTree(tree);
    if (disposalLoopInfo != null) {
      CollectionOwnershipStore elseStore = res.getElseStore();
      ExpressionTree collectionExpression = disposalLoopInfo.expressionTree();
      JavaExpression collectionJE = JavaExpression.fromTree(collectionExpression);
      Set<String> disposalLoopCalledMethods = atypeFactory.getCalledMethods(disposalLoopInfo);

      CollectionOwnershipType collectionCoType = atypeFactory.getCoType(collectionExpression);
      if (collectionCoType == CollectionOwnershipType.OwningCollection) {
        List<String> requiredElementMethods =
            atypeFactory.getMustCallValuesOfResourceCollectionComponent(collectionExpression);
        if (disposalLoopCalledMethods != null
            && requiredElementMethods != null
            && disposalLoopCalledMethods.containsAll(requiredElementMethods)) {
          elseStore.clearValue(collectionJE);
          elseStore.insertValue(collectionJE, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
          return new ConditionalTransferResult<>(
              res.getResultValue(), res.getThenStore(), elseStore);
        }
      }
    }
    return res;
  }

  /**
   * Returns whether the given assignment is a variable declaration whose declared type is
   * explicitly {@code @NotOwningCollection}.
   *
   * <p>Such a declaration initializes a non-owning alias at the lhs instead of consuming ownership
   * from the rhs expression.
   *
   * @param node an assignment node
   * @return true if the assignment is a variable declaration with declared type
   *     {@code @NotOwningCollection}
   */
  private boolean hasExplicitNotOwningCollectionDeclaration(AssignmentNode node) {
    if (!(node.getTree() instanceof VariableTree variableTree)) {
      return false;
    }
    VariableElement declaredElement = TreeUtils.elementFromDeclaration(variableTree);
    if (declaredElement == null) {
      return false;
    }
    List<? extends AnnotationMirror> declaredType = declaredElement.asType().getAnnotationMirrors();
    return atypeFactory.getCoType(declaredType) == CollectionOwnershipType.NotOwningCollection;
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitLessThan(node, in);
    return updateStoreForDisposalLoop(res, node.getTree());
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitMethodInvocation(node, in);

    updateStoreWithTempVar(res, node);

    ExecutableElement method = node.getTarget().getMethod();
    List<Node> args = node.getArguments();
    res = transferOwnershipForMethodInvocation(method, node, args, res);
    Tree tree = node.getTree();
    if (tree != null) {
      // An invocation without a tree cannot be a disposal-loop condition.
      res = updateStoreForDisposalLoop(res, tree);
    }

    // Check whether the method is annotated @CreatesCollectionObligation.
    boolean hasCreatesCollectionObligation =
        atypeFactory.getDeclAnnotation(method, CreatesCollectionObligation.class) != null;
    boolean hasCollectionFieldDestructor =
        atypeFactory.getDeclAnnotation(method, CollectionFieldDestructor.class) != null;
    if (hasCreatesCollectionObligation) {
      Node receiverNode = node.getTarget().getReceiver();
      JavaExpression receiverJE = JavaExpression.fromNode(receiverNode);
      if (atypeFactory.getCoType(receiverNode, atypeFactory.getStoreBefore(node))
          == CollectionOwnershipType.OwningCollectionWithoutObligation) {
        replaceInStores(res, receiverJE, atypeFactory.OWNINGCOLLECTION);
      }
    }
    if (hasCollectionFieldDestructor) {
      List<String> destructedFields = atypeFactory.getCollectionFieldDestructorAnnoFields(method);
      for (String destructedFieldName : destructedFields) {
        JavaExpression fieldExpr = atypeFactory.stringToJavaExpression(destructedFieldName, method);
        if (fieldExpr != null) {
          replaceInStores(res, fieldExpr, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
        }
      }
    }
    return res;
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitConditionalNot(
      ConditionalNotNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitConditionalNot(node, in);
    return updateStoreForDisposalLoop(res, node.getTree());
  }

  @Override
  public TransferResult<CFValue, CollectionOwnershipStore> visitGreaterThan(
      GreaterThanNode node, TransferInput<CFValue, CollectionOwnershipStore> in) {
    TransferResult<CFValue, CollectionOwnershipStore> res = super.visitGreaterThan(node, in);
    return updateStoreForDisposalLoop(res, node.getTree());
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
      Node arg = getNodeOrTempVar(args.get(i));
      JavaExpression argJE = JavaExpression.fromNode(arg);
      CollectionOwnershipType argType =
          atypeFactory.getCoType(arg, atypeFactory.getStoreBefore(node));
      CollectionOwnershipType paramType =
          atypeFactory.getCoType(param.asType().getAnnotationMirrors());
      if (argType == null || paramType == null) {
        continue;
      }

      boolean transferOwnership = false;
      switch (paramType) {
        case OwningCollection -> {
          switch (argType) {
            case OwningCollection:
            case OwningCollectionWithoutObligation:
              transferOwnership = true;
              break;
            default:
          }
        }
        case OwningCollectionWithoutObligation -> {
          switch (argType) {
            case OwningCollectionWithoutObligation:
              transferOwnership = true;
              break;
            default:
          }
        }
        default -> {}
      }
      if (transferOwnership) {
        if (arg instanceof FieldAccessNode argFieldAccess) {
          checker.reportError(
              argFieldAccess.getTree(),
              "transfer.owningcollection.field.ownership",
              argFieldAccess.getTree().toString());
        } else {
          replaceInStores(res, argJE, atypeFactory.NOTOWNINGCOLLECTION);
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

    // The return value defaulting logic cannot recognize that a diamond-constructed collection
    // is a resource collection, as it runs before the type variable is inferred:
    //   List<Socket> = new ArrayList<>();
    // Thus, the following checks object creation expressions again on whether they are
    // resource collections with no type variables, and if they are @Bottom, they are
    // unrefined to @OwningCollection. Change the type of both the type var and the computed
    // expression itself.
    CollectionOwnershipStore store = result.getRegularStore();
    CFValue resultValue = result.getResultValue();
    Node tempVarNode = getNodeOrTempVar(node);
    JavaExpression tempVarJE = JavaExpression.fromNode(tempVarNode);

    CollectionOwnershipType resolvedType =
        atypeFactory.getCoType(tempVarNode, atypeFactory.getStoreBefore(node));
    if (atypeFactory.isResourceCollection(node.getTree())) {
      boolean isDiamond = node.getTree().getTypeArguments().size() == 0;
      if (isDiamond && resolvedType == CollectionOwnershipType.OwningCollectionBottom) {
        store.clearValue(tempVarJE);
        store.insertValue(tempVarJE, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION);
        resultValue =
            analysis.createSingleAnnotationValue(
                atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION, node.getType());
      }
    }

    return new RegularTransferResult<CFValue, CollectionOwnershipStore>(resultValue, store);
  }

  /**
   * Updates the store to give the temp var for {@code node} the same type as {@code node}.
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
        Tree tree = node.getTree();
        assert tree != null
            : "@AssumeAssertion(nullness): Must Call creates temp vars only for nodes with trees";
        AnnotationMirror anm =
            atypeFactory.getAnnotatedType(tree).getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
        insertIntoStores(result, localExp, anm == null ? atypeFactory.TOP : anm);
      }
    }
  }

  /**
   * Inserts {@code newAnno} as the value into all stores (conditional or not) in the result for
   * node, clearing the previous value first.
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
   * Returns the temp-var corresponding to {@code node} if it exists, or else returns {@code node}
   * with casts removed.
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
