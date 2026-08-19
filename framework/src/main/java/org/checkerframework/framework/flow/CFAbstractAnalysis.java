package org.checkerframework.framework.flow;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * {@link CFAbstractAnalysis} is an extensible org.checkerframework.dataflow analysis for the
 * Checker Framework that tracks the annotations using a flow-sensitive analysis. It uses an {@link
 * AnnotatedTypeFactory} to provide checker-specific logic how to combine types (e.g., what is the
 * type of a string concatenation, given the types of the two operands) and as an abstraction
 * function (e.g., determine the annotations on literals).
 *
 * <p>The purpose of this class is twofold: Firstly, it serves as factory for abstract values,
 * stores and the transfer function. Furthermore, it makes it easy for the transfer function and the
 * stores to access the {@link AnnotatedTypeFactory}, the qualifier hierarchy, etc.
 */
public abstract class CFAbstractAnalysis<
        V extends CFAbstractValue<V>,
        S extends CFAbstractStore<V, S>,
        T extends CFAbstractTransfer<V, S, T>>
    extends ForwardAnalysisImpl<V, S, T> {
  /** The qualifier hierarchy for which to track annotations. */
  protected final QualifierHierarchy qualHierarchy;

  /** The type hierarchy. */
  protected final TypeHierarchy typeHierarchy;

  /**
   * The dependent type helper used to standardize both annotations belonging to the type hierarchy,
   * and contract expressions.
   */
  protected final DependentTypesHelper dependentTypesHelper;

  /** A type factory that can provide static type annotations for AST Trees. */
  protected final GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>>
      atypeFactory;

  /** A checker that contains command-line arguments and other information. */
  protected final SourceChecker checker;

  /**
   * A triple of field, value corresponding to the annotations on its declared type, value of its
   * initializer. The value of the initializer is {@code null} if the field does not have one.
   *
   * @param <V> type of value
   * @param fieldDecl a field access that corresponds to the declaration of a field
   * @param declared the value corresponding to the annotations on the declared type of the field
   * @param initializer the value of the initializer of the field, or null if no initializer exists
   */
  public record FieldInitialValue<V extends CFAbstractValue<V>>(
      FieldAccess fieldDecl, V declared, @Nullable V initializer) {}

  /** Initial abstract types for fields. */
  protected final List<FieldInitialValue<V>> fieldValues;

  /** The associated processing environment. */
  protected final ProcessingEnvironment env;

  /** Instance of the types utility. */
  protected final Types types;

  /**
   * Cache for {@link #getSideEffectsOnlyExpressions}, which would otherwise re-parse the
   * annotation's expressions once per dataflow iteration per call site. A key that is mapped to
   * null stands for the null result, so test membership with {@link Map#containsKey} rather than
   * comparing the result of {@link Map#get} to null.
   *
   * <p>The keys are nodes of a single control flow graph, so {@link #performAnalysis} clears this.
   *
   * <p>This is an {@link IdentityHashMap} because the cached values are viewpoint-adapted to a
   * particular call site, but {@link MethodInvocationNode#equals} compares only the target and the
   * arguments. Two call sites that are equal in that sense need not adapt to the same expressions,
   * so a hash map keyed by {@code equals} could return one call site's expressions for another. The
   * same node object is passed on every dataflow iteration, so identity keys still hit.
   */
  private final IdentityHashMap<MethodInvocationNode, @Nullable List<JavaExpression>>
      sideEffectsOnlyExpressionsCache = new IdentityHashMap<>();

  /**
   * Create a CFAbstractAnalysis.
   *
   * @param checker a checker that contains command-line arguments and other information
   * @param factory an annotated type factory to introduce type and dataflow rules
   * @param maxCountBeforeWidening number of times a block can be analyzed before widening
   */
  @SuppressWarnings("this-escape")
  protected CFAbstractAnalysis(
      BaseTypeChecker checker,
      GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
      int maxCountBeforeWidening) {
    super(maxCountBeforeWidening);
    env = checker.getProcessingEnvironment();
    types = env.getTypeUtils();
    qualHierarchy = factory.getQualifierHierarchy();
    typeHierarchy = factory.getTypeHierarchy();
    dependentTypesHelper = factory.getDependentTypesHelper();
    this.atypeFactory = factory;
    this.checker = checker;
    this.transferFunction = createTransferFunction();
    this.fieldValues = new ArrayList<>();
  }

  /**
   * Create a CFAbstractAnalysis.
   *
   * @param checker a checker that contains command-line arguments and other information
   * @param factory an annotated type factory to introduce type and dataflow rules
   */
  protected CFAbstractAnalysis(
      BaseTypeChecker checker,
      GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory) {
    this(checker, factory, factory.getQualifierHierarchy().numberOfIterationsBeforeWidening());
  }

  /**
   * Analyze the given control flow graph.
   *
   * @param cfg control flow graph to analyze
   * @param fieldValues initial values of the fields
   */
  public void performAnalysis(ControlFlowGraph cfg, List<FieldInitialValue<V>> fieldValues) {
    this.fieldValues.clear();
    this.fieldValues.addAll(fieldValues);
    // The cache's keys are nodes of the control flow graph that was analyzed previously.
    sideEffectsOnlyExpressionsCache.clear();
    super.performAnalysis(cfg);
  }

  /**
   * Returns the expressions that the invoked method side-effects (specified as arguments/elements
   * of {@code @SideEffectsOnly}), viewpoint-adapted to the given method invocation. Returns null if
   * the invoked method has no {@code @SideEffectsOnly} annotation.
   *
   * <p>Also returns null if any of the annotation's expressions cannot be parsed at the call site.
   * Null means "the method might side-effect anything", which is the conservative result; returning
   * a list that omits the unparseable expression would treat the method as side-effecting
   * <em>less</em> than it was declared to.
   *
   * <p>The result is cached, because dataflow calls this once per iteration per call site and
   * parsing an expression is not cheap. Clients should not side-effect the returned value, which is
   * aliased to internal state.
   *
   * @param methodInvocationNode the call site at which the side-effecting expressions will be used
   * @return the expressions that the method side-effects, viewpoint-adapted to the given
   *     invocation; or null if the method has no valid {@code @SideEffectsOnly} annotation
   */
  public @Nullable List<JavaExpression> getSideEffectsOnlyExpressions(
      MethodInvocationNode methodInvocationNode) {
    if (sideEffectsOnlyExpressionsCache.containsKey(methodInvocationNode)) {
      return sideEffectsOnlyExpressionsCache.get(methodInvocationNode);
    }
    ExecutableElement method = methodInvocationNode.getTarget().getMethod();
    List<JavaExpression> result = computeSideEffectsOnlyExpressions(method, methodInvocationNode);
    sideEffectsOnlyExpressionsCache.put(methodInvocationNode, result);
    return result;
  }

  /**
   * Computes the value that {@link #getSideEffectsOnlyExpressions} caches and returns; see that
   * method for the specification.
   *
   * @param method a method
   * @param methodInvocationNode the call site at which the side-effecting expressions will be used
   * @return the expressions that the method side-effects, viewpoint-adapted to the given
   *     invocation, or null
   */
  private @Nullable List<JavaExpression> computeSideEffectsOnlyExpressions(
      ExecutableElement method, MethodInvocationNode methodInvocationNode) {
    Map<ExecutableElement, List<String>> seOnlyExpressionStrings =
        atypeFactory.getSideEffectsOnlyExpressionStrings(method);
    if (seOnlyExpressionStrings == null) {
      return null;
    }

    List<JavaExpression> seOnlyExpressions = new ArrayList<>();

    for (Map.Entry<ExecutableElement, List<String>> entry : seOnlyExpressionStrings.entrySet()) {
      // The method whose `@SideEffectsOnly` annotation contains the expressions.  It is `method`
      // itself, unless `method` inherits the annotation.
      ExecutableElement declaringMethod = entry.getKey();
      for (String seOnlyExpr : entry.getValue()) {
        try {
          // An expression is parsed in the scope of the method that declares it, which is not
          // necessarily the scope of `method`:  a field that the declaring method's class declares
          // might be shadowed or inaccessible in `method`'s class.
          // Do not use `StringToJavaExpression.atMethodInvocation(seOnlyExpr,
          // methodInvocationNode, checker)`, which obtains the invoked method from
          // `methodInvocationNode.getTree()`; that tree is null for a call that corresponds to no
          // AST tree, such as the `Iterator.next()` that an enhanced for loop is desugared to.
          JavaExpression exprJe =
              StringToJavaExpression.atMethodDecl(seOnlyExpr, declaringMethod, checker)
                  .atMethodInvocation(methodInvocationNode);

          if (exprJe.containsUnknown()) {
            // Nothing in the store can match an `Unknown`, so returning the expression would
            // discard no refinement at all.  Returning null makes the caller discard every
            // refinement.
            return null;
          }

          // At a call of the form `super.m()`, viewpoint-adapting the callee's `this` yields
          // `super`.
          // The caller refers to that same object as `this`, so rewrite it that way; otherwise
          // the refinements of `this` and of its fields would not be discarded.
          exprJe = JavaExpression.superToThis(exprJe);
          seOnlyExpressions.add(exprJe);
        } catch (JavaExpressionParseException ex) {
          // The expression cannot be represented at the call site, so the caller must assume that
          // the call might side-effect anything.  A future change will report the parse error.
          return null;
        }
      }
    }

    return seOnlyExpressions;
  }

  /**
   * A list of initial abstract values for the fields.
   *
   * @return a list of initial abstract values for the fields
   */
  public List<FieldInitialValue<V>> getFieldInitialValues() {
    return fieldValues;
  }

  /**
   * Returns the transfer function to be used by the analysis.
   *
   * @return the transfer function to be used by the analysis
   */
  public T createTransferFunction() {
    return atypeFactory.createFlowTransferFunction(this);
  }

  /**
   * Returns an empty store of the appropriate type.
   *
   * @return an empty store of the appropriate type
   */
  public abstract S createEmptyStore(boolean sequentialSemantics);

  /**
   * Returns an identical copy of the store {@code s}.
   *
   * @return an identical copy of the store {@code s}
   */
  public abstract S createCopiedStore(S s);

  /**
   * Creates an abstract value from the annotated type mirror. The value contains the set of primary
   * annotations on the type, unless the type is an AnnotatedWildcardType. For an
   * AnnotatedWildcardType, the annotations in the created value are the primary annotations on the
   * extends bound. See {@link CFAbstractValue} for an explanation.
   *
   * @param type the type to convert into an abstract value
   * @return an abstract value containing the given annotated {@code type} or null if {@code type}
   *     is missing annotations
   */
  public @Nullable V createAbstractValue(AnnotatedTypeMirror type) {
    AnnotationMirrorSet annos;
    if (type.getKind() == TypeKind.WILDCARD) {
      annos = ((AnnotatedWildcardType) type).getExtendsBound().getPrimaryAnnotations();
    } else if (TypesUtils.isCapturedTypeVariable(type.getUnderlyingType())) {
      annos = ((AnnotatedTypeVariable) type).getUpperBound().getPrimaryAnnotations();
    } else {
      annos = type.getPrimaryAnnotations();
    }
    return createAbstractValue(annos, type.getUnderlyingType());
  }

  /**
   * Returns an abstract value containing the given {@code annotations} and {@code underlyingType}.
   * Returns null if the annotation set has missing annotations.
   *
   * @param annotations the annotations for the result annotated type
   * @param underlyingType the unannotated type for the result annotated type
   * @return an abstract value containing the given {@code annotations} and {@code underlyingType}
   *     or null if {@code annotations} is missing annotations
   */
  public abstract @Nullable V createAbstractValue(
      AnnotationMirrorSet annotations, TypeMirror underlyingType);

  // This cannot be inlined into `createAbstractValue()`, because the Java type system forbids it.
  /**
   * Default implementation for {@link #createAbstractValue(AnnotationMirrorSet, TypeMirror)}.
   *
   * @param analysis the analysis
   * @param annotations the annotations for the result annotated type
   * @param underlyingType the unannotated type for the result annotated type
   * @return an abstract value containing the given {@code annotations} and {@code underlyingType}
   */
  public final @Nullable CFValue getCfValue(
      CFAbstractAnalysis<CFValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType) {
    if (!CFAbstractValue.hasAnnotationFromEveryHierarchy(
        annotations, underlyingType, atypeFactory)) {
      return null;
    }
    return new CFValue(analysis, annotations, underlyingType);
  }

  /**
   * Returns the type hierarchy.
   *
   * @return the type hierarchy
   */
  public TypeHierarchy getTypeHierarchy() {
    return typeHierarchy;
  }

  public GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>>
      getTypeFactory() {
    return atypeFactory;
  }

  @Override
  protected TransferResult<V, S> callTransferFunction(Node node, TransferInput<V, S> input) {
    TransferResult<V, S> result;
    try {
      result = super.callTransferFunction(node, input);
    } catch (Throwable t) {
      throw new BugInCF(node.getTree(), t);
    }
    return result;
  }

  /**
   * Returns an abstract value containing an annotated type with the annotation {@code anno}, and
   * 'top' for all other hierarchies. The underlying type is {@code underlyingType}.
   *
   * @param anno the annotation for the result annotated type
   * @param underlyingType the unannotated type for the result annotated type
   * @return an abstract value with {@code anno} and {@code underlyingType}
   */
  public V createSingleAnnotationValue(AnnotationMirror anno, TypeMirror underlyingType) {
    QualifierHierarchy hierarchy = getTypeFactory().getQualifierHierarchy();
    AnnotationMirrorSet annos = new AnnotationMirrorSet();
    annos.addAll(hierarchy.getTopAnnotations());
    AnnotationMirror f = hierarchy.findAnnotationInSameHierarchy(annos, anno);
    annos.remove(f);
    annos.add(anno);
    return createAbstractValue(annos, underlyingType);
  }

  /**
   * Returns the types utility.
   *
   * @return {@link #types}
   */
  public Types getTypes() {
    return types;
  }

  /**
   * Returns the processing environment.
   *
   * @return {@link #env}
   */
  public ProcessingEnvironment getEnv() {
    return env;
  }
}
