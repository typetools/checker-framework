package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/** Transfer function for the MustCallOnElements type system. */
public class MustCallOnElementsTransfer extends CFTransfer {

  /** The type factory. */
  private final MustCallOnElementsAnnotatedTypeFactory atypeFactory;

  /** True if -AenableWpiForRlc was passed on the command line. */
  private final boolean enableWpiForRlc;

  /** The processing environment. */
  private final ProcessingEnvironment env;

  /**
   * Create a MustCallOnElementsTransfer.
   *
   * @param analysis the analysis
   */
  public MustCallOnElementsTransfer(CFAnalysis analysis) {
    super(analysis);
    if (analysis.getTypeFactory() instanceof MustCallOnElementsAnnotatedTypeFactory) {
      atypeFactory = (MustCallOnElementsAnnotatedTypeFactory) analysis.getTypeFactory();
    } else {
      atypeFactory =
          new MustCallOnElementsAnnotatedTypeFactory(
              ((MustCallAnnotatedTypeFactory) analysis.getTypeFactory()).getChecker());
    }
    enableWpiForRlc = atypeFactory.getChecker().hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    this.env = atypeFactory.getChecker().getProcessingEnvironment();
  }

  // @Override
  // public TransferResult<CFValue, CFStore> visitVariableDeclaration(
  //     VariableDeclarationNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> res = super.visitVariableDeclaration(node, input);
  //   // since @OwningArray is enforced to be array, the following cast is guaranteed to succeed
  //   VariableElement elmnt = TreeUtils.elementFromDeclaration(node.getTree());
  //   if (atypeFactory.getDeclAnnotation(elmnt, OwningArray.class) != null
  //       && elmnt.getKind() == ElementKind.FIELD) {
  //     TypeMirror componentType = ((ArrayType) elmnt.asType()).getComponentType();
  //     List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
  //     AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
  //     JavaExpression field = JavaExpression.fromVariableTree(node.getTree());
  //     res.getRegularStore().clearValue(field);
  //     res.getRegularStore().insertValue(field, newType);
  //   }
  //   return res;
  // }

  // /**
  //  * Returns the list of mustcall obligations for a type.
  //  *
  //  * @param type the type
  //  * @return the list of mustcall obligations for the type
  //  */
  // private List<String> getMustCallValuesForType(TypeMirror type) {
  //   InheritableMustCall imcAnnotation =
  //       TypesUtils.getClassFromType(type).getAnnotation(InheritableMustCall.class);
  //   MustCall mcAnnotation = TypesUtils.getClassFromType(type).getAnnotation(MustCall.class);
  //   Set<String> mcValues = new HashSet<>();
  //   if (mcAnnotation != null) {
  //     mcValues.addAll(Arrays.asList(mcAnnotation.value()));
  //   }
  //   if (imcAnnotation != null) {
  //     mcValues.addAll(Arrays.asList(imcAnnotation.value()));
  //   }
  //   return new ArrayList<>(mcValues);
  // }

  // @Override
  // public TransferResult<CFValue, CFStore> visitFieldAccess(
  //     FieldAccessNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> res = super.visitFieldAccess(node, input);
  //   Element elmnt = TreeUtils.elementFromTree(node.getTree());
  //   if (atypeFactory.getDeclAnnotation(elmnt, OwningArray.class) != null
  //       && elmnt.getKind() == ElementKind.FIELD) {
  //     // since @OwningArray is enforced to be array, the following cast is guaranteed to succeed
  //     TypeMirror componentType = ((ArrayType) elmnt.asType()).getComponentType();
  //     List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
  //     AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
  //     JavaExpression field = JavaExpression.fromTree((ExpressionTree) node.getTree());
  //     res.getRegularStore().clearValue(field);
  //     res.getRegularStore().insertValue(field, newType);
  //     System.out.println("changed type of: " + node);
  //   }
  //   return res;
  // }

