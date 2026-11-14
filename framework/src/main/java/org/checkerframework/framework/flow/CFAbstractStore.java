package org.checkerframework.framework.flow;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.IteratedCollectionElement;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.SuperReference;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory.AnnotationWithMetaAnnotation;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.CollectionsP;
import org.plumelib.util.MapsP;
import org.plumelib.util.ToStringComparator;
import org.plumelib.util.UniqueId;

/**
 * A store for the Checker Framework analysis. It tracks the annotations of memory locations such as
 * local variables and fields.
 *
 * <p>When adding a new field to track values for a code construct (similar to {@code
 * localVariableValues} and {@code thisValue}), it is important to review all constructors and
 * methods in this class for locations where the new field must be handled (such as the copy
 * constructor and {@code clearValue}), as well as all constructors/methods in subclasses of {@code
 * CFAbstractStore}. Note that this includes not only overridden methods in the subclasses, but new
 * methods in the subclasses as well. Also check if
 * BaseTypeVisitor#getJavaExpressionContextFromNode(Node) needs to be updated. Failing to do so
 * might result in silent failures that are time-consuming to debug.
 */
// TODO: Split this class into two parts: one that is reusable generally and
// one that is specific to the Checker Framework.
public abstract class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>>
    implements Store<S>, UniqueId {

  /** The analysis class this store belongs to. */
  protected final CFAbstractAnalysis<V, S, ?> analysis;

  /** Information collected about local variables (including method parameters). */
  protected final Map<LocalVariable, V> localVariableValues;

  /** Information collected about iterated collection elements. */
  protected final Map<IteratedCollectionElement, V> iteratedCollectionElements;

  /** Information collected about the current object. */
  protected @Nullable V thisValue;

  /** Information collected about fields, using the internal representation {@link FieldAccess}. */
  protected Map<FieldAccess, V> fieldValues;

  /**
   * Returns information about fields. Clients should not side-effect the returned value, which is
   * aliased to internal state.
   *
   * @return information about fields
   */
  public Map<FieldAccess, V> getFieldValues() {
    return fieldValues;
  }

  /**
   * Information collected about array elements, using the internal representation {@link
   * ArrayAccess}.
   */
  protected final Map<ArrayAccess, V> arrayValues;

  /**
   * Information collected about the expressions to which method calls evaluate, using the internal
   * representation {@link MethodCall}.
   */
  protected final Map<MethodCall, V> methodCallExpressions;

  /**
   * Information collected about <i>classname</i>.class values, using the internal representation
   * {@link ClassName}.
   */
  protected final Map<ClassName, V> classValues;

  /**
   * True if the analysis should use sequential Java semantics (i.e., assume that only one thread is
   * running at all times).
   */
  protected final boolean sequentialSemantics;

  /** True if -AassumeSideEffectFree or -AassumePure was passed on the command line. */
  private final boolean assumeSideEffectFree;

  /** True if -AassumePureGetters was passed on the command line. */
  private final boolean assumePureGetters;

  /** The unique ID for the next-created object. */
  private static final AtomicLong nextUid = new AtomicLong(0);

  /** The unique ID of this object. */
  private final transient long uid = nextUid.getAndIncrement();

  @Override
  public long getUid() {
    return uid;
  }

  /* --------------------------------------------------------- */
  /* Initialization */
  /* --------------------------------------------------------- */

  /**
   * Creates a new CFAbstractStore.
   *
   * @param analysis the analysis class this store belongs to
   * @param sequentialSemantics true if the analysis should use sequential Java semantics
   */
  protected CFAbstractStore(CFAbstractAnalysis<V, S, ?> analysis, boolean sequentialSemantics) {
    this.analysis = analysis;
    this.localVariableValues = new HashMap<>();
    this.iteratedCollectionElements = new HashMap<>();
    this.thisValue = null;
    this.fieldValues = new HashMap<>();
    this.methodCallExpressions = new HashMap<>();
    this.arrayValues = new HashMap<>();
    this.classValues = new HashMap<>();
    this.sequentialSemantics = sequentialSemantics;
    this.assumeSideEffectFree =
        analysis.checker.hasOption("assumeSideEffectFree")
            || analysis.checker.hasOption("assumePure");
    this.assumePureGetters = analysis.checker.hasOption("assumePureGetters");
  }

  /**
   * Copy constructor.
   *
   * @param other a CFAbstractStore to copy into this
   */
  protected CFAbstractStore(CFAbstractStore<V, S> other) {
    this.analysis = other.analysis;
    this.localVariableValues = new HashMap<>(other.localVariableValues);
    this.iteratedCollectionElements = new HashMap<>(other.iteratedCollectionElements);
    this.thisValue = other.thisValue;
    this.fieldValues = new HashMap<>(other.fieldValues);
    this.methodCallExpressions = new HashMap<>(other.methodCallExpressions);
    this.arrayValues = new HashMap<>(other.arrayValues);
    this.classValues = new HashMap<>(other.classValues);
    this.sequentialSemantics = other.sequentialSemantics;
    this.assumeSideEffectFree = other.assumeSideEffectFree;
    this.assumePureGetters = other.assumePureGetters;
  }

  /**
   * Sets the abstract value of a method parameter (only adds the information to the store, does not
   * remove any other knowledge). Any previous information is erased; this method should only be
   * used to initialize the abstract value.
   */
  public void initializeMethodParameter(LocalVariableNode p, @Nullable V value) {
    if (value != null) {
      localVariableValues.put(new LocalVariable(p.getElement()), value);
    }
  }

  /**
   * Sets the value of the current object. Any previous information is erased; this method should
   * only be used to initialize the value.
   */
  public void initializeThisValue(AnnotationMirror a, TypeMirror underlyingType) {
    if (a != null) {
      thisValue = analysis.createSingleAnnotationValue(a, underlyingType);
    }
  }

  /* --------------------------------------------------------- */
  /* Handling of fields */
  /* --------------------------------------------------------- */

  /**
   * Removes any information that might not be valid any more after a method call, and adds
   * information guaranteed by the method.
   *
   * <ol>
   *   <li>If the method is side-effect-free (as indicated by {@link
   *       org.checkerframework.dataflow.qual.SideEffectFree} or {@link
   *       org.checkerframework.dataflow.qual.Pure}), then no information needs to be removed.
   *   <li>Otherwise, all information about field accesses {@code a.f} needs to be removed, except
   *       if the method cannot modify {@code a.f}. This unmodifiability property holds if {@code a}
   *       is a local variable or {@code this}, and {@code f} is final, or if {@code a.f} has a
   *       {@link MonotonicQualifier} in the current store. Subclasses can change this behavior by
   *       overriding {@link #newFieldValueAfterMethodCall(FieldAccess, GenericAnnotatedTypeFactory,
   *       CFAbstractValue)}.
   *   <li>Furthermore, if the field has a monotonic annotation, then its information can also be
   *       kept.
   * </ol>
   *
   * Furthermore, if the method is deterministic, we store its result {@code val} in the store.
   *
   * @param methodInvocationNode method whose information is being updated
   * @param val abstract value of the method call, or null to store no value for the method call
   */
  public void updateForMethodCall(MethodInvocationNode methodInvocationNode, @Nullable V val) {
    ExecutableElement method = methodInvocationNode.getTarget().getMethod();

    @SuppressWarnings("unchecked")
    GenericAnnotatedTypeFactory<V, S, ?, ?> atypeFactory = analysis.atypeFactory;

    // Case 1: The method is side-effect-free.
    boolean hasSideEffect =
        !(assumeSideEffectFree
            || (assumePureGetters && ElementUtils.isGetter(method))
            || atypeFactory.isSideEffectFree(method));
    if (hasSideEffect) {

      boolean sideEffectsUnrefineAliases = atypeFactory.sideEffectsUnrefineAliases;
      Node receiver = methodInvocationNode.getTarget().getReceiver();

      boolean hasDoesNotUnrefineReceiver = atypeFactory.hasDoesNotUnrefineReceiver(method);
      // This is an expression that is exempted from unrefinement, or null if no expression is
      // exempted.
      @Nullable JavaExpression unrefinableReceiverJe =
          hasDoesNotUnrefineReceiver ? JavaExpression.fromNode(receiver) : null;

      @Nullable List<JavaExpression> seOnlyExpressions =
          analysis.getSideEffectsOnlyExpressions(methodInvocationNode);

      // TODO: Also remove if any element/argument to the annotation is not
      // isUnmodifiableByOtherCode.  Example: @KeyFor("valueThatCanBeMutated").

      // Update local variables and iterated collection elements.
      if (sideEffectsUnrefineAliases) {
        localVariableValues
            .entrySet()
            .removeIf(e -> isSideEffected(e.getKey(), unrefinableReceiverJe, seOnlyExpressions));
        iteratedCollectionElements
            .entrySet()
            .removeIf(e -> isSideEffected(e.getKey(), unrefinableReceiverJe, seOnlyExpressions));
      }

      // Update this value.
      // `thisValue` is the abstract value of the expression `this`.  A callee with a
      // `@SideEffectsOnly` annotation can change it only if the annotation lists `this` itself.
      // (At call site `x.m()`, the callee's `this` corresponds to `x`.)
      boolean thisIsSideEffected =
          seOnlyExpressions == null
              || seOnlyExpressions.stream().anyMatch(je -> je instanceof ThisReference);
      if (sideEffectsUnrefineAliases
          && thisIsSideEffected
          && !(unrefinableReceiverJe instanceof ThisReference)
          && !(unrefinableReceiverJe instanceof SuperReference)) {
        thisValue = null;
      }

      // Update field values.
      if (sideEffectsUnrefineAliases) {
        fieldValues
            .entrySet()
            .removeIf(e -> isSideEffected(e.getKey(), unrefinableReceiverJe, seOnlyExpressions));
      } else {
        // Case 2 (unassignable fields) and case 3 (monotonic fields).
        updateFieldValuesForMethodCall(atypeFactory, unrefinableReceiverJe, seOnlyExpressions);
      }

      // Update array values.
      arrayValues
          .entrySet()
          .removeIf(e -> isSideEffected(e.getKey(), unrefinableReceiverJe, seOnlyExpressions));

      // Update information about method calls.
      methodCallExpressions
          .entrySet()
          .removeIf(e -> isSideEffected(e.getKey(), unrefinableReceiverJe, seOnlyExpressions));
    }

    // Store information about method calls if possible.
    JavaExpression methodCall = JavaExpression.fromNode(methodInvocationNode);
    replaceValue(methodCall, val);
  }

  /**
   * Returns true if a method call might change the abstract value of the given expression, so its
   * refinement should be discarded.
   *
   * <p>When {@code sideEffectsOnlyExpressions} is non-null (the method has a
   * {@code @SideEffectsOnly} annotation), {@code expr} is side-effected only if {@link
   * #mayChangeValue} holds of it and one of those expressions. This is the counterpart of the
   * exact-equality test used at the declaration site in {@code DisallowedSideEffects}, which checks
   * what the method body actually modifies.
   *
   * <p>Some side effects are ignored: {@code notSideEffectedExpression} is treated as if it cannot
   * change. Concretely, the implementation evaluates to false if {@code expr} is strictly equal to
   * {@code notSideEffectedExpression}.
   *
   * <p>An {@link IteratedCollectionElement} is side-effected exactly when its {@link
   * IteratedCollectionElement#reference reference} is, such as the loop variable {@code o} in
   * {@code for (Object o : list)}.
   *
   * @param expr an expression
   * @param notSideEffectedExpression an expression that is never considered to be side-effected, or
   *     null
   * @param sideEffectsOnlyExpressions if non-null, only these expressions (and expressions whose
   *     value they might change) are considered to be side-effected
   * @return true if the abstract value of the expression might have changed
   */
  private boolean isSideEffected(
      JavaExpression expr,
      @Nullable JavaExpression notSideEffectedExpression,
      @Nullable List<JavaExpression> sideEffectsOnlyExpressions) {
    if (expr instanceof IteratedCollectionElement ice) {
      return isSideEffected(ice.reference, notSideEffectedExpression, sideEffectsOnlyExpressions);
    }
    if (!expr.isModifiableByOtherCode()) {
      return false;
    }
    if (notSideEffectedExpression != null && expr.equals(notSideEffectedExpression)) {
      return false;
    }
    if (sideEffectsOnlyExpressions != null) {
      return sideEffectsOnlyExpressions.stream().anyMatch(seOnly -> mayChangeValue(expr, seOnly));
    }
    return true;
  }

  /**
   * Returns true if modifying {@code seOnlyExpr}, or anything reached through it, might change the
   * value of {@code expr}.
   *
   * <p>That is the case when {@code expr} contains {@code seOnlyExpr} as a subexpression: modifying
   * {@code x} can change {@code x.f}.
   *
   * <p>It is also the case when {@code expr} contains a method call through which {@code
   * seOnlyExpr} is reached; see {@link #callMayChangeValue}.
   *
   * <p>This method is unsound for two array accesses that might denote the same element without
   * containing one another. It returns false for {@code a[i]} and {@code a[0]}, so the refinement
   * of {@code a[i]} survives a call to a {@code @SideEffectsOnly("a[0]")} method even when {@code
   * i} is 0. TODO: return true when both expressions are array accesses on arrays that might be the
   * same, unless the two index expressions are known to differ.
   *
   * @param expr an expression whose value is stored in this store
   * @param seOnlyExpr an expression that might be modified
   * @return true if modifying {@code seOnlyExpr} might change the value of {@code expr}
   */
  private static boolean mayChangeValue(JavaExpression expr, JavaExpression seOnlyExpr) {
    return expr.containsSyntacticEqualJavaExpression(seOnlyExpr)
        || callMayChangeValue(expr, seOnlyExpr);
  }

  /**
   * Returns true if {@code expr} contains a method call such that modifying {@code seOnlyExpr}
   * might change the method call's value.
   *
   * <p>The value of a call to a {@code @Pure} method depends on the state that the method reads,
   * which no annotation declares; this method approximates the read state by what is reachable from
   * the call's receiver and arguments. If {@code getF()} returns {@code this.f}, then a call to a
   * {@code @SideEffectsOnly("x.f")} method can change the value of {@code x.getF()}, even though
   * {@code x.getF()} does not contain {@code x.f}.
   *
   * <p>The approximation misses state that a call reads but does not reach through its receiver or
   * arguments. Static state is the main such case: the receiver of a static call is a class name,
   * which reaches nothing, so only the arguments contribute. If {@code Util.getField()} returns
   * {@code Other.field}, this method returns false for {@code Util.getField()} and {@code
   * Other.field}, even though a call to a {@code @SideEffectsOnly("Other.field")} method can change
   * the call's value. (For a static call with no modifiable argument, the omission is masked by
   * {@link #isSideEffected}, which returns false before reaching this method because such a call is
   * not {@link JavaExpression#isModifiableByOtherCode}; its refinement survives every call, not
   * just a {@code @SideEffectsOnly} one.)
   *
   * <p>The call need not be {@code expr} itself: the value of {@code x.getF().g} and of {@code
   * x.getArr()[0]} also changes when the value of the call within them does. Such an expression
   * contains no method call as a <em>subexpression</em> in the sense of {@link
   * JavaExpression#containsSyntacticEqualJavaExpression}, because a receiver or array is not
   * compared against {@code seOnlyExpr} but is descended into here.
   *
   * @param expr an expression whose value is stored in this store
   * @param seOnlyExpr an expression that might be modified
   * @return true if modifying {@code seOnlyExpr} might change the value of expr, by changing the
   *     value of a call within {@code expr}
   */
  private static boolean callMayChangeValue(JavaExpression expr, JavaExpression seOnlyExpr) {
    if (expr instanceof MethodCall methodCall) {
      if (mayReach(methodCall.getReceiver(), seOnlyExpr)) {
        return true;
      }
      for (JavaExpression argument : methodCall.getArguments()) {
        if (mayReach(argument, seOnlyExpr)) {
          return true;
        }
      }
      return false;
    } else if (expr instanceof FieldAccess fieldAccess) {
      return callMayChangeValue(fieldAccess.getReceiver(), seOnlyExpr);
    } else if (expr instanceof ArrayAccess arrayAccess) {
      return callMayChangeValue(arrayAccess.getArray(), seOnlyExpr)
          || callMayChangeValue(arrayAccess.getIndex(), seOnlyExpr);
    } else {
      return false;
    }
  }

  /**
   * Returns true if {@code seOnlyExpr} might be reached through {@code input}.
   *
   * @param input the receiver or an argument of a stored method call
   * @param seOnlyExpr an expression that might be modified
   * @return true if {@code seOnlyExpr} might be reached through {@code input}
   */
  private static boolean mayReach(JavaExpression input, JavaExpression seOnlyExpr) {
    // The recursive call handles a nested call such as `x.getA().getB()`, whose receiver is
    // itself a method call.
    return seOnlyExpr.containsSyntacticEqualJavaExpression(input)
        || mayChangeValue(input, seOnlyExpr);
  }

  /**
   * Returns the new value of a field after a method call, or {@code null} if the field should be
   * removed from the store.
   *
   * <p>In this default implementation, the field's value is preserved if it is either not
   * assignable (see {@link FieldAccess#isAssignableByOtherCode()}) or has a monotonic qualifier
   * (see {@link #newMonotonicFieldValueAfterMethodCall(FieldAccess, GenericAnnotatedTypeFactory,
   * CFAbstractValue)}). Otherwise, it is removed from the store.
   *
   * @param fieldAccess the field whose value to update
   * @param atypeFactory AnnotatedTypeFactory of the associated checker
   * @param value the field's value before the method call
   * @return the field's value after the method call, or {@code null} if the field should be removed
   *     from the store
   */
  protected V newFieldValueAfterMethodCall(
      FieldAccess fieldAccess, GenericAnnotatedTypeFactory<V, S, ?, ?> atypeFactory, V value) {
    // Handle unassignable fields.
    if (!fieldAccess.isAssignableByOtherCode()) {
      return value;
    }

    // Handle fields with monotonic annotations.
    return newMonotonicFieldValueAfterMethodCall(fieldAccess, atypeFactory, value);
  }

  /**
   * Computes the value of a field whose declaration has a monotonic annotation, or returns {@code
   * null} if the field has no monotonic annotation.
   *
   * <p>Used by {@link #newFieldValueAfterMethodCall(FieldAccess, GenericAnnotatedTypeFactory,
   * CFAbstractValue)} to handle fields with monotonic annotations.
   *
   * @param fieldAccess the field whose value to compute
   * @param atypeFactory AnnotatedTypeFactory of the associated checker
   * @param value the field's value before the method call
   * @return the field's value after the method call, or {@code null} if the field has no monotonic
   *     annotation
   */
  protected V newMonotonicFieldValueAfterMethodCall(
      FieldAccess fieldAccess, GenericAnnotatedTypeFactory<V, S, ?, ?> atypeFactory, V value) {

    // case 3: the field has a monotonic annotation
    if (atypeFactory.getSupportedMonotonicTypeQualifiers().isEmpty()) {
      return null;
    }

    List<AnnotationWithMetaAnnotation> fieldAnnotationPairs =
        atypeFactory.getAnnotationWithMetaAnnotation(
            fieldAccess.getField(), MonotonicQualifier.class);
    List<AnnotationMirror> metaAnnotations =
        CollectionsP.withoutDuplicates(
            CollectionsP.mapList(
                AnnotationWithMetaAnnotation::metaAnnotation, fieldAnnotationPairs));
    List<AnnotationMirror> monotonicAnnotations = new ArrayList<>(metaAnnotations.size());
    for (AnnotationMirror metaAnnotation : metaAnnotations) {
      @SuppressWarnings("deprecation") // permitted for use in the framework
      Name annoName = AnnotationUtils.getElementValueClassName(metaAnnotation, "value", false);
      monotonicAnnotations.add(
          AnnotationBuilder.fromName(atypeFactory.getElementUtils(), annoName));
    }
    Collection<AnnotationMirror> valueAnnos = value.getAnnotations();
    V newValue = null;
    for (AnnotationMirror monotonicAnnotation : monotonicAnnotations) {
      // Make sure the target annotation is present.
      if (AnnotationUtils.containsSame(valueAnnos, monotonicAnnotation)) {
        newValue =
            analysis
                .createSingleAnnotationValue(monotonicAnnotation, value.getUnderlyingType())
                .mostSpecific(newValue, null);
      }
    }
    return newValue;
  }

  /**
   * Helper for {@link #updateForMethodCall(MethodInvocationNode, CFAbstractValue)}. Removes any
   * information about field values that might not be valid any more after a method call, and adds
   * information guaranteed by the method.
   *
   * <p>More specifically, removes all information about fields except for unassignable fields and
   * fields that have a monotonic annotation.
   *
   * <p>A non-null {@code sideEffectsOnlyExpressions} indicates that the invoked method has limited
   * side effects. In this case, removes information for fields that appear in the list of
   * side-effected expressions.
   *
   * @param atypeFactory AnnotatedTypeFactory of the associated checker
   * @param unrefinableReceiverJe if non-null, the receiver, which should not be unrefined
   * @param sideEffectsOnlyExpressions the expressions that are side-effected by a method call
   */
  private void updateFieldValuesForMethodCall(
      GenericAnnotatedTypeFactory<V, S, ?, ?> atypeFactory,
      @Nullable JavaExpression unrefinableReceiverJe,
      @Nullable List<JavaExpression> sideEffectsOnlyExpressions) {
    Map<FieldAccess, V> newFieldValues = new HashMap<>(MapsP.mapCapacity(fieldValues));
    for (Map.Entry<FieldAccess, V> e : fieldValues.entrySet()) {
      FieldAccess fieldAccess = e.getKey();
      V previousValue = e.getValue();

      if (!isSideEffected(fieldAccess, unrefinableReceiverJe, sideEffectsOnlyExpressions)) {
        // If the field hasn't been side-effected, there is no need to compute a new value for it.
        // For unmodifiable fields, this is safe because they are not assignable by other code.
        // For the exempt receiver, skipping recomputation is necessary to preserve its value.
        newFieldValues.put(fieldAccess, previousValue);
      } else {
        V newValue = newFieldValueAfterMethodCall(fieldAccess, atypeFactory, previousValue);
        if (newValue != null) {
          // Keep information for all hierarchies where we had a monotonic annotation.
          newFieldValues.put(fieldAccess, newValue);
        }
      }
    }
    fieldValues = newFieldValues;
  }

  /**
   * Adds the annotation {@code a} for the expression {@code expr} (correctly deciding where to
   * store the information depending on the type of the expression {@code expr}).
   *
   * <p>This method does not take care of removing other information that might be influenced by
   * changes to certain parts of the state.
   *
   * <p>If there is already a value {@code v} present for {@code expr}, then the stronger of the new
   * and old value is taken (according to the lattice). Note that this happens per hierarchy, and if
   * the store already contains information about a hierarchy other than {@code a}'s hierarchy, that
   * information is preserved.
   *
   * <p>If {@code expr} is nondeterministic, this method does not insert {@code a} into the store.
   *
   * @param expr an expression
   * @param a an annotation for the expression
   */
  public void insertValue(JavaExpression expr, AnnotationMirror a) {
    insertValue(expr, analysis.createSingleAnnotationValue(a, expr.getType()));
  }

  /**
   * Like {@link #insertValue(JavaExpression, AnnotationMirror)}, but permits nondeterministic
   * expressions to be stored.
   *
   * <p>For an explanation of when to permit nondeterministic expressions, see {@link
   * #insertValuePermitNondeterministic(JavaExpression, CFAbstractValue)}.
   *
   * @param expr an expression
   * @param a an annotation for the expression
   */
  public void insertValuePermitNondeterministic(JavaExpression expr, AnnotationMirror a) {
    insertValuePermitNondeterministic(
        expr, analysis.createSingleAnnotationValue(a, expr.getType()));
  }

  /**
   * Adds the annotation {@code newAnno} for the expression {@code expr} (correctly deciding where
   * to store the information depending on the type of the expression {@code expr}).
   *
   * <p>This method does not take care of removing other information that might be influenced by
   * changes to certain parts of the state.
   *
   * <p>If there is already a value {@code v} present for {@code expr}, then the greatest lower
   * bound of the new and old value is inserted into the store.
   *
   * <p>Note that this happens per hierarchy, and if the store already contains information about a
   * hierarchy other than {@code newAnno}'s hierarchy, that information is preserved.
   *
   * <p>If {@code expr} is nondeterministic, this method does not insert {@code newAnno} into the
   * store.
   *
   * @param expr an expression
   * @param newAnno the expression's annotation
   */
  public final void insertOrRefine(JavaExpression expr, AnnotationMirror newAnno) {
    insertOrRefine(expr, newAnno, false);
  }

  /**
   * Like {@link #insertOrRefine(JavaExpression, AnnotationMirror)}, but permits nondeterministic
   * expressions to be inserted.
   *
   * <p>For an explanation of when to permit nondeterministic expressions, see {@link
   * #insertValuePermitNondeterministic(JavaExpression, CFAbstractValue)}.
   *
   * @param expr an expression
   * @param newAnno the expression's annotation
   */
  public final void insertOrRefinePermitNondeterministic(
      JavaExpression expr, AnnotationMirror newAnno) {
    insertOrRefine(expr, newAnno, true);
  }

  /**
   * Helper function for {@link #insertOrRefine(JavaExpression, AnnotationMirror)} and {@link
   * #insertOrRefinePermitNondeterministic}.
   *
   * @param expr an expression
   * @param newAnno the expression's annotation
   * @param permitNondeterministic true if nondeterministic expressions can be inserted into the
   *     store
   */
  protected void insertOrRefine(
      JavaExpression expr, AnnotationMirror newAnno, boolean permitNondeterministic) {
    if (!canInsertJavaExpression(expr)) {
      return;
    }
    if (!(permitNondeterministic || expr.isDeterministic(analysis.getTypeFactory()))) {
      return;
    }

    V newValue = analysis.createSingleAnnotationValue(newAnno, expr.getType());
    V oldValue = getValue(expr);
    if (oldValue == null) {
      insertValue(
          expr,
          analysis.createSingleAnnotationValue(newAnno, expr.getType()),
          permitNondeterministic);
      return;
    }
    computeNewValueAndInsert(
        expr,
        newValue,
        (previousValue, value) -> {
          assert previousValue != null
              : "@AssumeAssertion(nullness): oldValue, checked above, is non-null";
          return previousValue.greatestLowerBound(value);
        },
        permitNondeterministic);
  }

  /** Returns true if {@code expr} can be stored in this store. */
  public static boolean canInsertJavaExpression(JavaExpression expr) {
    if (expr instanceof FieldAccess
        || expr instanceof ThisReference
        || expr instanceof SuperReference
        || expr instanceof LocalVariable
        || expr instanceof IteratedCollectionElement
        || expr instanceof MethodCall
        || expr instanceof ArrayAccess
        || expr instanceof ClassName) {
      return !expr.containsUnknown();
    }
    return false;
  }

  /**
   * Adds the abstract value {@code value} for the expression {@code expr} (correctly deciding where
   * to store the information depending on the type of the expression {@code expr}).
   *
   * <p>This method does not take care of removing other information that might be influenced by
   * changes to certain parts of the state.
   *
   * <p>If there is already a value {@code v} present for {@code expr}, then the stronger of the new
   * and old value is taken (according to the lattice). Note that this happens per hierarchy, and if
   * the store already contains information about a hierarchy for which {@code value} does not
   * contain information, then that information is preserved.
   *
   * <p>If {@code expr} is nondeterministic, this method does not insert {@code value} into the
   * store.
   *
   * @param expr the expression to insert in the store
   * @param value the value of the expression
   */
  public final void insertValue(JavaExpression expr, @Nullable V value) {
    insertValue(expr, value, false);
  }

  /**
   * Like {@link #insertValue(JavaExpression, CFAbstractValue)}, but updates the store even if
   * {@code expr} is nondeterministic.
   *
   * <p>Usually, nondeterministic JavaExpressions should not be stored in a Store. For example, in
   * the body of {@code if (nondet() == 3) {...}}, the store should not record that the value of
   * {@code nondet()} is 3, because it might not be 3 the next time {@code nondet()} is executed.
   *
   * <p>However, contracts can mention a nondeterministic JavaExpression. For example, a contract
   * might have a postcondition that {@code nondet()} is odd. This means that the next call to
   * {@code nondet()} will return odd. Such a postcondition can be evicted from the store by calling
   * a side-effecting method.
   *
   * @param expr the expression to insert in the store
   * @param value the value of the expression
   */
  public final void insertValuePermitNondeterministic(JavaExpression expr, @Nullable V value) {
    insertValue(expr, value, true);
  }

  /**
   * Returns true if the given (expression, value) pair can be inserted in the store, namely if the
   * value is non-null and the expression does not contain an unknown or a nondeterministic
   * expression.
   *
   * <p>This method returning true does not guarantee that the value will be inserted; the
   * implementation of {@link #insertValue(JavaExpression, CFAbstractValue, boolean)} might still
   * not insert it.
   *
   * @param expr the expression to insert in the store
   * @param value the value of the expression
   * @param permitNondeterministic if false, returns false if {@code expr} is nondeterministic; if
   *     true, permits nondeterministic expressions to be placed in the store
   * @return true if the given (expression, value) pair can be inserted in the store
   */
  @EnsuresNonNullIf(expression = "#2", result = true)
  protected boolean shouldInsert(
      JavaExpression expr, @Nullable V value, boolean permitNondeterministic) {
    if (value == null) {
      // No need to insert a null abstract value because it represents
      // top and top is also the default value.
      return false;
    }
    if (expr.containsUnknown()) {
      // Expressions containing unknown expressions are not stored.
      return false;
    }
    if (!(permitNondeterministic || expr.isDeterministic(analysis.getTypeFactory()))) {
      // Nondeterministic expressions may not be stored.
      // (They are likely to be quickly evicted, as soon as a side-effecting method is
      // called.)
      return false;
    }
    return true;
  }

  /**
   * Helper method for {@link #insertValue(JavaExpression, CFAbstractValue)} and {@link
   * #insertValuePermitNondeterministic}.
   *
   * <p>Every overriding implementation should start with
   *
   * <pre>{@code
   * if (!shouldInsert) {
   *   return;
   * }
   * }</pre>
   *
   * @param expr the expression to insert in the store
   * @param value the value of the expression
   * @param permitNondeterministic if false, does nothing if {@code expr} is nondeterministic; if
   *     true, permits nondeterministic expressions to be placed in the store
   */
  protected void insertValue(
      JavaExpression expr, @Nullable V value, boolean permitNondeterministic) {
    computeNewValueAndInsert(
        expr,
        value,
        (old, newValue) -> {
          assert newValue != null
              : "@AssumeAssertion(nullness): shouldInsert ensures a non-null value";
          return newValue.mostSpecific(old, null);
        },
        permitNondeterministic);
  }

  /**
   * Inserts the result of applying {@code merger} to {@code value} and the previous value for
   * {@code expr}.
   *
   * @param expr the JavaExpression
   * @param value the value of the JavaExpression
   * @param merger the function used to merge {@code value} and the previous value of {@code expr}
   * @param permitNondeterministic if false, does nothing if {@code expr} is nondeterministic; if
   *     true, permits nondeterministic expressions to be placed in the store
   */
  protected void computeNewValueAndInsert(
      JavaExpression expr,
      @Nullable V value,
      BinaryOperator<@Nullable V> merger,
      boolean permitNondeterministic) {
    if (!shouldInsert(expr, value, permitNondeterministic)) {
      return;
    }

    if (expr instanceof LocalVariable localVar) {
      mergeIntoMap(localVariableValues, localVar, value, merger);
    } else if (expr instanceof IteratedCollectionElement collectionElt) {
      mergeIntoMap(iteratedCollectionElements, collectionElt, value, merger);
    } else if (expr instanceof FieldAccess fieldAcc) {
      // Only store information about final fields (where the receiver is
      // also fixed) if concurrent semantics are enabled.
      boolean isMonotonic = isMonotonicUpdate(fieldAcc, value);
      if (sequentialSemantics || isMonotonic || !fieldAcc.isAssignableByOtherCode()) {
        mergeIntoMap(fieldValues, fieldAcc, value, merger);
      }
    } else if (expr instanceof MethodCall method) {
      // Don't store any information if concurrent semantics are enabled.
      if (sequentialSemantics) {
        mergeIntoMap(methodCallExpressions, method, value, merger);
      }
    } else if (expr instanceof ArrayAccess arrayAccess) {
      if (sequentialSemantics) {
        mergeIntoMap(arrayValues, arrayAccess, value, merger);
      }
    } else if (expr instanceof ThisReference || expr instanceof SuperReference) {
      if (sequentialSemantics || !expr.isAssignableByOtherCode()) {
        @Nullable V oldValue = thisValue;
        @Nullable V newValue = merger.apply(oldValue, value);
        if (newValue != null) {
          thisValue = newValue;
        }
      }
    } else if (expr instanceof ClassName className) {
      if (sequentialSemantics || !className.isAssignableByOtherCode()) {
        mergeIntoMap(classValues, className, value, merger);
      }
    } else {
      // No other types of expressions need to be stored.
    }
  }

  /**
   * Puts into {@code map} the result of applying {@code merger} to the value that {@code map} gives
   * {@code key} (or null) and {@code value}. Does nothing if the result is null.
   *
   * @param <K> the type of the keys, such as {@link LocalVariable}
   * @param map a map in this store, such as {@link #localVariableValues}, which this method
   *     side-effects
   * @param key the key whose value to update
   * @param value the value to merge with the value that {@code map} gives {@code key}
   * @param merger the function that merges the value that {@code map} gives {@code key} and {@code
   *     value}
   */
  private <K extends Object> void mergeIntoMap(
      Map<K, V> map, K key, @Nullable V value, BinaryOperator<@Nullable V> merger) {
    @Nullable V newValue = merger.apply(map.get(key), value);
    if (newValue != null) {
      map.put(key, newValue);
    }
  }

  /**
   * Returns true if fieldAcc is an update of a monotonic qualifier to its target qualifier (e.g.,
   * {@code @MonotonicNonNull} to {@code @NonNull}). Always returns false if {@code
   * sequentialSemantics} is true.
   *
   * @return true if fieldAcc is an update of a monotonic qualifier to its target qualifier
   *     (e.g. @MonotonicNonNull to @NonNull)
   */
  protected boolean isMonotonicUpdate(FieldAccess fieldAcc, V value) {
    if (analysis.atypeFactory.getSupportedMonotonicTypeQualifiers().isEmpty()) {
      return false;
    }
    if (sequentialSemantics) { // only compute if necessary
      return false;
    }
    AnnotatedTypeFactory atypeFactory = this.analysis.atypeFactory;
    List<AnnotationWithMetaAnnotation> fieldAnnotations =
        atypeFactory.getAnnotationWithMetaAnnotation(fieldAcc.getField(), MonotonicQualifier.class);
    for (AnnotationWithMetaAnnotation fieldAnnotation : fieldAnnotations) {
      AnnotationMirror metaAnnotation = fieldAnnotation.metaAnnotation();
      @SuppressWarnings("deprecation") // permitted for use in the framework
      Name annoName = AnnotationUtils.getElementValueClassName(metaAnnotation, "value", false);
      AnnotationMirror monotonicAnnotation =
          AnnotationBuilder.fromName(atypeFactory.getElementUtils(), annoName);
      // Make sure the 'target' annotation is present.
      if (AnnotationUtils.containsSame(value.getAnnotations(), monotonicAnnotation)) {
        return true;
      }
    }
    return false;
  }

  public void insertThisValue(AnnotationMirror a, TypeMirror underlyingType) {
    if (a == null) {
      return;
    }

    V value = analysis.createSingleAnnotationValue(a, underlyingType);

    V oldValue = thisValue;
    V newValue = value.mostSpecific(oldValue, null);
    if (newValue != null) {
      thisValue = newValue;
    }
  }

  /**
   * Completely replaces the abstract value for the expression {@code expr} (correctly deciding
   * where to store the information depending on the type of the expression {@code expr}). Any
   * previous information is discarded.
   *
   * <p>This method does not take care of removing other information that might be influenced by
   * changes to certain parts of the state.
   *
   * @param expr the expression whose value to replace
   * @param a the new annotation
   */
  public void replaceValue(JavaExpression expr, AnnotationMirror a) {
    replaceValue(expr, analysis.createSingleAnnotationValue(a, expr.getType()));
  }

  /**
   * Completely replaces the abstract value for the expression {@code expr} (correctly deciding
   * where to store the information depending on the type of the expression {@code expr}). Any
   * previous information is discarded.
   *
   * <p>This method does not take care of removing other information that might be influenced by
   * changes to certain parts of the state.
   *
   * @param expr the expression whose value to replace
   * @param value the new value
   */
  public void replaceValue(JavaExpression expr, @Nullable V value) {
    clearValue(expr);
    insertValue(expr, value);
  }

  /**
   * Removes any knowledge about the expression {@code expr} (correctly deciding where to remove the
   * information depending on the type of the expression {@code expr}).
   */
  public void clearValue(JavaExpression expr) {
    if (expr.containsUnknown()) {
      // Expressions containing unknown expressions are not stored.
      return;
    }
    if (expr instanceof LocalVariable localVar) {
      localVariableValues.remove(localVar);
    } else if (expr instanceof IteratedCollectionElement collectionElt) {
      iteratedCollectionElements.remove(collectionElt);
    } else if (expr instanceof FieldAccess fieldAcc) {
      fieldValues.remove(fieldAcc);
    } else if (expr instanceof MethodCall method) {
      methodCallExpressions.remove(method);
    } else if (expr instanceof ArrayAccess a) {
      arrayValues.remove(a);
    } else if (expr instanceof ClassName c) {
      classValues.remove(c);
    } else if (expr instanceof ThisReference) {
      thisValue = null;
    } else {
      // No other types of expressions are stored.
    }
  }

  /**
   * Returns the {@link IteratedCollectionElement} in this store that the given node or tree
   * denotes, or null if this store has no such element.
   *
   * <p>An element is denoted by its own tree (compared by reference) and by every node that {@link
   * IteratedCollectionElement#isDenotedBy denotes} it, such as each use of {@code o} in the body of
   * {@code for (Object o : list)}. An element whose tree is {@code tree} takes precedence, because
   * more than one element might be denoted by {@code node}: for example, two loops that each call
   * {@code it.next()} on the same iterator {@code it} have equal references.
   *
   * @param node a CFG node, such as the node for {@code o} in {@code o.close()}
   * @param tree a tree that might be an element's tree, or null
   * @return the {@link IteratedCollectionElement} that {@code node} or {@code tree} denotes, or
   *     null if this store has no such element
   */
  public @Nullable IteratedCollectionElement getIteratedCollectionElement(
      Node node, @Nullable Tree tree) {
    // Check for an exact tree match first, because `isDenotedBy` can match more than one element.
    for (IteratedCollectionElement ice : iteratedCollectionElements.keySet()) {
      // `Tree` does not override `equals`, so this compares trees by reference.
      if (ice.tree.equals(tree)) {
        return ice;
      }
    }
    for (IteratedCollectionElement ice : iteratedCollectionElements.keySet()) {
      if (ice.isDenotedBy(node)) {
        return ice;
      }
    }
    return null;
  }

  /**
   * Returns the current abstract value of a Java expression, or {@code null} if no information is
   * available.
   *
   * @return the current abstract value of a Java expression, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(JavaExpression expr) {
    if (expr instanceof LocalVariable localVar) {
      return localVariableValues.get(localVar);
    } else if (expr instanceof IteratedCollectionElement collectionElt) {
      return iteratedCollectionElements.get(collectionElt);
    } else if (expr instanceof ThisReference || expr instanceof SuperReference) {
      return thisValue;
    } else if (expr instanceof FieldAccess fieldAcc) {
      return fieldValues.get(fieldAcc);
    } else if (expr instanceof MethodCall method) {
      return methodCallExpressions.get(method);
    } else if (expr instanceof ArrayAccess a) {
      return arrayValues.get(a);
    } else if (expr instanceof ClassName c) {
      return classValues.get(c);
    } else {
      throw new BugInCF("Unexpected JavaExpression: " + expr + " (" + expr.getClass() + ")");
    }
  }

  /**
   * Returns the current abstract value of a field access, or {@code null} if no information is
   * available.
   *
   * @param n the node whose abstract value to return
   * @return the current abstract value of a field access, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(FieldAccessNode n) {
    JavaExpression je = JavaExpression.fromNodeFieldAccess(n);
    if (je instanceof FieldAccess fa) {
      return fieldValues.get(fa);
    } else if (je instanceof ClassName cn) {
      return classValues.get(cn);
    } else if (je instanceof ThisReference || je instanceof SuperReference) {
      // "return thisValue" is wrong, because the node refers to an outer this.
      // So, return null for now.  TODO: improve.
      return null;
    } else {
      throw new BugInCF(
          "Unexpected JavaExpression %s %s for FieldAccessNode %s",
          je.getClass().getSimpleName(), je, n);
    }
  }

  /**
   * Returns the current abstract value of a field access, or {@code null} if no information is
   * available.
   *
   * @param fieldAccess the field access to look up in this store
   * @return current abstract value of a field access, or {@code null} if no information is
   *     available
   */
  public @Nullable V getFieldValue(FieldAccess fieldAccess) {
    return fieldValues.get(fieldAccess);
  }

  /**
   * Returns the current abstract value of a method call, or {@code null} if no information is
   * available.
   *
   * @param n a method call
   * @return the current abstract value of a method call, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(MethodInvocationNode n) {
    JavaExpression method = JavaExpression.fromNode(n);
    if (method == null) {
      return null;
    }
    return methodCallExpressions.get(method);
  }

  /**
   * Returns the current abstract value of an array access, or {@code null} if no information is
   * available.
   *
   * @param n the node whose abstract value to return
   * @return the current abstract value of an array access, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(ArrayAccessNode n) {
    ArrayAccess arrayAccess = JavaExpression.fromArrayAccess(n);
    return arrayValues.get(arrayAccess);
  }

  /**
   * Updates the information in the store by considering an assignment with target {@code n}.
   *
   * @param n the left-hand side of an assignment
   * @param val the right-hand value of an assignment
   */
  public void updateForAssignment(Node n, @Nullable V val) {
    JavaExpression je = JavaExpression.fromNode(n);
    if (je instanceof ArrayAccess aa) {
      updateForArrayAssignment(aa, val);
    } else if (je instanceof FieldAccess fa) {
      updateForFieldAccessAssignment(fa, val);
    } else if (je instanceof LocalVariable lv) {
      updateForLocalVariableAssignment(lv, val);
    } else {
      throw new BugInCF("Unexpected je of class " + je.getClass());
    }
  }

  /**
   * Updates the information in the store by considering a field assignment with target {@code
   * fieldAccess}, where the right-hand side has the abstract value {@code val}.
   *
   * @param fieldAccess the target of the assignment
   * @param val the abstract value of the value assigned to {@code fieldAccess} (or {@code null} if
   *     the abstract value is not known)
   */
  protected void updateForFieldAccessAssignment(FieldAccess fieldAccess, @Nullable V val) {
    removeConflicting(fieldAccess, val);
    if (!fieldAccess.containsUnknown() && val != null) {
      // Only store information about final fields (where the receiver is
      // also fixed) if concurrent semantics are enabled.
      if (sequentialSemantics
          || isMonotonicUpdate(fieldAccess, val)
          || !fieldAccess.isAssignableByOtherCode()) {
        fieldValues.put(fieldAccess, val);
      }
    }
  }

  /**
   * Updates the information in the store by considering an assignment with target {@code
   * arrayAccess}, where the right-hand side has the abstract value {@code val}.
   *
   * <p>See {@link #removeConflicting(ArrayAccess,CFAbstractValue)}, as it is called first by this
   * method.
   *
   * @param arrayAccess the target of the assignment
   * @param val the abstract value of the value assigned to {@code arrayAccess} (or {@code null} if
   *     the abstract value is not known)
   */
  protected void updateForArrayAssignment(ArrayAccess arrayAccess, @Nullable V val) {
    removeConflicting(arrayAccess, val);
    if (!arrayAccess.containsUnknown() && val != null) {
      // Only store information about final fields (where the receiver is
      // also fixed) if concurrent semantics are enabled.
      if (sequentialSemantics) {
        arrayValues.put(arrayAccess, val);
      }
    }
  }

  /**
   * Sets the abstract value of a local variable in the store. Overwrites any value that might have
   * been available previously.
   *
   * @param receiver the local variable that is assigned
   * @param val the abstract value of the value assigned to {@code receiver} (or {@code null} if the
   *     abstract value is not known)
   */
  protected void updateForLocalVariableAssignment(LocalVariable receiver, @Nullable V val) {
    removeConflicting(receiver);
    if (val != null) {
      localVariableValues.put(receiver, val);
    }
  }

  /**
   * Removes any information in this store that might not be true any more after {@code fieldAccess}
   * has been assigned a new value (with the abstract value {@code val}). This includes the
   * following steps (assume that {@code fieldAccess} is of the form <em>a.f</em> for some
   * <em>a</em>).
   *
   * <ol>
   *   <li value="1">Update the abstract value of other field accesses <em>b.g</em> where the field
   *                 is equal (that is, <em>f=g</em>), and the receiver <em>b</em> might alias the
   *                 receiver of {@code fieldAccess}, <em>a</em>. This update will raise the
   *                 abstract value for such field accesses to at least {@code val} (or the old
   *                 value, if that was less precise). However, this is only necessary if the field
   *                 <em>g</em> is not final.
   *   <li value="2">Remove any abstract values for field accesses <em>b.g</em> where the receiver
   *                 <em>b</em> reads a location that {@code fieldAccess} might denote.
   *   <li value="3">Remove any information about method calls, including about iterated collection
   *                 elements whose reference is a method call.
   *   <li value="4">Remove any abstract values for an array access <em>b[i]</em> where the array
   *                 <em>b</em> or the index <em>i</em> reads a location that {@code fieldAccess}
   *                 might denote.
   * </ol>
   *
   * @param fieldAccess the field access that is assigned
   * @param val the abstract value of the value assigned to {@code fieldAccess} (or {@code null} if
   *     the abstract value is not known)
   */
  protected void removeConflicting(FieldAccess fieldAccess, @Nullable V val) {
    Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator = fieldValues.entrySet().iterator();
    while (fieldValuesIterator.hasNext()) {
      Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
      FieldAccess otherFieldAccess = entry.getKey();
      V otherVal = entry.getValue();
      // case 2:
      if (otherFieldAccess.getReceiver().containsModifiableAliasOf(this, fieldAccess)) {
        fieldValuesIterator.remove(); // remove information completely
      }
      // case 1:
      else if (fieldAccess.getField().equals(otherFieldAccess.getField())) {
        if (canAlias(fieldAccess.getReceiver(), otherFieldAccess.getReceiver())) {
          if (!otherFieldAccess.isFinal()) {
            if (val != null) {
              V newVal = val.leastUpperBound(otherVal);
              entry.setValue(newVal);
            } else {
              // remove information completely
              fieldValuesIterator.remove();
            }
          }
        }
      }
    }

    Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator = arrayValues.entrySet().iterator();
    while (arrayValuesIterator.hasNext()) {
      Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
      ArrayAccess otherArrayAccess = entry.getKey();
      if (otherArrayAccess.containsModifiableAliasOf(this, fieldAccess)) {
        // remove information completely
        arrayValuesIterator.remove();
      }
    }

    // case 3:
    methodCallExpressions.clear();
    removeIteratedCollectionElementsWithMethodCallReference();
  }

  /**
   * Removes any information about iterated collection elements whose {@link
   * IteratedCollectionElement#reference reference} is a method call, such as {@code list.get(i)}.
   * This is called wherever all information about method calls is removed.
   */
  private void removeIteratedCollectionElementsWithMethodCallReference() {
    iteratedCollectionElements.keySet().removeIf(ice -> ice.reference instanceof MethodCall);
  }

  /**
   * Removes any information in the store that might not be true any more after {@code arrayAccess}
   * has been assigned a new value (with the abstract value {@code val}). This includes the
   * following steps (assume that {@code arrayAccess} is of the form <em>a[i]</em> for some
   * <em>a</em>).
   *
   * <ol>
   *   <li value="1">Remove any abstract value for other array access <em>b[j]</em> where <em>a</em>
   *                 and <em>b</em> can be aliases, or where either <em>b</em> or <em>j</em> reads a
   *                 location that <em>a[i]</em> might denote.
   *   <li value="2">Remove any abstract values for field accesses <em>b.g</em> where the receiver
   *                 <em>b</em> reads a location that <em>a[i]</em> might denote.
   *   <li value="3">Remove any information about method calls, including about iterated collection
   *                 elements whose reference is a method call.
   * </ol>
   *
   * @param arrayAccess the array access that is assigned
   * @param val the abstract value of the value assigned to {@code arrayAccess} (or {@code null} if
   *     the abstract value is not known)
   */
  protected void removeConflicting(ArrayAccess arrayAccess, @Nullable V val) {
    Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator = arrayValues.entrySet().iterator();
    while (arrayValuesIterator.hasNext()) {
      Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
      ArrayAccess otherArrayAccess = entry.getKey();
      // case 1:
      // TODO: When the arrays can be aliases, one could be less strict here, and only raise the
      // abstract value of the other array access.
      if (otherArrayAccess.containsModifiableAliasOf(this, arrayAccess)) {
        arrayValuesIterator.remove(); // remove information completely
      }
    }

    // case 2:
    Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator = fieldValues.entrySet().iterator();
    while (fieldValuesIterator.hasNext()) {
      Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
      FieldAccess otherFieldAccess = entry.getKey();
      JavaExpression otherReceiver = otherFieldAccess.getReceiver();
      if (otherReceiver.containsModifiableAliasOf(this, arrayAccess)) {
        // remove information completely
        fieldValuesIterator.remove();
      }
    }

    // case 3:
    methodCallExpressions.clear();
    removeIteratedCollectionElementsWithMethodCallReference();
  }

  /**
   * Removes any information in this store that might not be true any more after {@code var} has
   * been assigned a new value. This includes the following steps:
   *
   * <ol>
   *   <li value="1">Remove any abstract values for field accesses <em>b.g</em> where {@code var}
   *                 appears in the receiver <em>b</em>.
   *   <li value="2">Remove any abstract values for array accesses <em>a[i]</em> where {@code var}
   *                 appears in the array <em>a</em> or the index <em>i</em>.
   *   <li value="3">Remove any information about method calls where the receiver or any of the
   *                 parameters contains {@code var}, including about iterated collection elements
   *                 whose reference is such a method call, such as {@code list.get(i)}.
   * </ol>
   *
   * @param var the local variable that is assigned
   */
  protected void removeConflicting(LocalVariable var) {
    Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator = fieldValues.entrySet().iterator();
    while (fieldValuesIterator.hasNext()) {
      Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
      FieldAccess otherFieldAccess = entry.getKey();
      // case 1:
      if (otherFieldAccess.containsSyntacticEqualJavaExpression(var)) {
        fieldValuesIterator.remove();
      }
    }

    Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator = arrayValues.entrySet().iterator();
    while (arrayValuesIterator.hasNext()) {
      Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
      ArrayAccess otherArrayAccess = entry.getKey();
      // case 2:
      if (otherArrayAccess.containsSyntacticEqualJavaExpression(var)) {
        arrayValuesIterator.remove();
      }
    }

    Iterator<Map.Entry<MethodCall, V>> methodCallExpressionsIterator =
        methodCallExpressions.entrySet().iterator();
    while (methodCallExpressionsIterator.hasNext()) {
      Map.Entry<MethodCall, V> entry = methodCallExpressionsIterator.next();
      MethodCall otherMethodAccess = entry.getKey();
      // case 3:
      if (otherMethodAccess.containsSyntacticEqualJavaExpression(var)) {
        methodCallExpressionsIterator.remove();
      }
    }

    // case 3, for iterated collection elements such as `list.get(i)`:
    iteratedCollectionElements
        .keySet()
        .removeIf(
            ice ->
                ice.reference instanceof MethodCall
                    && ice.reference.containsSyntacticEqualJavaExpression(var));
  }

  /**
   * Returns true if the objects {@code a} and {@code b} can be aliases. Returns a conservative
   * answer (i.e., returns {@code true} if not enough information is available to determine
   * aliasing).
   */
  @Override
  public boolean canAlias(JavaExpression a, JavaExpression b) {
    Types types = analysis.getTypes();
    // Erasure makes the test conservative for type variables, wildcards, and type arguments.  For
    // example, values of types `T` and `U` that are both bounded by `D` might be the same object,
    // and so might values of types `Box<? extends Number>` and `Box<? super Integer>`, even though
    // neither type is a subtype of the other.
    TypeMirror ta = types.erasure(a.getType());
    TypeMirror tb = types.erasure(b.getType());
    return types.isSubtype(ta, tb) || types.isSubtype(tb, ta);
  }

  /* --------------------------------------------------------- */
  /* Handling of local variables */
  /* --------------------------------------------------------- */

  /**
   * Returns the current abstract value of a local variable, or {@code null} if no information is
   * available.
   *
   * @param n the local variable
   * @return the current abstract value of a local variable, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(LocalVariableNode n) {
    VariableElement el = n.getElement();
    return localVariableValues.get(new LocalVariable(el));
  }

  /* --------------------------------------------------------- */
  /* Handling of the current object */
  /* --------------------------------------------------------- */

  /**
   * Returns the current abstract value of the current object, or {@code null} if no information is
   * available.
   *
   * @param n a reference to "this"
   * @return the current abstract value of the current object, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(ThisNode n) {
    return thisValue;
  }

  /* --------------------------------------------------------- */
  /* Helper and miscellaneous methods */
  /* --------------------------------------------------------- */

  @SuppressWarnings("unchecked")
  @Override
  public S copy() {
    return analysis.createCopiedStore((S) this);
  }

  @Override
  public S leastUpperBound(S other) {
    return upperBound(other, false);
  }

  @Override
  public S widenedUpperBound(S previous) {
    return upperBound(previous, true);
  }

  private S upperBound(S other, boolean shouldWiden) {
    S newStore = analysis.createEmptyStore(sequentialSemantics);

    upperBoundOfMaps(
        localVariableValues, other.localVariableValues, newStore.localVariableValues, shouldWiden);
    upperBoundOfMaps(
        iteratedCollectionElements,
        other.iteratedCollectionElements,
        newStore.iteratedCollectionElements,
        shouldWiden);

    // information about the current object
    {
      V otherVal = other.thisValue;
      V myVal = thisValue;
      if (myVal == null || otherVal == null) {
        newStore.thisValue = null;
      } else {
        newStore.thisValue = upperBoundOfValues(otherVal, myVal, shouldWiden);
      }
    }

    upperBoundOfMaps(fieldValues, other.fieldValues, newStore.fieldValues, shouldWiden);
    upperBoundOfMaps(arrayValues, other.arrayValues, newStore.arrayValues, shouldWiden);
    upperBoundOfMaps(
        methodCallExpressions,
        other.methodCallExpressions,
        newStore.methodCallExpressions,
        shouldWiden);
    upperBoundOfMaps(classValues, other.classValues, newStore.classValues, shouldWiden);
    return newStore;
  }

  /**
   * Puts into {@code result} the upper bound of the values that {@code thisMap} and {@code
   * otherMap} give each key that both maps contain. A key that only one of the maps contains is
   * discarded, because the other map implicitly gives it the value top.
   *
   * @param <K> the type of the keys, such as {@link LocalVariable}
   * @param thisMap a map in this store, such as {@link #localVariableValues}
   * @param otherMap the corresponding map in the other store
   * @param result the corresponding map in the new store, which this method side-effects
   * @param shouldWiden true if the values should be widened rather than joined
   */
  private <K extends Object> void upperBoundOfMaps(
      Map<K, V> thisMap, Map<K, V> otherMap, Map<K, V> result, boolean shouldWiden) {
    for (Map.Entry<K, V> e : otherMap.entrySet()) {
      K key = e.getKey();
      V thisVal = thisMap.get(key);
      if (thisVal != null) {
        V mergedVal = upperBoundOfValues(e.getValue(), thisVal, shouldWiden);
        if (mergedVal != null) {
          result.put(key, mergedVal);
        }
      }
    }
  }

  private V upperBoundOfValues(V otherVal, V thisVal, boolean shouldWiden) {
    return shouldWiden ? thisVal.widenUpperBound(otherVal) : thisVal.leastUpperBound(otherVal);
  }

  /**
   * Returns true iff this {@link CFAbstractStore} contains a superset of the map entries of the
   * argument {@link CFAbstractStore}. Note that we test the entry keys and values by Java equality,
   * not by any subtype relationship. This method is used primarily to simplify the equals
   * predicate.
   *
   * @param other a store
   * @return true iff this store contains a superset of the map entries of {@code other}
   */
  protected boolean supersetOf(CFAbstractStore<V, S> other) {
    return mapSupersetOf(localVariableValues, other.localVariableValues)
        && mapSupersetOf(iteratedCollectionElements, other.iteratedCollectionElements)
        && Objects.equals(thisValue, other.thisValue)
        && mapSupersetOf(fieldValues, other.fieldValues)
        && mapSupersetOf(arrayValues, other.arrayValues)
        && mapSupersetOf(methodCallExpressions, other.methodCallExpressions)
        && mapSupersetOf(classValues, other.classValues);
  }

  /**
   * Returns true if {@code map} contains every entry of {@code otherMap}, comparing keys and values
   * by {@code equals}.
   *
   * @param <K> the type of the keys, such as {@link LocalVariable}
   * @param map a map in this store, such as {@link #localVariableValues}
   * @param otherMap the corresponding map in another store
   * @return true if {@code map} contains every entry of {@code otherMap}
   */
  private <K> boolean mapSupersetOf(Map<K, V> map, Map<K, V> otherMap) {
    return map.entrySet().containsAll(otherMap.entrySet());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof CFAbstractStore) {
      @SuppressWarnings("unchecked")
      CFAbstractStore<V, S> other = (CFAbstractStore<V, S>) o;
      return this.supersetOf(other) && other.supersetOf(this);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    // `equals` is mutual containment of the six maps, so equal stores have equal key sets --
    // except that a subclass may ignore some entries of `fieldValues`, which is why the keys of
    // `fieldValues` are hashed by the overridable `fieldValuesKeysHashCode`.  `Set.hashCode` does
    // not depend on iteration order.
    // This hashes only the keys, not the values, because `CFAbstractValue.hashCode` is not
    // consistent with `CFAbstractValue.equals`: `equals` compares underlying types with
    // `Types.isSameType` and annotations with `AnnotationUtils.areSame`, but `hashCode` uses the
    // identity hash codes of the javac `Type` and of each javac annotation mirror.
    return Objects.hash(
        localVariableValues.keySet(),
        iteratedCollectionElements.keySet(),
        fieldValuesKeysHashCode(),
        arrayValues.keySet(),
        methodCallExpressions.keySet(),
        classValues.keySet());
  }

  /**
   * Returns the contribution of the keys of {@link #fieldValues} to {@link #hashCode}. The result
   * is the hash code of a set of field accesses, as defined by {@link java.util.Set#hashCode}: the
   * sum of the hash codes of its elements.
   *
   * <p>A subclass whose {@code supersetOf} ignores some entries of {@code fieldValues} must
   * override this method to ignore the same entries, so that {@code hashCode} remains consistent
   * with {@code equals}.
   *
   * @return the contribution of the keys of {@code fieldValues} to {@code hashCode}
   */
  protected int fieldValuesKeysHashCode() {
    return fieldValues.keySet().hashCode();
  }

  @SideEffectFree
  @Override
  public String toString() {
    return visualize(new StringCFGVisualizer<>());
  }

  @Override
  public String visualize(CFGVisualizer<?, S, ?> viz) {
    // This cast is guaranteed to be safe, as long as the CFGVisualizer is created by
    // CFGVisualizer<Value, Store, TransferFunction> createCFGVisualizer() of
    // GenericAnnotatedTypeFactory.
    @SuppressWarnings("unchecked")
    CFGVisualizer<V, S, ?> castedViz = (CFGVisualizer<V, S, ?>) viz;
    String internal = internalVisualize(castedViz);
    if (internal.trim().isEmpty()) {
      return this.getClassAndUid() + "()";
    } else {
      return this.getClassAndUid() + "(" + viz.getSeparator() + internal + ")";
    }
  }

  /**
   * Adds a representation of the internal information of this Store to visualizer {@code viz}.
   *
   * @param viz the visualizer
   * @return a representation of the internal information of this {@link Store}
   */
  protected String internalVisualize(CFGVisualizer<V, S, ?> viz) {
    StringJoiner res = new StringJoiner(viz.getSeparator());
    for (LocalVariable lv : ToStringComparator.sorted(localVariableValues.keySet())) {
      res.add(viz.visualizeStoreLocalVar(lv, localVariableValues.get(lv)));
    }
    List<Map.Entry<IteratedCollectionElement, V>> iceEntries =
        new ArrayList<>(iteratedCollectionElements.entrySet());
    // Sort by node uid, which is cheap and deterministic.  Elements of different loops can have the
    // same `toString()`, so sorting by `toString()` would not be deterministic.
    iceEntries.sort(Comparator.comparingLong(e -> e.getKey().node.getUid()));
    for (Map.Entry<IteratedCollectionElement, V> iceEntry : iceEntries) {
      res.add(viz.visualizeStoreIteratedCollectionElt(iceEntry.getKey(), iceEntry.getValue()));
    }
    if (thisValue != null) {
      res.add(viz.visualizeStoreThisVal(thisValue));
    }
    for (FieldAccess fa : ToStringComparator.sorted(fieldValues.keySet())) {
      res.add(viz.visualizeStoreFieldVal(fa, fieldValues.get(fa)));
    }
    for (ArrayAccess fa : ToStringComparator.sorted(arrayValues.keySet())) {
      res.add(viz.visualizeStoreArrayVal(fa, arrayValues.get(fa)));
    }
    for (MethodCall fa : ToStringComparator.sorted(methodCallExpressions.keySet())) {
      res.add(viz.visualizeStoreMethodVals(fa, methodCallExpressions.get(fa)));
    }
    for (ClassName fa : ToStringComparator.sorted(classValues.keySet())) {
      res.add(viz.visualizeStoreClassVals(fa, classValues.get(fa)));
    }
    return res.toString();
  }
}
