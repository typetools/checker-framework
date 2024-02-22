package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.plumelib.util.CollectionsPlume;

/**
 * Transfer function for the must-call type system. Its primary purposes are (1) to create temporary
 * variables for expressions (which allow those expressions to have refined information in the
 * store, which the consistency checker can use), and (2) to reset refined information when a method
 * annotated with @CreatesMustCallFor is called.
 */
public class MustCallOnElementsTransfer extends CFTransfer {

  /** For building new AST nodes. */
  private final TreeBuilder treeBuilder;

  /** The type factory. */
  private final MustCallOnElementsAnnotatedTypeFactory atypeFactory;

  /**
   * A cache for the default type for java.lang.String, to avoid needing to look it up for every
   * implicit string conversion. See {@link #getDefaultStringType(StringConversionNode)}.
   */
  private @MonotonicNonNull AnnotationMirror defaultStringType;

  /**
   * True if -AenableWpiForRlc was passed on the command line. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   */
  private final boolean enableWpiForRlc;

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
    treeBuilder = new TreeBuilder(env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> input) {
    if (!(atypeFactory instanceof MustCallOnElementsAnnotatedTypeFactory)) {
      return super.visitLessThan(node, input);
    }
    TransferResult<CFValue, CFStore> res = super.visitLessThan(node, input);
    BinaryTree tree = node.getTree();
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "failed assumption: binaryTree in calledmethodsonelements transfer function is not lessthan tree";

    List<String> mustcallMethods =
        MustCallOnElementsAnnotatedTypeFactory.whichObligationsDoesLoopWithThisConditionCreate(
            tree);
    if (mustcallMethods != null) {
      CFStore thenStore = res.getElseStore();
      CFStore elseStore = res.getElseStore();
      ExpressionTree arrayTree =
          MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(
              node.getTree());
      AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(arrayTree);
      AnnotationMirror newType = getUpdatedMustCallOnElementsType(currentType, mustcallMethods);
      JavaExpression receiverReceiver = JavaExpression.fromTree(arrayTree);
      thenStore.clearValue(receiverReceiver);
      thenStore.insertValue(receiverReceiver, newType);
      elseStore.clearValue(receiverReceiver);
      elseStore.insertValue(receiverReceiver, newType);

      return new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
    }
    return res;
  }

  /**
   * Extract the current called-methods type from {@code currentType}, and then add each element of
   * {@code methodNames} to it, and return the result. This method is similar to GLB, but should be
   * used when the new methods come from a source other than an {@code CalledMethodsOnElements}
   * annotation.
   *
   * @param currentType the current type in the called-methods hierarchy
   * @param methodNames the names of the new methods to add to the type
   * @return the new annotation to be added to the type, or null if the current type cannot be
   *     converted to an accumulator annotation
   */
  private @Nullable AnnotationMirror getUpdatedMustCallOnElementsType(
      AnnotatedTypeMirror currentType, List<String> methodNames) {
    AnnotationMirror type;
    if (currentType == null || !currentType.hasPrimaryAnnotationInHierarchy(atypeFactory.TOP)) {
      type = atypeFactory.TOP;
    } else {
      AnnotatedArrayType curType = (AnnotatedArrayType) currentType;
      type = curType.getComponentType().getPrimaryAnnotation(MustCallOnElements.class);
    }

    ExecutableElement valueElement = atypeFactory.getMustCallOnElementsValueElement();
    List<String> currentMethods =
        AnnotationUtils.getElementValueArray(type, valueElement, String.class);
    List<String> newList = CollectionsPlume.concatenate(currentMethods, methodNames);

    return createAccumulatorAnnotation(newList, type);
  }

