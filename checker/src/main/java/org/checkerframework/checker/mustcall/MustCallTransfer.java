package org.checkerframework.checker.mustcall;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * Transfer function for the must-call type system. Its primary purposes are (1) to create temporary
 * variables for expressions (which allow those expressions to have refined information in the
 * store, which the consistency checker can use), and (2) to reset refined information when a method
 * annotated with @CreatesObligation is called.
 */
public class MustCallTransfer extends CFTransfer {

  /** For building new AST nodes. */
  private final TreeBuilder treeBuilder;

  /** The type factory. */
  private MustCallAnnotatedTypeFactory atypeFactory;

  /**
   * A cache for the default type for java.lang.String, to avoid needing to look it up for every
   * implicit string conversion.
   */
  private @MonotonicNonNull AnnotationMirror defaultStringType;

  /**
   * Create a MustCallTransfer.
   *
   * @param analysis the analysis
   */
  public MustCallTransfer(CFAnalysis analysis) {
    super(analysis);
    atypeFactory = (MustCallAnnotatedTypeFactory) analysis.getTypeFactory();
    ProcessingEnvironment env = atypeFactory.getChecker().getProcessingEnvironment();
    treeBuilder = new TreeBuilder(env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitStringConversion(
      StringConversionNode n, TransferInput<CFValue, CFStore> p) {
    // Implicit String conversions should assume that the String's type is
    // whatever the default for String is, not that the conversion is polymorphic.
    TransferResult<CFValue, CFStore> result = super.visitStringConversion(n, p);
    AnnotationMirror defaultStringType =
        this.defaultStringType != null
            ? this.defaultStringType
            : atypeFactory
                .getAnnotatedType(TypesUtils.getTypeElement(n.getType()))
                .getAnnotationInHierarchy(atypeFactory.TOP);
    LocalVariableNode temp = getOrCreateTempVar(n);
    if (temp != null) {
      JavaExpression localExp = JavaExpression.fromNode(temp);
      insertIntoStores(result, localExp, defaultStringType);
    }
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    updateStoreWithTempVar(result, n);
    if (!atypeFactory.getChecker().hasOption(MustCallChecker.NO_ACCUMULATION_FRAMES)) {
      List<JavaExpression> targetExprs =
          getCreatesObligationExpressions(n, atypeFactory, atypeFactory);
      for (JavaExpression targetExpr : targetExprs) {
        AnnotationMirror defaultType =
            atypeFactory
                .getAnnotatedType(TypesUtils.getTypeElement(targetExpr.getType()))
                .getAnnotationInHierarchy(atypeFactory.TOP);

        if (result.containsTwoStores()) {
          CFStore thenStore = result.getThenStore();
          lubWithStoreValue(thenStore, targetExpr, defaultType);

          CFStore elseStore = result.getElseStore();
          lubWithStoreValue(elseStore, targetExpr, defaultType);
        } else {
          CFStore store = result.getRegularStore();
          lubWithStoreValue(store, targetExpr, defaultType);
        }
      }
    }
    return result;
  }

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
    CFValue defaultTypeAsValue = analysis.createSingleAnnotationValue(defaultType, expr.getType());
    CFValue newValue;
    if (value == null) {
      newValue = defaultTypeAsValue;
    } else {
      newValue = value.leastUpperBound(defaultTypeAsValue);
    }
    store.clearValue(expr);
    store.insertValue(expr, newValue);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitObjectCreation(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  /**
   * Adds newAnno as the value for target to all stores contained in result.
   *
   * @param result a TransferResult containing one or more stores
   * @param target a JavaExpression whose type is being modified
   * @param newAnno the new type for target
   */
  public void insertIntoStores(
      TransferResult<CFValue, CFStore> result, JavaExpression target, AnnotationMirror newAnno) {
    if (result.containsTwoStores()) {
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      thenStore.insertValue(target, newAnno);
      elseStore.insertValue(target, newAnno);
    } else {
      CFStore store = result.getRegularStore();
      store.insertValue(target, newAnno);
    }
  }

  /**
   * Returns the arguments of the @CreatesObligation annotation on the invoked method, as
   * JavaExpressions. Returns the empty set if the given method has no @CreatesObligation
   * annotation.
   *
   * <p>If any expression is unparseable, this method reports an error and returns the empty set.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @param supplier a type factory that can supply the executable elements for CreatesObligation
   *     and CreatesObligation.List's value elements. Usually, you should just pass atypeFactory
   *     again. The arguments are different so that the given type factory's adherence to both
   *     protocols are checked by the type system.
   * @return the arguments of the method's @CreatesObligation annotation, or an empty list
   */
  public static List<JavaExpression> getCreatesObligationExpressions(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesObligationElementSupplier supplier) {
    AnnotationMirror createsObligationList =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.List.class);
    List<JavaExpression> results = new ArrayList<>(1);
    if (createsObligationList != null) {
      // Handle a set of CreatesObligation annotations.
      List<AnnotationMirror> createsObligations =
          AnnotationUtils.getElementValueArray(
              createsObligationList,
              supplier.getCreatesObligationListValueElement(),
              AnnotationMirror.class);
      for (AnnotationMirror co : createsObligations) {
        JavaExpression expr = getCreatesObligationExpression(co, n, atypeFactory, supplier);
        if (expr != null && !results.contains(expr)) {
          results.add(expr);
        }
      }
    }
    AnnotationMirror createsObligation =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.class);
    if (createsObligation != null) {
      JavaExpression expr =
          getCreatesObligationExpression(createsObligation, n, atypeFactory, supplier);
      if (expr != null && !results.contains(expr)) {
        results.add(expr);
      }
    }
    return results;
  }

  /**
   * Parses a single CreatesObligation annotation. See {@link
   * #getCreatesObligationExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory,
   * CreatesObligationElementSupplier)}, which should always be used instead by clients.
   *
   * @param createsObligation a create obligation annotation
   * @param n the method invocation of a reset method
   * @param atypeFactory the type factory.
   * @param supplier a type factory that can supply the executable elements for CreatesObligation
   *     and CreatesObligation.List's value elements. Usually, you should just pass atypeFactory
   *     again. The arguments are different so that the given type factory's adherence to both
   *     protocols are checked by the type system.
   * @return the java expression representing the target, or null if the target is unparseable
   */
  private static @Nullable JavaExpression getCreatesObligationExpression(
      AnnotationMirror createsObligation,
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesObligationElementSupplier supplier) {
    // Unfortunately, there is no way to avoid passing "this" here. The default must be hard-coded
    // into the client, such as here. That is the price for the efficiency of not having to query
    // the annotation definition (such queries are expensive).
    String targetStrWithoutAdaptation =
        AnnotationUtils.getElementValue(
            createsObligation, supplier.getCreatesObligationValueElement(), String.class, "this");
    // TODO: find a way to also check if the target is a known tempvar, and if so return that. That
    // should improve the quality of the error messages we give.
    JavaExpression targetExpr;
    try {
      targetExpr =
          StringToJavaExpression.atMethodInvocation(
              targetStrWithoutAdaptation, n, atypeFactory.getChecker());
      if (targetExpr instanceof Unknown) {
        issueUnparseableError(n, atypeFactory, targetStrWithoutAdaptation);
        return null;
      }
    } catch (JavaExpressionParseException e) {
      issueUnparseableError(n, atypeFactory, targetStrWithoutAdaptation);
      return null;
    }
    return targetExpr;
  }

  /**
   * Issues a mustcall.not.parseable error.
   *
   * @param n the node
   * @param atypeFactory the type factory to use to issue the error
   * @param targetStrWithoutAdaptation the unparseable string
   */
  private static void issueUnparseableError(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      String targetStrWithoutAdaptation) {
    atypeFactory
        .getChecker()
        .reportError(
            n.getTree(),
            "mustcall.not.parseable",
            n.getTarget().getMethod().getSimpleName(),
            targetStrWithoutAdaptation);
  }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as node.
   *
   * @param node the node to be assigned to a temporal variable
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
                .getAnnotationInHierarchy(atypeFactory.TOP);
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
      }
    }
    atypeFactory.tempVars.put(node.getTree(), localVariableNode);
    return localVariableNode;
  }

  /**
   * Creates a variable declaration for the given expression node, if possible.
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
      enclosingElement = TreeUtils.elementFromTree(tree).getEnclosingElement();
    } else {
      ClassTree classTree = TreePathUtil.enclosingClass(path);
      enclosingElement = TreeUtils.elementFromTree(classTree);
    }
    if (enclosingElement == null) {
      return null;
    }
    // Declare and initialize a new, unique iterator variable
    VariableTree tmpVarTree =
        treeBuilder.buildVariableDecl(
            treeType, // annotatedIteratorTypeTree,
            uniqueName("temp-var"),
            enclosingElement,
            tree);
    return tmpVarTree;
  }

  /** A unique identifier counter for node names. */
  protected long uid = 0;

  /**
   * Creates a unique, abitrary string that can be used as a name for a temporary variable, using
   * the given prefix. Can be used up to Long.MAX_VALUE times.
   *
   * @param prefix the prefix for the name
   * @return a unique name that starts with the prefix
   */
  protected String uniqueName(String prefix) {
    return prefix + "-" + uid++;
  }
}