  /*
   * Sets type of LHS to @MustCallOnElementsUnknown if rhs is @OwningArray and lhs is not.
   * The semantics is that LHS is then a read-only copy
   */
  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitAssignment(node, input);
    // return res;
    CFStore store = res.getRegularStore();
    Node lhs = node.getTarget();
    Node rhs = node.getExpression();
    boolean lhsIsOwningArray =
        lhs != null
            && lhs.getTree() != null
            && TreeUtils.elementFromTree(lhs.getTree()) != null
            && TreeUtils.elementFromTree(lhs.getTree()).getAnnotation(OwningArray.class) != null;
    boolean rhsIsOwningArray =
        rhs != null
            && rhs.getTree() != null
            && TreeUtils.elementFromTree(rhs.getTree()) != null
            && TreeUtils.elementFromTree(rhs.getTree()).getAnnotation(OwningArray.class) != null;
    if (!lhsIsOwningArray && rhsIsOwningArray) {
      JavaExpression lhsJavaExpression = JavaExpression.fromNode(lhs);
      store.clearValue(lhsJavaExpression);
      store.insertValue(lhsJavaExpression, getMustCallOnElementsUnknown());
    }
    return new RegularTransferResult<CFValue, CFStore>(res.getResultValue(), store);
  }

  /*
   * Empties the @MustCallOnElements() type of arguments passed as @OwningArray parameters to the
   * constructor and enforces that only @OwningArray arguments are passed to @OwningArray parameters.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitObjectCreation(node, input);
    ExecutableElement constructor = TreeUtils.elementFromUse(node.getTree());
    List<? extends VariableElement> params = constructor.getParameters();
    List<Node> args = node.getArguments();
    Iterator<? extends VariableElement> paramIterator = params.iterator();
    Iterator<Node> argIterator = args.iterator();
    while (paramIterator.hasNext() && argIterator.hasNext()) {
      VariableElement param = paramIterator.next();
      Node arg = argIterator.next();
      boolean paramIsOwningArray = param.getAnnotation(OwningArray.class) != null;
      if (paramIsOwningArray) {
        if (TreeUtils.elementFromTree(arg.getTree()).getAnnotation(OwningArray.class) == null) {
          atypeFactory.getChecker().reportError(node.getTree(), "unexpected.argument.ownership");
        }
        JavaExpression array = JavaExpression.fromNode(arg);
        CFStore store = res.getRegularStore();
        store.clearValue(array);
        store.insertValue(array, getMustCallOnElementsUnknown());
        return new RegularTransferResult<CFValue, CFStore>(res.getResultValue(), store);
      }
    }
    return res;
  }

  /**
   * Returns a list of {@code @MustCall} values of the given node. Returns the empty list if the
   * node has no {@code @MustCall} values or is null.
   *
   * @param node the node
   * @return a list of {@code @MustCall} values of the given node
   */
  private List<String> getMustCallValues(Node node) {
    if (node.getTree() == null || TreeUtils.elementFromTree(node.getTree()) == null) {
      return new ArrayList<>();
    }
    Element elt = TreeUtils.elementFromTree(node.getTree());
    elt = TypesUtils.getTypeElement(elt.asType());
    if (elt == null) {
      return new ArrayList<>();
    }
    MustCallAnnotatedTypeFactory mcAtf =
        new MustCallAnnotatedTypeFactory(atypeFactory.getChecker());
    AnnotationMirror imcAnnotation = mcAtf.getDeclAnnotation(elt, InheritableMustCall.class);
    AnnotationMirror mcAnnotation = mcAtf.getDeclAnnotation(elt, MustCall.class);
    Set<String> mcValues = new HashSet<>();
    if (mcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              mcAnnotation, mcAtf.getMustCallValueElement(), String.class));
    }
    if (imcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              imcAnnotation, mcAtf.getInheritableMustCallValueElement(), String.class));
    }
    return new ArrayList<>(mcValues);
  }

  /**
   * The abstract transformer for Collection.add(E)
   *
   * @param node the {@code MethodInvocationNode}
   * @param res the {@code TransferResult} containing the store to be edited
   * @param receiver JavaExpression of the collection, whose type should be changed
   * @return updated {@code TransferResult}
   */
  private TransferResult<CFValue, CFStore> transformCollectionAdd(
      MethodInvocationNode node, TransferResult<CFValue, CFStore> res, JavaExpression receiver) {
    List<Node> args = node.getArguments();
    assert args.size() == 1
        : "calling abstract transformer for Collection.add(E), but params are: " + args;
    Node arg = args.get(0);
    List<String> mcValues = getMustCallValues(arg);
    CFStore store = res.getRegularStore();
    CFValue oldTypeValue = store.getValue(receiver);
    assert oldTypeValue != null : "Collection " + receiver + " not in Store.";
    AnnotationMirror oldType = oldTypeValue.getAnnotations().first();
    List<String> mcoeMethods = new ArrayList<>();
    if (oldType.getElementValues().get(atypeFactory.getMustCallOnElementsValueElement()) != null) {
      mcoeMethods =
          AnnotationUtils.getElementValueArray(
              oldType, atypeFactory.getMustCallOnElementsValueElement(), String.class);
    }
    mcoeMethods.addAll(mcValues);
    AnnotationMirror newType = getMustCallOnElementsType(new HashSet<>(mcoeMethods));
    store.clearValue(receiver);
    store.insertValue(receiver, newType);
    return new RegularTransferResult<CFValue, CFStore>(res.getResultValue(), store);
  }

  /*
   * The abstract transformer for Collection.add(int,E)
   */
  // private TransferResult<CFValue, CFStore> transformCollectionAddWithIdx(
  //     MethodAccessNode node, TransferResult<CFValue, CFStore> res, List<? extends
  // VariableElement> params) {
  //   System.out.println("collectionAddwithIdx: " + node + params);
  //   return res;
  // }

  /**
   * Responsible for abstract transformers of all methods called on a collection. If the transformer
   * for a method is not specifically implemented, it reports a checker error.
   *
   * @param node a {@code MethodInvocationNode}
   * @param res the {@code TransferResult} containing the store to be edited
   * @return updated {@code TransferResult}
   */
  private TransferResult<CFValue, CFStore> updateStoreForMethodInvocationOnCollection(
      MethodInvocationNode node, TransferResult<CFValue, CFStore> res) {
    MethodAccessNode methodAccessNode = node.getTarget();
    Node receiver = methodAccessNode.getReceiver();
    boolean isCollection =
        receiver.getTree() != null
            && MustCallOnElementsAnnotatedTypeFactory.isCollection(
                receiver.getTree(), atypeFactory);
    boolean isOwningArray =
        receiver.getTree() != null
            && TreeUtils.elementFromTree(receiver.getTree()) != null
            && TreeUtils.elementFromTree(receiver.getTree()).getAnnotation(OwningArray.class)
                != null;
    JavaExpression receiverJx = JavaExpression.fromNode(receiver);
    Tree receiverTree = receiver.getTree();
    boolean isAlias =
        receiverTree != null // ensure tree for collection is valid
            && res.getRegularStore() != null // ensure store exists
            && atypeFactory.isMustCallOnElementsUnknown(res.getRegularStore(), receiverTree);
    if (isCollection && (isOwningArray || isAlias)) {
      ExecutableElement method = methodAccessNode.getMethod();
      List<? extends VariableElement> parameters = method.getParameters();
      String methodSignature =
          method.getSimpleName().toString()
              + parameters.stream()
                  .map(param -> param.asType().toString())
                  .collect(Collectors.joining(",", "(", ")"));
      System.out.println("methodsignature: " + methodSignature);
      switch (methodSignature) {
        case "add(E)":
          res = transformCollectionAdd(node, res, receiverJx);
          break;
          // case "add(int,E)":
          //   res = transformCollectionAddWithIdx(node, res, parameters);
          //   break;
        default:
          atypeFactory.getChecker().reportError(node.getTree(), "unsafe.method", methodSignature);
      }
    }
    return res;
  }

  /*
   * Responsible for:
   *
   * Abstract transformers of all methods called on a collection.
   * If the transformer for a method is not specifically implemented, it reports a checker error.
   *
   * Emptying the @MustCallOnElements type of arguments passed as @OwningArray parameters to the
   * method.
   *
   * Enforcing that only @OwningArray arguments are passed to @OwningArray parameters.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitMethodInvocation(node, input);

    // checks if method is called on a collection, in which case it calls the transformer of the
    // respective method on the collection
    res = updateStoreForMethodInvocationOnCollection(node, res);

    // ensure method call args respects ownership consistency
    // also, empty mcoe type of @OwningArray args that are passed as @OwningArray params
    ExecutableElement method = node.getTarget().getMethod();
    List<? extends VariableElement> params = method.getParameters();
    List<Node> args = node.getArguments();
    Iterator<? extends VariableElement> paramIterator = params.iterator();
    Iterator<Node> argIterator = args.iterator();
    while (paramIterator.hasNext() && argIterator.hasNext()) {
      VariableElement param = paramIterator.next();
      Node arg = argIterator.next();
      Element argElt = arg.getTree() != null ? TreeUtils.elementFromTree(arg.getTree()) : null;
      boolean argIsOwningArray = argElt != null && argElt.getAnnotation(OwningArray.class) != null;
      boolean paramIsOwningArray = param != null && param.getAnnotation(OwningArray.class) != null;
      boolean argIsMcoeUnknown =
          atypeFactory.isMustCallOnElementsUnknown(res.getRegularStore(), arg.getTree());
      if (argIsMcoeUnknown) {
        atypeFactory
            .getChecker()
            .reportError(arg.getTree(), "argument.with.revoked.ownership", arg.getTree());
      } else if (paramIsOwningArray) {
        if (!argIsOwningArray) {
          atypeFactory.getChecker().reportError(arg.getTree(), "unexpected.argument.ownership");
        }
        JavaExpression array = JavaExpression.fromNode(arg);
        CFStore store = res.getRegularStore();
        store.clearValue(array);
        store.insertValue(array, getMustCallOnElementsType(new HashSet<>()));
        return new RegularTransferResult<CFValue, CFStore>(res.getResultValue(), store);
      } else if (argIsOwningArray) {
        // param non-@OwningArray and arg @OwningArray would imply we have an alias
        atypeFactory.getChecker().reportError(arg.getTree(), "unexpected.argument.ownership");
      }
    }
    return res;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitLessThan(node, input);
    BinaryTree tree = node.getTree();
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "failed assumption: binaryTree in calledmethodsonelements transfer function is not lessthan tree";
    Set<String> newMustCallMethods =
        MustCallOnElementsAnnotatedTypeFactory.whichObligationsDoesLoopWithThisConditionCreate(
            tree);
    Set<String> calledMethods =
        MustCallOnElementsAnnotatedTypeFactory.whichMethodsDoesLoopWithThisConditionCall(tree);
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getCollectionTreeForLoopWithThisCondition(tree);
    if (arrayTree == null) return res;
    CFStore elseStore = res.getElseStore();
    JavaExpression receiverReceiver = JavaExpression.fromTree(arrayTree);
    if (newMustCallMethods != null) {
      // this is an obligation-creating loop
      AnnotationMirror newType = getMustCallOnElementsType(newMustCallMethods);
      CFValue oldCFVal = elseStore.getValue(receiverReceiver);
      CFValue newCFVal = analysis.createSingleAnnotationValue(newType, receiverReceiver.getType());
      newCFVal =
          oldCFVal == null
              ? newCFVal
              : oldCFVal.leastUpperBound(newCFVal, receiverReceiver.getType());
      elseStore.replaceValue(receiverReceiver, newCFVal);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    } else if (calledMethods != null && calledMethods.size() > 0) {
      // this loop fulfills an obligation - remove that methodname from
      // the MustCallOnElements type of the array
      CFValue oldTypeValue = elseStore.getValue(receiverReceiver);
      assert oldTypeValue != null : "Array " + arrayTree + " not in Store.";
      AnnotationMirror oldType = oldTypeValue.getAnnotations().first();
      List<String> mcoeMethods = new ArrayList<>();
      if (oldType.getElementValues().get(atypeFactory.getMustCallOnElementsValueElement())
          != null) {
        mcoeMethods =
            AnnotationUtils.getElementValueArray(
                oldType, atypeFactory.getMustCallOnElementsValueElement(), String.class);
      }
      mcoeMethods.removeAll(calledMethods);
      AnnotationMirror newType = getMustCallOnElementsType(new HashSet<>(mcoeMethods));
      elseStore.clearValue(receiverReceiver);
      elseStore.insertValue(receiverReceiver, newType);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }
    return res;
  }

  /**
   * Generate an annotation from a list of method names.
   *
   * @param methodNames the names of the methods to add to the type
   * @return the annotation with the given methods as value
   */
  private @Nullable AnnotationMirror getMustCallOnElementsType(Set<String> methodNames) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, atypeFactory.BOTTOM);
    builder.setValue(
        "value", CollectionsPlume.withoutDuplicatesSorted(new ArrayList<>(methodNames)));
    return builder.build();
  }

  /**
   * Return a {@code @MustCallOnElementsUnknown} annotation.
   *
   * @return a {@code @MustCallOnElementsUnknown} AnnotationMirror.
   */
  private AnnotationMirror getMustCallOnElementsUnknown() {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, atypeFactory.TOP);
    return builder.build();
  }

  /**
   * @param tree a tree
   * @return false if Resource Leak Checker is running as one of the upstream checkers and the
   *     -AenableWpiForRlc flag is not passed as a command line argument, otherwise returns the
   *     result of the super call
   */
  @Override
  protected boolean shouldPerformWholeProgramInference(Tree tree) {
    if (!isWpiEnabledForRLC()
        && atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
      return false;
    }
    return super.shouldPerformWholeProgramInference(tree);
  }

  /**
   * @param expressionTree a tree
   * @param lhsTree its element
   * @return false if Resource Leak Checker is running as one of the upstream checkers and the
   *     -AenableWpiForRlc flag is not passed as a command line argument, otherwise returns the
   *     result of the super call
   */
  @Override
  protected boolean shouldPerformWholeProgramInference(Tree expressionTree, Tree lhsTree) {
    if (!isWpiEnabledForRLC()
        && atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
      return false;
    }
    return super.shouldPerformWholeProgramInference(expressionTree, lhsTree);
  }

  /**
   * Checks if WPI is enabled for the Resource Leak Checker inference. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
   * @return returns true if WPI is enabled for the Resource Leak Checker
   */
  protected boolean isWpiEnabledForRLC() {
    return enableWpiForRlc;
  }

  // /**
  //  * Pretty-prints a list of mustcall values into a string to output in a warning message.
  //  *
  //  * @param mustCallVal a list of mustcall values
  //  * @return a string, which is a pretty-print of the method list
  //  */
  // private String formatMissingMustCallMethods(List<String> mustCallVal) {
  //   int size = mustCallVal.size();
  //   if (size == 0) {
  //     return "None";
  //   } else if (size == 1) {
  //     return "method " + mustCallVal.get(0);
  //   } else {
  //     return "methods " + String.join(", ", mustCallVal);
  //   }
  // }
}