  /**
   * Creates a new instance of the accumulator annotation that contains the elements of {@code
   * values}.
   *
   * @param values the arguments to the annotation. The values can contain duplicates and can be in
   *     any order.
   * @return an annotation mirror representing the accumulator annotation with {@code values}'s
   *     arguments; this is top if {@code values} is empty
   */
  public AnnotationMirror createAccumulatorAnnotation(List<String> values, AnnotationMirror type) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
    builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(values));
    return builder.build();
  }

  /**
   * Creates a new instance of the accumulator annotation that contains exactly one value.
   *
   * @param value the argument to the annotation
   * @return an annotation mirror representing the accumulator annotation with {@code value} as its
   *     argument
   */
  public AnnotationMirror createAccumulatorAnnotation(String value, AnnotationMirror type) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
    builder.setValue("value", Collections.singletonList(value));
    return builder.build();
  }

  // @Override
  // public TransferResult<CFValue, CFStore> visitMethodInvocation(
  //     MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
  //   TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

  //   updateStoreWithTempVar(result, n);
  //   if (!noCreatesMustCallFor) {
  //     List<JavaExpression> targetExprs =
  //         CreatesMustCallForToJavaExpression.getCreatesMustCallForExpressionsAtInvocation(
  //             n, atypeFactory, atypeFactory);
  //     for (JavaExpression targetExpr : targetExprs) {
  //       AnnotationMirror defaultType =
  //           atypeFactory
  //               .getAnnotatedType(TypesUtils.getTypeElement(targetExpr.getType()))
  //               .getPrimaryAnnotationInHierarchy(atypeFactory.TOP);

  //       if (result.containsTwoStores()) {
  //         CFStore thenStore = result.getThenStore();
  //         lubWithStoreValue(thenStore, targetExpr, defaultType);

  //         CFStore elseStore = result.getElseStore();
  //         lubWithStoreValue(elseStore, targetExpr, defaultType);
  //       } else {
  //         CFStore store = result.getRegularStore();
  //         lubWithStoreValue(store, targetExpr, defaultType);
  //       }
  //     }
  //   }
  //   return result;
  // }

  /**
   * Computes the LUB of the current value in the store for expr, if it exists, and defaultType.
   * Inserts that LUB into the store as the new value for expr.
   *
   * @param store a CFStore
   * @param expr an expression that might be in the store
   * @param defaultType the default type of the expression's static type
   */
  private void lubWithStoreValue(CFStore store, JavaExpression expr, AnnotationMirror defaultType) {
    CFValue value = store.getValue(expr);
    CFValue defaultTypeAsCFValue =
        analysis.createSingleAnnotationValue(defaultType, expr.getType());
    CFValue newValue = defaultTypeAsCFValue.leastUpperBound(value);
    store.clearValue(expr);
    store.insertValue(expr, newValue);
  }

  /**
   * See {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
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
   * See {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
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

  // @Override
  // public TransferResult<CFValue, CFStore> visitObjectCreation(
  //     ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> result = super.visitObjectCreation(node, input);
  //   updateStoreWithTempVar(result, node);
  //   return result;
  // }

  // @Override
  // public TransferResult<CFValue, CFStore> visitTernaryExpression(
  //     TernaryExpressionNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(node, input);
  //   if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
  //     // Add the synthetic variable created during CFG construction to the temporary
  //     // variable map (rather than creating a redundant temp var)
  //     atypeFactory.tempVars.put(node.getTree(), node.getTernaryExpressionVar());
  //   }
  //   return result;
  // }

  // @Override
  // public TransferResult<CFValue, CFStore> visitSwitchExpressionNode(
  //     SwitchExpressionNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> result = super.visitSwitchExpressionNode(node, input);
  //   if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
  //     // Add the synthetic variable created during CFG construction to the temporary
  //     // variable map (rather than creating a redundant temp var)
  //     atypeFactory.tempVars.put(node.getTree(), node.getSwitchExpressionVar());
  //   }
  //   return result;
  // }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as {@code node}.
   *
   * @param node the node to be assigned to a temporary variable
   * @param result the transfer result containing the store to be modified
   */
  public void updateStoreWithTempVar(TransferResult<CFValue, CFStore> result, Node node) {
    // Must-call obligations on primitives are not supported.
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      LocalVariableNode temp = getOrCreateTempVar(node);
      if (temp != null) {
        JavaExpression localExp = JavaExpression.fromNode(temp);
        AnnotationMirror anm =
            atypeFactory
                .getAnnotatedType(node.getTree())
                .getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
        insertIntoStores(result, localExp, anm == null ? atypeFactory.TOP : anm);
      }
    }
  }

  /**
   * Either returns the temporary variable associated with node, or creates one if one does not
   * exist.
   *
   * @param node a node, which must be an expression (not a statement)
   * @return a temporary variable node representing {@code node} that can be placed into a store
   */
  private @Nullable LocalVariableNode getOrCreateTempVar(Node node) {
    LocalVariableNode localVariableNode = atypeFactory.tempVars.get(node.getTree());
    if (localVariableNode == null) {
      VariableTree temp = createTemporaryVar(node);
      if (temp != null) {
        IdentifierTree identifierTree = treeBuilder.buildVariableUse(temp);
        localVariableNode = new LocalVariableNode(identifierTree);
        localVariableNode.setInSource(true);
        atypeFactory.tempVars.put(node.getTree(), localVariableNode);
      }
    }
    return localVariableNode;
  }

  /**
   * Creates a variable declaration for the given expression node, if possible.
   *
   * <p>Note that error reporting code assumes that the names of temporary variables are not legal
   * Java identifiers (see <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-3.html#jls-3.8">JLS 3.8</a>). The
   * temporary variable names generated here include an {@code '-'} character to make the names
   * invalid.
   *
   * @param node an expression node
   * @return a variable tree for the node, or null if an appropriate containing element cannot be
   *     located
   */
  protected @Nullable VariableTree createTemporaryVar(Node node) {
    ExpressionTree tree = (ExpressionTree) node.getTree();
    TypeMirror treeType = TreeUtils.typeOf(tree);
    Element enclosingElement;
    TreePath path = atypeFactory.getPath(tree);
    if (path == null) {
      enclosingElement = TreeUtils.elementFromUse(tree).getEnclosingElement();
    } else {
      ClassTree classTree = TreePathUtil.enclosingClass(path);
      enclosingElement = TreeUtils.elementFromDeclaration(classTree);
    }
    if (enclosingElement == null) {
      return null;
    }
    // Declare and initialize a new, unique variable
    VariableTree tmpVarTree =
        treeBuilder.buildVariableDecl(treeType, uniqueName("temp-var"), enclosingElement, tree);
    return tmpVarTree;
  }

  /** A unique identifier counter for node names. */
  private static AtomicLong uid = new AtomicLong();

  /**
   * Creates a unique, arbitrary string that can be used as a name for a temporary variable, using
   * the given prefix. Can be used up to Long.MAX_VALUE times.
   *
   * <p>Note that the correctness of the Resource Leak Checker depends on these names actually being
   * unique, because {@code LocalVariableNode}s derived from them are used as keys in a map.
   *
   * @param prefix the prefix for the name
   * @return a unique name that starts with the prefix
   */
  protected String uniqueName(String prefix) {
    return prefix + "-" + uid.getAndIncrement();
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
}
