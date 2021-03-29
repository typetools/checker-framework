package org.checkerframework.checker.mustcall;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
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
 * Transfer function for the must-call type system. Handles defaulting for string concatenations and
 * some logic for creating temporary variables for expressions.
 */
public class MustCallTransfer extends CFTransfer {

  /** TreeBuilder for building new AST nodes */
  private final TreeBuilder treeBuilder;

  /** The type factory */
  private MustCallAnnotatedTypeFactory atypeFactory;

  /**
   * Create a MustCallTransfer
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
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    updateStoreWithTempVar(result, n);
    if (!atypeFactory.getChecker().hasOption(MustCallChecker.NO_ACCUMULATION_FRAMES)) {
      Set<JavaExpression> targetExprs = getCreatesObligationExpressions(n, atypeFactory);
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
   * Takes the LUB of the current value in the store for expr, if it exists, and defaultType.
   * Inserts the result into the store as the new value for expr.
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
   * If the given method invocation node is a CreatesObligation method, then gets the
   * JavaExpressions corresponding to the targets. If any expression is unparseable, this method
   * uses the type factory's error reporting inferface to throw an error and returns the empty set.
   * Also return the empty set if the given method is not a CreatesObligation method.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @return a list of JavaExpressions representing the targets, if the method is a
   *     CreatesObligation method and the targets are parseable; the empty set otherwise.
   */
  public static Set<JavaExpression> getCreatesObligationExpressions(
      MethodInvocationNode n, GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    return getCreatesObligationExpressions(n, atypeFactory, null);
  }

  /**
   * If the given method invocation node is a CreatesObligation method, then gets the
   * JavaExpressions corresponding to the targets. If any expression is unparseable, this method
   * uses the type factory's error reporting inferface to throw an error and returns the empty set.
   * Also return the empty set if the given method is not a CreatesObligation method.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @param currentPath the path to n, if it is already available, to avoid potentially-expensive
   *     recomputation. If null, the path will be computed.
   * @return a list of JavaExpressions representing the targets, if the method is a
   *     CreatesObligation method and the targets are parseable; the empty set otherwise.
   */
  public static Set<JavaExpression> getCreatesObligationExpressions(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      @Nullable TreePath currentPath) {
    AnnotationMirror createsObligation =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.class);
    if (createsObligation == null) {
      AnnotationMirror createsObligationList =
          atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.List.class);
      if (createsObligationList == null) {
        return Collections.emptySet();
      }
      // Handle a set of create obligation annotations.
      List<AnnotationMirror> createsObligations =
          AnnotationUtils.getElementValueArray(
              createsObligationList, "value", AnnotationMirror.class, false);
      Set<JavaExpression> results = new HashSet<>();
      if (currentPath == null) {
        currentPath = atypeFactory.getPath(n.getTree());
      }
      for (AnnotationMirror co : createsObligations) {
        JavaExpression expr = getCreatesObligationExpressionsImpl(co, n, atypeFactory, currentPath);
        if (expr != null) {
          results.add(expr);
        }
      }
      return results;
    }
    // Handle a single create obligation annotation.
    if (currentPath == null) {
      currentPath = atypeFactory.getPath(n.getTree());
    }
    JavaExpression expr =
        getCreatesObligationExpressionsImpl(createsObligation, n, atypeFactory, currentPath);
    return expr != null ? Collections.singleton(expr) : Collections.emptySet();
  }

  /**
   * Implementation of parsing a single CreatesObligation annotation. See {@link
   * #getCreatesObligationExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory)}.
   *
   * @param createsObligation a create obligation annotation
   * @param n the method invocation of a reset method
   * @param atypeFactory the type factory
   * @return the java expression representing the target, or null if the target is unparseable
   */
  private static @Nullable JavaExpression getCreatesObligationExpressionsImpl(
      AnnotationMirror createsObligation,
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      TreePath currentPath) {
    String targetStrWithoutAdaptation =
        AnnotationUtils.getElementValue(createsObligation, "value", String.class, true);
    // Note that it *is* necessary to parse this string twice - the first time to standardize
    // and viewpoint adapt it via the utility method called on the next line, and the second
    // time (in the try block below) to actually get the relevant expression.
    String targetStr =
        MustCallTransfer.standardizeAndViewpointAdapt(
            targetStrWithoutAdaptation, n, atypeFactory.getChecker());
    // TODO: find a way to also check if the target is a known tempvar, and if so return that. That
    // should
    // improve the quality of the error messages we give, e.g. in tests/socket/BindChannel.java.
    JavaExpression targetExpr;
    try {
      targetExpr = atypeFactory.parseJavaExpressionString(targetStr, currentPath);
    } catch (JavaExpressionParseException e) {
      atypeFactory
          .getChecker()
          .reportError(
              n.getTree(),
              "mustcall.not.parseable",
              n.getTarget().getMethod().getSimpleName(),
              targetStr);
      return null;
    }
    return targetExpr;
  }

  /*
   * Helper function to standardize and viewpoint adapt a String given a path and a context.
   * Wraps JavaExpressionParseUtil#parse. If a parse exception is encountered, this returns
   * its argument.
   */
  public static String standardizeAndViewpointAdapt(
      String s, MethodInvocationNode miNode, BaseTypeChecker checker) {
    try {
      return StringToJavaExpression.atMethodInvocation(s, miNode, checker).toString();
    } catch (JavaExpressionParseException e) {
      return s;
    }
  }

  @Override
  public TransferResult<CFValue, CFStore> visitStringConcatenate(
      StringConcatenateNode n, TransferInput<CFValue, CFStore> input) {
    return handleStringConcatenation(super.visitStringConcatenate(n, input));
  }

  @Override
  public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(
      StringConcatenateAssignmentNode n, TransferInput<CFValue, CFStore> in) {
    return handleStringConcatenation(super.visitStringConcatenateAssignment(n, in));
  }

  private TransferResult<CFValue, CFStore> handleStringConcatenation(
      TransferResult<CFValue, CFStore> result) {
    TypeMirror underlyingType = result.getResultValue().getUnderlyingType();
    CFValue newValue = analysis.createSingleAnnotationValue(atypeFactory.BOTTOM, underlyingType);
    return new RegularTransferResult<>(newValue, result.getRegularStore());
  }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as node
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

  protected @Nullable VariableTree createTemporaryVar(Node method) {
    ExpressionTree tree = (ExpressionTree) method.getTree();
    TypeMirror treeType = TreeUtils.typeOf(tree);
    Element enclosingElement = null;
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

  protected long uid = 0;

  protected String uniqueName(String prefix) {
    return prefix + "-" + uid++;
  }
}
