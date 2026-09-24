package org.checkerframework.checker.modifiability;

import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.function.Predicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.modifiability.iterator.IteratorChecker;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.PolyModifiable;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.UnmodifiableParam;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Shared annotated type factory logic for the Modifiability sub-checkers. */
public abstract class ModifiabilityBaseAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link IteratorPolyMod} qualifier. */
  protected final AnnotationMirror ITERATOR_POLY_MOD;

  /** The erased {@code java.util.Collection} type. */
  protected final TypeMirror collectionErasure;

  /** The erased {@code java.util.Map} type. */
  protected final TypeMirror mapErasure;

  /** The erased {@code java.util.Map.Entry} type. */
  protected final TypeMirror mapEntryErasure;

  /** The erased {@code java.util.Iterator} type. */
  protected final TypeMirror iteratorErasure;

  /** The erased {@code java.util.ListIterator} type. */
  protected final TypeMirror listIteratorErasure;

  /**
   * The methods whose result this factory refines: {@code Iterable.iterator()}, {@code
   * List.listIterator()}, and {@code List.listIterator(int)}.
   */
  private final List<ExecutableElement> iteratorMethods;

  /**
   * Creates a ModifiabilityBaseAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  protected ModifiabilityBaseAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.ITERATOR_POLY_MOD = AnnotationBuilder.fromClass(elements, IteratorPolyMod.class);
    this.collectionErasure = erasureOf("java.util.Collection");
    this.mapErasure = erasureOf("java.util.Map");
    this.mapEntryErasure = erasureOf("java.util.Map.Entry");
    this.iteratorErasure = erasureOf("java.util.Iterator");
    this.listIteratorErasure = erasureOf("java.util.ListIterator");
    this.iteratorMethods =
        List.of(
            TreeUtils.getMethod("java.lang.Iterable", "iterator", 0, processingEnv),
            TreeUtils.getMethod("java.util.List", "listIterator", 0, processingEnv),
            TreeUtils.getMethod("java.util.List", "listIterator", 1, processingEnv));
  }

  /**
   * Returns the erasure of the named type.
   *
   * <p>The parameter is {@code @FullyQualifiedName} rather than {@code @CanonicalName}, which is
   * what {@link javax.lang.model.util.Elements#getTypeElement} really requires, because the
   * Signature Checker cannot prove that a string literal such as {@code "java.util.Map.Entry"} is a
   * canonical name.
   *
   * @param canonicalName the canonical name of a type that is always present
   * @return the erasure of the named type
   */
  protected final TypeMirror erasureOf(
      @UnderInitialization(BaseAnnotatedTypeFactory.class) ModifiabilityBaseAnnotatedTypeFactory this,
      @FullyQualifiedName String canonicalName) {
    return types.erasure(elements.getTypeElement(canonicalName).asType());
  }

  // -- Qualifiers of this checker's hierarchy ----------

  /**
   * Returns the top qualifier of this checker's hierarchy, such as {@code @MaybeGrowable}.
   *
   * @return the top qualifier of this checker's hierarchy
   */
  protected abstract AnnotationMirror topAnnotation();

  /**
   * Returns the positive capability qualifier, such as {@code @Growable}.
   *
   * @return the positive capability qualifier
   */
  protected abstract AnnotationMirror positiveCapability();

  /**
   * Returns the negative capability qualifier, such as {@code @Ungrowable}. Only call this method
   * if {@link #hasNegativeCapability} returns true.
   *
   * @return the negative capability qualifier
   */
  protected abstract AnnotationMirror negativeCapability();

  /**
   * Returns the polymorphic capability qualifier, such as {@code @PolyGrowable}.
   *
   * @return the polymorphic capability qualifier
   */
  protected abstract AnnotationMirror polyCapability();

  /**
   * Returns true if this checker's hierarchy contains a negative qualifier, such as
   * {@code @Ungrowable}. The Iterator hierarchy does not.
   *
   * @return true if this checker's hierarchy contains a negative qualifier
   */
  protected boolean hasNegativeCapability() {
    return true;
  }

  // -- Expansion of the whole-modifiability aliases ----------

  /**
   * Returns true if {@code type} structurally cannot support this checker's capability, so that
   * {@code @Modifiable} and {@code @Unmodifiable} weaken to the top qualifier on {@code type}. For
   * example, {@code Map.Entry} cannot grow.
   *
   * @param type the type on which an alias was written; it is an upper bound, so it is never a type
   *     variable or a wildcard, and {@link #lacksCapability} has already decomposed intersection
   *     types, so it is never an intersection type either
   * @return true if {@code type} structurally cannot support this checker's capability
   */
  protected boolean typeLacksCapability(TypeMirror type) {
    return false;
  }

  /**
   * Returns erased types whose every subtype has this checker's capability, unless {@link
   * #typeLacksCapability} holds of the subtype. For example, the Replace Checker's result includes
   * {@code List}.
   *
   * <p>A type variable or wildcard may be instantiated by any subtype of its upper bound, including
   * a subtype that also implements an unrelated interface. For example, a type variable whose upper
   * bound is {@code AbstractCollection} or {@code Serializable} may be instantiated by {@code
   * HashSet}, which cannot be replaced into. So an alias written on a type variable claims this
   * checker's capability only if the upper bound is a subtype of one of these types.
   *
   * @return erased types whose subtypes have this checker's capability
   */
  protected abstract List<TypeMirror> typesWithCapability();

  /**
   * Returns true if a type variable or wildcard whose upper bound is {@code bound} may be
   * instantiated by a type that structurally cannot support this checker's capability; that is, if
   * no bound is a subtype of a type in {@link #typesWithCapability}.
   *
   * @param bound the upper bound of a type variable or wildcard; it may be an intersection type
   * @return true if some instantiation of the type variable or wildcard may lack the capability
   */
  private boolean someInstantiationLacksCapability(TypeMirror bound) {
    List<? extends TypeMirror> bounds =
        bound.getKind() == TypeKind.INTERSECTION
            ? ((IntersectionType) bound).getBounds()
            : List.of(bound);
    for (TypeMirror withCapability : typesWithCapability()) {
      for (TypeMirror b : bounds) {
        if (TypesUtils.isErasedSubtype(b, withCapability, types)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if {@code @PolyModifiable} weakens to the top qualifier on {@code type}, rather
   * than to this checker's polymorphic qualifier. This differs from {@link #typeLacksCapability}
   * because a polymorphic qualifier may usefully carry a capability that the type itself cannot
   * exercise; for example, {@code Map.Entry} carries the replace capability of its map.
   *
   * @param type the type on which {@code @PolyModifiable} was written; it is an upper bound, so it
   *     is never a type variable or a wildcard, and {@link #lacksCapability} has already decomposed
   *     intersection types, so it is never an intersection type either
   * @return true if {@code @PolyModifiable} weakens to the top qualifier on {@code type}
   */
  protected boolean polyLacksCapability(TypeMirror type) {
    return false;
  }

  /**
   * Returns the result of {@code lacks} on {@code type}, decomposing an intersection type. A value
   * of an intersection type is a value of each of its bounds, so it has a capability if any bound
   * does; for example, the upper bound of {@code <T extends Deque<String> & Cloneable>} is an
   * intersection type, and such a {@code T} can be sequenced-grown.
   *
   * @param type the type on which an alias was written; it is an upper bound, so it is never a type
   *     variable or a wildcard
   * @param lacks {@link #typeLacksCapability} or {@link #polyLacksCapability}
   * @return true if {@code lacks} holds of {@code type} or of every bound of {@code type}
   */
  private boolean lacksCapability(TypeMirror type, Predicate<TypeMirror> lacks) {
    if (type.getKind() == TypeKind.INTERSECTION) {
      for (TypeMirror bound : ((IntersectionType) type).getBounds()) {
        if (!lacksCapability(bound, lacks)) {
          return false;
        }
      }
      return true;
    }
    return lacks.test(type);
  }

  /**
   * Returns true if this checker's hierarchy is one of the capabilities that the
   * whole-modifiability aliases ({@code @Modifiable}, {@code @Unmodifiable},
   * {@code @MaybeModifiable}, {@code @UnmodifiableParam}, and {@code @PolyModifiable}) expand into.
   * The Iterator hierarchy is not: it states what a collection's iterator preserves rather than
   * whether a mutating method throws {@link UnsupportedOperationException}.
   *
   * @return true if the whole-modifiability aliases expand into this checker's hierarchy
   */
  protected boolean expandsModifiabilityAliases() {
    return true;
  }

  /**
   * Expands the whole-modifiability aliases into this hierarchy, with structural weakening only for
   * aliases whose meaning depends on the annotated type.
   *
   * <p>{@code @Modifiable} and {@code @Unmodifiable} claim every component capability, so on a type
   * that structurally cannot exercise this checker's capability, they weaken to the top qualifier;
   * see {@link #typeLacksCapability}. {@code @PolyModifiable} weakens under the different condition
   * of {@link #polyLacksCapability}.
   *
   * <p>When {@code tm} is null, as for an alias written in {@code @DefaultQualifier}, no structural
   * weakening is applied.
   *
   * <p>A type variable or wildcard is classified by its upper bound, so that, for example, {@code
   * <T extends Deque<String>>} has the same capabilities as {@code Deque}. In addition, on a type
   * variable or wildcard, {@code @Modifiable} and {@code @Unmodifiable} weaken to the top qualifier
   * unless the upper bound is a subtype of a type that has this checker's capability; see {@link
   * #typesWithCapability}. For example, {@code @Modifiable T} for an unbounded {@code T} is
   * {@code @MaybeReplaceable}, because {@code T} may be {@code Set}.
   */
  @Override
  public AnnotationMirror canonicalAnnotation(
      AnnotationMirror annotation, @Nullable TypeMirror tm) {
    if (expandsModifiabilityAliases()) {
      TypeMirror bound = tm == null ? null : TypesUtils.upperBound(tm);
      if (areSameByClass(annotation, Modifiable.class)
          || areSameByClass(annotation, Unmodifiable.class)) {
        boolean weaken =
            bound != null
                // `tm != null` is redundant because if `bound` is non-null, then so is `tm`.
                && tm != null
                && (lacksCapability(bound, this::typeLacksCapability)
                    || ((tm.getKind() == TypeKind.TYPEVAR || tm.getKind() == TypeKind.WILDCARD)
                        && someInstantiationLacksCapability(bound)));
        if (weaken) {
          return topAnnotation();
        }
        return areSameByClass(annotation, Modifiable.class)
            ? positiveCapability()
            : negativeCapability();
      } else if (areSameByClass(annotation, PolyModifiable.class)) {
        return bound != null && lacksCapability(bound, this::polyLacksCapability)
            ? topAnnotation()
            : polyCapability();
      } else if (areSameByClass(annotation, MaybeModifiable.class)
          || areSameByClass(annotation, UnmodifiableParam.class)) {
        return topAnnotation();
      }
    }
    return super.canonicalAnnotation(annotation);
  }

  @Override
  public AnnotationMirror canonicalAnnotation(AnnotationMirror annotation) {
    return canonicalAnnotation(annotation, null);
  }

  // -- Refinement of method return types ----------

  /**
   * Returns the erased type that the result of an iterator method must be a subtype of for this
   * checker to refine the result, or null if this checker does not refine iterator results. The
   * Shrink Checker refines the result of {@code iterator()} and {@code listIterator()}; the Grow
   * and Replace Checkers refine only the result of {@code listIterator()}, because a plain {@code
   * Iterator} can neither grow nor replace.
   *
   * @return the erased upper bound of the iterator results this checker refines, or null
   */
  protected @Nullable TypeMirror refinedIteratorResultBound() {
    return null;
  }

  @Override
  protected ParameterizedExecutableType methodFromUse(
      MethodInvocationTree tree, boolean inferTypeArgs) {
    ParameterizedExecutableType mType = super.methodFromUse(tree, inferTypeArgs);
    AnnotatedExecutableType method = mType.executableType();

    TypeMirror iteratorResultBound = refinedIteratorResultBound();
    if (iteratorResultBound != null
        && TypesUtils.isErasedSubtype(
            method.getReturnType().getUnderlyingType(), iteratorResultBound, types)) {
      refineIteratorReturnType(tree, method);
    }

    // When `inferTypeArgs` is false, this is being called during type argument inference, possibly
    // while inferring the type of the argument itself.  Refining would compute the argument's type,
    // which would re-enter inference for the argument and recurse infinitely.
    ExecutableElement invokedMethod = TreeUtils.elementFromUse(tree);
    if (inferTypeArgs && getDeclAnnotation(invokedMethod, PreservesModifiability.class) != null) {
      refineReturnTypeForPreservesModifiability(tree, method);
    }

    return mType;
  }

  /**
   * Refines the return type of a {@code @PreservesModifiability} method.
   *
   * <p>If the method does not have exactly one formal parameter, which is not a varargs parameter,
   * and a non-void result, then the annotation has no effect.
   *
   * <p>Otherwise, if the declared return type has a qualifier other than the top qualifier, that
   * declared qualifier is used. If the first argument has this checker's positive qualifier (for
   * example, {@code @Shrinkable}), then so does the return type. For every other first argument,
   * the return type is the top qualifier.
   *
   * <p>Such a method cannot be annotated as {@code @Poly*}, because a negative (for example,
   * {@code @Unshrinkable}) input could yield either a positive or a negative result. It would be
   * imprecise to always use the top qualifier, because passing a positive argument guarantees a
   * positive return type.
   *
   * <p>This method is called by all five sub-checkers.
   *
   * @param tree an invocation of a {@code @PreservesModifiability} method
   * @param methodType the annotated executable type of the invoked method
   */
  protected void refineReturnTypeForPreservesModifiability(
      MethodInvocationTree tree, AnnotatedExecutableType methodType) {
    AnnotatedTypeMirror returnType = methodType.getReturnType();
    if (methodType.getParameterTypes().size() != 1
        || methodType.getElement().isVarArgs()
        || tree.getArguments().isEmpty()
        || returnType.getUnderlyingType().getKind() == TypeKind.VOID) {
      // The annotation relates the result to the sole argument, so it says nothing about such a
      // method.  (For a varargs method, the first argument of a call is an element of the varargs
      // array rather than the sole formal parameter.)  `ModifiabilityVisitor` issues an error for a
      // source declaration like this; the declaration might also come from an annotation file,
      // which is not checked.
      return;
    }
    AnnotationMirror declaredReturnAnno =
        returnType.getPrimaryAnnotationInHierarchy(topAnnotation());
    if (declaredReturnAnno != null
        && !AnnotationUtils.areSameByName(declaredReturnAnno, topAnnotation())) {
      // The declared result states its own qualifier, which is more precise (or, for a negative
      // qualifier, a stronger guarantee) than what the argument implies.
      return;
    }
    AnnotatedTypeMirror argumentType = getAnnotatedType(tree.getArguments().get(0));
    // `hasAnnotation()` rather than `hasPrimaryAnnotation()`, so that an argument whose type is a
    // type variable is classified by its upper bound.
    if (argumentType.hasAnnotation(positiveCapability())) {
      returnType.replaceAnnotation(positiveCapability());
    } else {
      returnType.replaceAnnotation(topAnnotation());
    }
  }

  /**
   * Refines the result of {@code iterator()} and {@code listIterator()} based on
   * {@code @IteratorPolyMod}.
   *
   * <p>{@code iterator()} and {@code listIterator()} cannot be annotated as {@code @PolyModifiable}
   * because not all collections preserve the modifiability of their iterators. (For example, {@code
   * CopyOnWriteArrayList} has unmodifiable iterators even though the list is modifiable.) Thus,
   * special treatment is needed for iterator methods.
   *
   * <p>A declared negative result keeps its declared qualifier, since such an iterator never has
   * the capability. A declared polymorphic result keeps the qualifier that polymorphic resolution
   * gives it. Otherwise, the result qualifier is computed from the receiver: the iterator of a
   * receiver with this checker's negative qualifier also has that negative qualifier, and the
   * iterator of a receiver that has both this checker's positive qualifier and
   * {@code @IteratorPolyMod} has the positive qualifier. In every other case the result is the top
   * qualifier.
   *
   * <p>A declared positive result is not kept, because it holds only when the receiver has the
   * capability and preserves it. For example, {@code ArrayList} declares {@code @Growable
   * ListIterator<E> listIterator()}, but an {@code @Ungrowable ArrayList} has an
   * {@code @Ungrowable} list iterator and a {@code @MaybeGrowable ArrayList} has a
   * {@code @MaybeGrowable} one.
   *
   * <p>This method is called by the Grow, Shrink, and Replace Checkers; see {@link
   * #refinedIteratorResultBound}.
   *
   * @param tree the iterator method invocation
   * @param methodType the annotated executable type of the invoked method
   */
  protected void refineIteratorReturnType(
      MethodInvocationTree tree, AnnotatedExecutableType methodType) {
    if (!hasNegativeCapability()) {
      // The Iterator hierarchy has no negative qualifier, so there is nothing to refine.
      return;
    }
    if (!isIteratorMethodInvocation(tree)) {
      // Some other method that happens to return an Iterator makes no promise about the
      // modifiability of its result.
      return;
    }
    AnnotatedTypeMirror returnType = methodType.getReturnType();
    // Keep an explicit "no capability" iterator contract (for example, CopyOnWriteArrayList),
    // whose iterator lacks the capability no matter what the receiver is.
    if (returnType.hasPrimaryAnnotation(negativeCapability())) {
      return;
    }
    // Keep a declared polymorphic result, which relates the iterator to the receiver.  Test the
    // declaration rather than `returnType`: `super.methodFromUse()` has already resolved the
    // polymorphic qualifier when it infers type arguments, but not otherwise; see
    // `GenericAnnotatedTypeFactory.methodFromUsePreSubstitution()`.
    if (getAnnotatedType(methodType.getElement())
        .getReturnType()
        .hasPrimaryAnnotation(polyCapability())) {
      return;
    }

    // `getReceiverType()` also handles an implicit `this` receiver, whose type is the receiver
    // type of the enclosing method.
    AnnotatedTypeMirror receiverType = getReceiverType(tree);
    if (receiverType == null) {
      // The receiver's type is unknown, so nothing is known about its iterator.
      returnType.replaceAnnotation(topAnnotation());
      return;
    }

    // The receiver tests below use `hasAnnotation()` rather than `hasPrimaryAnnotation()`, so that
    // a
    // receiver whose type is a type variable is classified by its upper bound.

    // The iterator of a collection that lacks the capability also lacks the capability, even if
    // the declaration says that the iterator has it (as ArrayList's does).
    if (receiverType.hasAnnotation(negativeCapability())) {
      returnType.replaceAnnotation(negativeCapability());
      return;
    }

    // The receiver has the capability; its iterator does too if the receiver is @IteratorPolyMod.
    if (receiverType.hasAnnotation(positiveCapability())) {
      AnnotatedTypeMirror iteratorHierarchyType =
          getTypeFactoryOfSubchecker(IteratorChecker.class).getReceiverType(tree);
      if (iteratorHierarchyType != null && iteratorHierarchyType.hasAnnotation(ITERATOR_POLY_MOD)) {
        returnType.replaceAnnotation(positiveCapability());
        return;
      }
    }

    // The receiver does not both have the capability and preserve it, so its iterator has no
    // guarantee about the capability -- even if the declared result says that it does, as
    // ArrayList's `@Growable ListIterator<E> listIterator()` does.
    returnType.replaceAnnotation(topAnnotation());
  }

  /**
   * Returns true if {@code tree} is an invocation of {@code Iterable.iterator()}, {@code
   * List.listIterator()}, {@code List.listIterator(int)}, or an override of one of those.
   *
   * @param tree a method invocation
   * @return true if {@code tree} is an invocation of an iterator method
   */
  private boolean isIteratorMethodInvocation(MethodInvocationTree tree) {
    return TreeUtils.isMethodInvocation(tree, iteratorMethods, processingEnv);
  }
}
