package org.checkerframework.framework.flow;

import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * An implementation of an abstract value used by the Checker Framework
 * org.checkerframework.dataflow analysis.
 *
 * <p>A value holds a set of annotations and a type mirror. The set of annotations represents the
 * primary annotation on a type; therefore, the set of annotations must have an annotation for each
 * hierarchy unless the type mirror is a type variable or a wildcard that extends a type variable.
 * Both type variables and wildcards may be missing a primary annotation. For this set of
 * annotations, there is an additional constraint that only wildcards that extend type variables can
 * be missing annotations.
 *
 * <p>In order to compute {@link #leastUpperBound(CFAbstractValue)} and {@link
 * #mostSpecific(CFAbstractValue, CFAbstractValue)}, the case where one value has an annotation in a
 * hierarchy and the other does not must be handled. For type variables, the {@link
 * AnnotatedTypeVariable} for the declaration of the type variable is used. The {@link
 * AnnotatedTypeVariable} is computed using the type mirror. For wildcards, it is not always
 * possible to get the {@link AnnotatedWildcardType} for the type mirror. However, a
 * CFAbstractValue's type mirror is only a wildcard if the type of some expression is a wildcard.
 * The type of an expression is only a wildcard because the Checker Framework does not implement
 * capture conversion. For these uses of uncaptured wildcards, only the primary annotation on the
 * upper bound is ever used. So, the set of annotations represents the primary annotation on the
 * wildcard's upper bound. If that upper bound is a type variable, then the set of annotations could
 * be missing an annotation in a hierarchy.
 *
 * @param <V> the values that this CFAbstractValue wraps
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements AbstractValue<V> {

  /** The analysis class this value belongs to. */
  protected final CFAbstractAnalysis<V, ?, ?> analysis;

  /** The type factory. */
  protected final AnnotatedTypeFactory atypeFactory;

  /** The qualifier hierarchy. */
  protected final QualifierHierarchy qualHierarchy;

  /** The underlying (Java) type in this abstract value. */
  protected final TypeMirror underlyingType;

  /** The annotations in this abstract value. */
  protected final AnnotationMirrorSet annotations;

  /**
   * Creates a new CFAbstractValue.
   *
   * @param analysis the analysis class this value belongs to
   * @param annotations the annotations in this abstract value
   * @param underlyingType the underlying (Java) type in this abstract value
   */
  @SuppressWarnings("this-escape")
  protected CFAbstractValue(
      CFAbstractAnalysis<V, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType) {
    this.analysis = analysis;
    this.atypeFactory = analysis.getTypeFactory();
    this.qualHierarchy = atypeFactory.getQualifierHierarchy();
    this.annotations = annotations;
    this.underlyingType = underlyingType;

    assert validateSet(this.getAnnotations(), this.getUnderlyingType(), atypeFactory)
        : "Encountered invalid type: "
            + underlyingType
            + " annotations: "
            + StringsPlume.join(", ", annotations);
  }

  /**
   * Returns true if the set has an annotation from every hierarchy (or if it doesn't need to);
   * returns false if the set is missing an annotation from some hierarchy.
   *
   * @param annos a set of annotations
   * @param typeMirror where the annotations are written
   * @param atypeFactory the type factory
   * @return true if no annotations are missing
   */
  public static boolean validateSet(
      AnnotationMirrorSet annos, TypeMirror typeMirror, AnnotatedTypeFactory atypeFactory) {

    boolean canBeMissing = canBeMissingAnnotations(typeMirror);
    if (canBeMissing) {
      return true;
    }

    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
    AnnotationMirrorSet missingHierarchy = null;
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      AnnotationMirror anno = qualHierarchy.findAnnotationInHierarchy(annos, top);
      if (anno == null) {
        if (missingHierarchy == null) {
          missingHierarchy = new AnnotationMirrorSet();
        }
        missingHierarchy.add(top);
      }
    }

    return missingHierarchy == null;
  }

  /**
   * Returns whether or not the set of annotations can be missing an annotation for any hierarchy.
   *
   * @return whether or not the set of annotations can be missing an annotation
   */
  public boolean canBeMissingAnnotations() {
    return canBeMissingAnnotations(underlyingType);
  }

  /**
   * Returns true if it is OK for the given type mirror not to be annotated, such as for VOID, NONE,
   * PACKAGE, TYPEVAR, and some WILDCARD.
   *
   * @param typeMirror a type
   * @return true if it is OK for the given type mirror not to be annotated
   */
  private static boolean canBeMissingAnnotations(TypeMirror typeMirror) {
    if (typeMirror == null) {
      return false;
    }
    if (typeMirror.getKind() == TypeKind.VOID
        || typeMirror.getKind() == TypeKind.NONE
        || typeMirror.getKind() == TypeKind.PACKAGE) {
      return true;
    }
    if (typeMirror.getKind() == TypeKind.WILDCARD) {
      return canBeMissingAnnotations(((WildcardType) typeMirror).getExtendsBound());
    }
    return typeMirror.getKind() == TypeKind.TYPEVAR;
  }

  /**
   * Returns a set of annotations. If {@link #canBeMissingAnnotations()} returns true, then the set
   * of annotations may not have an annotation for every hierarchy.
   *
   * <p>To get the single annotation in a particular hierarchy, use {@link
   * QualifierHierarchy#findAnnotationInHierarchy}.
   *
   * @return a set of annotations
   */
  @Pure
  public AnnotationMirrorSet getAnnotations() {
    return annotations;
  }

  @Pure
  public TypeMirror getUnderlyingType() {
    return underlyingType;
  }

  @SuppressWarnings("interning:not.interned") // efficiency pre-test
  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof CFAbstractValue)) {
      return false;
    }

    CFAbstractValue<?> other = (CFAbstractValue<?>) obj;
    if (this.getUnderlyingType() != other.getUnderlyingType()
        && !analysis.getTypes().isSameType(this.getUnderlyingType(), other.getUnderlyingType())) {
      return false;
    }
    return AnnotationUtils.areSame(this.getAnnotations(), other.getAnnotations());
  }

  @Pure
  @Override
  public int hashCode() {
    return Objects.hash(getAnnotations(), underlyingType);
  }

  /**
   * Returns the string representation, using fully-qualified names.
   *
   * @return the string representation, using fully-qualified names
   */
  @SideEffectFree
  public String toStringFullyQualified() {
    return "CFAV{" + annotations + ", " + underlyingType + '}';
  }

  /**
   * Returns the string representation, using simple (not fully-qualified) names.
   *
   * @return the string representation, using simple (not fully-qualified) names
   */
  @SideEffectFree
  public String toStringSimple() {
    return "CFAV{"
        + AnnotationUtils.toStringSimple(annotations)
        + ", "
        + TypesUtils.simpleTypeName(underlyingType)
        + '}';
  }

  /**
   * Returns the string representation.
   *
   * @return the string representation
   */
  @SideEffectFree
  @Override
  public String toString() {
    return toStringSimple();
  }

  /**
   * Returns the more specific of two values {@code this} and {@code other}. If they do not contain
   * information for all hierarchies, then it is possible that information from both {@code this}
   * and {@code other} are taken.
   *
   * <p>If neither of the two is more specific for one of the hierarchies (i.e., if the two are
   * incomparable as determined by {@link QualifierHierarchy#isSubtypeShallow(AnnotationMirror,
   * TypeMirror, AnnotationMirror, TypeMirror)}, then the respective value from {@code backup} is
   * used.
   *
   * @param other the other value to obtain information from
   * @param backup the value to use if {@code this} and {@code other} are incomparable
   * @return the more specific of two values {@code this} and {@code other}
   */
  public V mostSpecific(@Nullable V other, @Nullable V backup) {
    if (other == null) {
      @SuppressWarnings("unchecked")
      V v = (V) this;
      return v;
    }
    Types types = analysis.getTypes();
    TypeMirror mostSpecifTypeMirror;
    if (types.isAssignable(this.getUnderlyingType(), other.getUnderlyingType())) {
      mostSpecifTypeMirror = this.getUnderlyingType();
    } else if (types.isAssignable(other.getUnderlyingType(), this.getUnderlyingType())) {
      mostSpecifTypeMirror = other.getUnderlyingType();
    } else if (TypesUtils.isErasedSubtype(
        this.getUnderlyingType(), other.getUnderlyingType(), types)) {
      mostSpecifTypeMirror = this.getUnderlyingType();
    } else if (TypesUtils.isErasedSubtype(
        other.getUnderlyingType(), this.getUnderlyingType(), types)) {
      mostSpecifTypeMirror = other.getUnderlyingType();
    } else {
      mostSpecifTypeMirror = this.getUnderlyingType();
    }

    MostSpecificVisitor ms = new MostSpecificVisitor(backup);
    AnnotationMirrorSet mostSpecific =
        ms.combineSets(
            this.getUnderlyingType(),
            this.getAnnotations(),
            other.getUnderlyingType(),
            other.getAnnotations(),
            canBeMissingAnnotations(mostSpecifTypeMirror));
    if (ms.error) {
      return backup;
    }
    return analysis.createAbstractValue(mostSpecific, mostSpecifTypeMirror);
  }

  /** Computes the most specific annotations. */
  private class MostSpecificVisitor extends AnnotationSetCombiner {
    /** If set to true, then this visitor was unable to find a most specific annotation. */
    boolean error = false;

    /** Set of annotations to use if a most specific value cannot be found. */
    final AnnotationMirrorSet backupAMSet;

    /**
     * Create a {@link MostSpecificVisitor}.
     *
     * @param backup value to use if no most specific value is found
     */
    public MostSpecificVisitor(V backup) {
      if (backup != null) {
        this.backupAMSet = backup.getAnnotations();
        // this.backupTypeMirror = backup.getUnderlyingType();
        // this.backupAtv = getEffectiveTypeVar(backupTypeMirror);
      } else {
        // this.backupAtv = null;
        // this.backupTypeMirror = null;
        this.backupAMSet = null;
      }
    }

    /**
     * Returns the backup annotation that is in the same hierarchy as {@code top}.
     *
     * @param top an annotation
     * @return the backup annotation that is in the same hierarchy as {@code top}
     */
    private @Nullable AnnotationMirror getBackupAnnoIn(AnnotationMirror top) {
      if (backupAMSet == null) {
        // If there is no backup value, but one is required, then the resulting set will
        // not be the most specific.  Indicate this with the error.
        error = true;
        return null;
      }
      return qualHierarchy.findAnnotationInHierarchy(backupAMSet, top);
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a,
        TypeMirror aTypeMirror,
        AnnotationMirror b,
        TypeMirror bTypeMirror,
        AnnotationMirror top) {
      if (aTypeMirror == null) {
        throw new NullPointerException("combineTwoAnnotations: aTypeMirror==null");
      }
      if (bTypeMirror == null) {
        throw new NullPointerException("combineTwoAnnotations: bTypeMirror==null");
      }
      GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf = analysis.getTypeFactory();
      if (gatf.hasQualifierParameterInHierarchy(TypesUtils.getTypeElement(aTypeMirror), top)
          && gatf.hasQualifierParameterInHierarchy(TypesUtils.getTypeElement(bTypeMirror), top)) {
        // Both types have qualifier parameters, so they are related by invariance rather
        // than subtyping.
        if (qualHierarchy.isSubtypeShallow(a, aTypeMirror, b, bTypeMirror)
            && qualHierarchy.isSubtypeShallow(b, bTypeMirror, a, aTypeMirror)) {
          return b;
        }
      } else if (qualHierarchy.isSubtypeShallow(a, aTypeMirror, b, bTypeMirror)) {
        // `a` may not be a subtype of `b`, if one of the type mirrors isn't relevant,
        // so return the lower of the two.
        return lowestQualifier(a, b);
      } else if (qualHierarchy.isSubtypeShallow(b, bTypeMirror, a, aTypeMirror)) {
        // `b` may not be a subtype of `a`, if one of the type mirrors isn't relevant,
        // so return the lower of the two.
        return lowestQualifier(a, b);
      }
      return getBackupAnnoIn(top);
    }

    /**
     * Returns the qualifier that is the lowest in the hierarchy. If the two qualifiers are not
     * comparable, then returns the qualifier that is ordered first by {@link
     * AnnotationUtils#compareAnnotationMirrors(AnnotationMirror, AnnotationMirror)}.
     *
     * <p>This is similar to glb, but one of the given qualifiers is always returned.
     *
     * @param qual1 a qualifier
     * @param qual2 a qualifier
     * @return the qualifier that is the lowest in the hierarchy
     */
    private final AnnotationMirror lowestQualifier(AnnotationMirror qual1, AnnotationMirror qual2) {
      if (qualHierarchy.isSubtypeQualifiersOnly(qual1, qual2)) {
        return qual1;
      } else if (qualHierarchy.isSubtypeQualifiersOnly(qual2, qual1)) {
        return qual2;
      } else {
        int i = AnnotationUtils.compareAnnotationMirrors(qual1, qual2);
        if (i > 0) {
          return qual2;
        } else {
          return qual1;
        }
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoTypeVars(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      if (canCombinedSetBeMissingAnnos) {
        return null;
      } else {
        AnnotationMirror aUB = aAtv.getEffectiveAnnotationInHierarchy(top);
        AnnotationMirror bUB = bAtv.getEffectiveAnnotationInHierarchy(top);
        TypeMirror aTM = aAtv.getUnderlyingType();
        TypeMirror bTM = bAtv.getUnderlyingType();
        return combineTwoAnnotations(aUB, aTM, bUB, bTM, top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineAnnotationWithTypeVar(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {

      AnnotationMirror upperBound = typeVar.getEffectiveAnnotationInHierarchy(top);
      TypeMirror upperBoundTM = typeVar.getUpperBound().getUnderlyingType();

      if (!canCombinedSetBeMissingAnnos) {
        TypeVariable typeVarTM = typeVar.getUnderlyingType();
        return combineTwoAnnotations(annotation, typeVarTM, upperBound, typeVarTM, top);
      }
      AnnotationMirrorSet lBSet =
          AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, typeVar);
      AnnotationMirror lowerBound = qualHierarchy.findAnnotationInHierarchy(lBSet, top);
      TypeMirror lowerBoundTM = typeVar.getLowerBound().getUnderlyingType();

      TypeMirror typeVarTM = typeVar.getUnderlyingType();
      if (qualHierarchy.isSubtypeShallow(upperBound, upperBoundTM, annotation, typeVarTM)) {
        // no anno is more specific than anno
        return null;
      } else if (qualHierarchy.isSubtypeShallow(annotation, typeVarTM, lowerBound, lowerBoundTM)) {
        return lowestQualifier(annotation, lowerBound);
      } else {
        return getBackupAnnoIn(top);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Subclasses should override {@link #upperBound(CFAbstractValue, TypeMirror, boolean)} instead
   * of this method.
   */
  @Override
  public final V leastUpperBound(@Nullable V other) {
    return upperBound(other, false);
  }

  /**
   * Compute the least upper bound of two abstract values. The returned value has a Java type of
   * {@code typeMirror}. {@code typeMirror} should be an upper bound of the Java types of {@code
   * this} an {@code other}, but it does not have be to the least upper bound.
   *
   * <p>Subclasses should override {@link #upperBound(CFAbstractValue, TypeMirror, boolean)} instead
   * of this method.
   *
   * @param other another value
   * @param typeMirror the underlying Java type of the returned value, which may or may not be the
   *     least upper bound
   * @return the least upper bound of two abstract values
   */
  public final V leastUpperBound(@Nullable V other, TypeMirror typeMirror) {
    return upperBound(other, typeMirror, false);
  }

  /**
   * Compute an upper bound of two values that is wider than the least upper bound of the two
   * values. Used to jump to a higher abstraction to allow faster termination of the fixed point
   * computations in {@link Analysis}.
   *
   * <p>A particular analysis might not require widening and should implement this method by calling
   * leastUpperBound.
   *
   * <p><em>Important</em>: This method must fulfill the following contract:
   *
   * <ul>
   *   <li>Does not change {@code this}.
   *   <li>Does not change {@code previous}.
   *   <li>Returns a fresh object which is not aliased yet.
   *   <li>Returns an object of the same (dynamic) type as {@code this}, even if the signature is
   *       more permissive.
   *   <li>Is commutative.
   * </ul>
   *
   * Subclasses should override {@link #upperBound(CFAbstractValue, TypeMirror, boolean)} instead of
   * this method.
   *
   * @param previous must be the previous value
   * @return an upper bound of two values that is wider than the least upper bound of the two values
   */
  public final V widenUpperBound(@Nullable V previous) {
    return upperBound(previous, true);
  }

  /**
   * Returns the least upper bound of this and {@code other}.
   *
   * @param other an abstract value
   * @param shouldWiden true if the lub should perform widening
   * @return the least upper bound of this and {@code other}
   */
  private V upperBound(@Nullable V other, boolean shouldWiden) {
    if (other == null) {
      @SuppressWarnings("unchecked")
      V v = (V) this;
      return v;
    }
    ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
    TypeMirror lubTypeMirror =
        TypesUtils.leastUpperBound(
            this.getUnderlyingType(), other.getUnderlyingType(), processingEnv);
    return upperBound(other, lubTypeMirror, shouldWiden);
  }

  /**
   * Returns an upper bound of {@code this} and {@code other}. The underlying type of the value
   * returned is {@code upperBoundTypeMirror}. If {@code shouldWiden} is false, this method returns
   * the least upper bound of {@code this} and {@code other}.
   *
   * <p>This is the implementation of {@link #leastUpperBound(CFAbstractValue, TypeMirror)}, {@link
   * #leastUpperBound(CFAbstractValue)}, {@link #widenUpperBound(CFAbstractValue)}, and {@link
   * #upperBound(CFAbstractValue, boolean)}. Subclasses may override it.
   *
   * @param other an abstract value
   * @param upperBoundTypeMirror the underlying type of the returned value
   * @param shouldWiden true if the method should perform widening
   * @return an upper bound of this and {@code other}
   */
  protected V upperBound(@Nullable V other, TypeMirror upperBoundTypeMirror, boolean shouldWiden) {
    ValueLub valueLub = new ValueLub(shouldWiden);
    AnnotationMirrorSet lub =
        valueLub.combineSets(
            this.getUnderlyingType(),
            this.getAnnotations(),
            other.getUnderlyingType(),
            other.getAnnotations(),
            canBeMissingAnnotations(upperBoundTypeMirror));
    return analysis.createAbstractValue(lub, upperBoundTypeMirror);
  }

  /**
   * Computes the least upper bound or, if {@code shouldWiden} is true, an upper bounds of two sets
   * of annotations. The computation accounts for sets that are missing annotations in hierarchies.
   */
  protected class ValueLub extends AnnotationSetCombiner {

    /**
     * If true, this class computes an upper bound; if false, this class computes the least upper
     * bound.
     */
    private final boolean widen;

    /**
     * Creates a {@link ValueLub}.
     *
     * @param shouldWiden if true, this class computes an upper bound
     */
    public ValueLub(boolean shouldWiden) {
      this.widen = shouldWiden;
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a,
        TypeMirror aTypeMirror,
        AnnotationMirror b,
        TypeMirror bTypeMirror,
        AnnotationMirror top) {
      if (widen) {
        return qualHierarchy.widenedUpperBound(a, b);
      } else {
        return qualHierarchy.leastUpperBoundShallow(a, aTypeMirror, b, bTypeMirror);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoTypeVars(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      if (canCombinedSetBeMissingAnnos) {
        // don't add an annotation
        return null;
      } else {
        AnnotationMirror aUB = aAtv.getEffectiveAnnotationInHierarchy(top);
        AnnotationMirror bUB = bAtv.getEffectiveAnnotationInHierarchy(top);
        return combineTwoAnnotations(
            aUB, aAtv.getUnderlyingType(), bUB, bAtv.getUnderlyingType(), top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineAnnotationWithTypeVar(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      TypeMirror typeVarTM = typeVar.getUnderlyingType();
      if (canCombinedSetBeMissingAnnos) {
        // anno is the primary annotation on the use of a type variable. typeVar is a use of
        // the same type variable that does not have a primary annotation. The lub of the
        // two type variables is computed as follows. If anno is a subtype (or equal) to the
        // annotation on the lower bound of typeVar, then typeVar is the lub, so no
        // annotation is added to lubset.
        // If anno is a supertype of the annotation on the lower bound of typeVar, then the
        // lub is typeVar with a primary annotation of lub(anno, upperBound), where
        // upperBound is the annotation on the upper bound of typeVar.
        AnnotationMirrorSet lBSet =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, typeVar);
        AnnotationMirror lowerBound = qualHierarchy.findAnnotationInHierarchy(lBSet, top);
        if (qualHierarchy.isSubtypeQualifiersOnly(annotation, lowerBound)) {
          return null;
        } else {
          return combineTwoAnnotations(
              annotation,
              typeVarTM,
              typeVar.getEffectiveAnnotationInHierarchy(top),
              typeVarTM,
              top);
        }
      } else {
        return combineTwoAnnotations(
            annotation, typeVarTM, typeVar.getEffectiveAnnotationInHierarchy(top), typeVarTM, top);
      }
    }
  }

  /**
   * Compute the greatest lower bound of two values.
   *
   * <p><em>Important</em>: This method must fulfill the following contract:
   *
   * <ul>
   *   <li>Does not change {@code this}.
   *   <li>Does not change {@code other}.
   *   <li>Returns a fresh object which is not aliased yet.
   *   <li>Returns an object of the same (dynamic) type as {@code this}, even if the signature is
   *       more permissive.
   *   <li>Is commutative.
   * </ul>
   *
   * @param other another value
   * @return the greatest lower bound of two values
   */
  public V greatestLowerBound(@Nullable V other) {
    if (other == null) {
      @SuppressWarnings("unchecked")
      V v = (V) this;
      return v;
    }
    ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
    TypeMirror glbTypeMirror =
        TypesUtils.greatestLowerBound(
            this.getUnderlyingType(), other.getUnderlyingType(), processingEnv);

    ValueGlb valueGlb = new ValueGlb();
    AnnotationMirrorSet glb =
        valueGlb.combineSets(
            this.getUnderlyingType(),
            this.getAnnotations(),
            other.getUnderlyingType(),
            other.getAnnotations(),
            canBeMissingAnnotations(glbTypeMirror));
    return analysis.createAbstractValue(glb, glbTypeMirror);
  }

  /**
   * Computes the GLB of two sets of annotations. The computation accounts for sets that are missing
   * annotations in hierarchies.
   */
  protected class ValueGlb extends AnnotationSetCombiner {

    @Override
    protected @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a,
        TypeMirror aTypeMirror,
        AnnotationMirror b,
        TypeMirror bTypeMirror,
        AnnotationMirror top) {
      return qualHierarchy.greatestLowerBoundShallow(a, aTypeMirror, b, bTypeMirror);
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoTypeVars(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      if (canCombinedSetBeMissingAnnos) {
        // don't add an annotation
        return null;
      } else {
        AnnotationMirror aUB = aAtv.getEffectiveAnnotationInHierarchy(top);
        AnnotationMirror bUB = bAtv.getEffectiveAnnotationInHierarchy(top);
        TypeMirror aTM = aAtv.getUnderlyingType();
        TypeMirror bTM = bAtv.getUnderlyingType();
        return combineTwoAnnotations(aUB, aTM, bUB, bTM, top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineAnnotationWithTypeVar(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      TypeMirror typeVarTM = typeVar.getUnderlyingType();
      if (canCombinedSetBeMissingAnnos) {
        // anno is the primary annotation on the use of a type variable. typeVar is a use of
        // the same type variable that does not have a primary annotation. The glb of the
        // two type variables is computed as follows. If anno is a supertype (or equal) to
        // the annotation on the upper bound of typeVar, then typeVar is the glb, so no
        // annotation is added to glbset.
        // If anno is a subtype of the annotation on the upper bound of typeVar, then the
        // glb is typeVar with a primary annotation of glb(anno, lowerBound), where
        // lowerBound is the annotation on the lower bound of typeVar.
        AnnotationMirror upperBound = typeVar.getEffectiveAnnotationInHierarchy(top);
        if (qualHierarchy.isSubtypeQualifiersOnly(upperBound, annotation)) {
          return null;
        } else {
          AnnotationMirrorSet lBSet =
              AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, typeVar);
          AnnotationMirror lowerBound = qualHierarchy.findAnnotationInHierarchy(lBSet, top);
          return combineTwoAnnotations(annotation, typeVarTM, lowerBound, typeVarTM, top);
        }
      } else {
        return combineTwoAnnotations(
            annotation, typeVarTM, typeVar.getEffectiveAnnotationInHierarchy(top), typeVarTM, top);
      }
    }
  }

  /**
   * Combines two sets of AnnotationMirrors by hierarchy.
   *
   * <p>Subclasses must define how to combine sets by implementing the following methods:
   *
   * <ol>
   *   <li>{@link #combineTwoAnnotations}
   *   <li>{@link #combineAnnotationWithTypeVar}
   *   <li>{@link #combineTwoTypeVars}
   * </ol>
   *
   * If a set is missing an annotation in a hierarchy, and if the combined set can be missing an
   * annotation, then there must be a TypeVariable for the set that can be used to find annotations
   * on its bounds.
   */
  protected abstract class AnnotationSetCombiner {

    /**
     * Combines the two sets.
     *
     * @param aTypeMirror the type mirror associated with {@code aSet}
     * @param aSet a set of annotation mirrors
     * @param bTypeMirror the type mirror associated with {@code bSet}
     * @param bSet a set of annotation mirrors
     * @param canCombinedSetBeMissingAnnos whether or not the combined set can be missing
     *     annotations
     * @return the combined sets
     */
    protected AnnotationMirrorSet combineSets(
        TypeMirror aTypeMirror,
        AnnotationMirrorSet aSet,
        TypeMirror bTypeMirror,
        AnnotationMirrorSet bSet,
        boolean canCombinedSetBeMissingAnnos) {
      if (aTypeMirror == null) {
        throw new NullPointerException("combineSets: aTypeMirror==null");
      }
      if (bTypeMirror == null) {
        throw new NullPointerException("combineSets: bTypeMirror==null");
      }

      AnnotatedTypeVariable aAtv = getEffectiveTypeVar(aTypeMirror);
      AnnotatedTypeVariable bAtv = getEffectiveTypeVar(bTypeMirror);
      AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
      AnnotationMirrorSet combinedSets = new AnnotationMirrorSet();
      for (AnnotationMirror top : tops) {
        AnnotationMirror a = qualHierarchy.findAnnotationInHierarchy(aSet, top);
        AnnotationMirror b = qualHierarchy.findAnnotationInHierarchy(bSet, top);
        AnnotationMirror result;
        if (a != null && b != null) {
          result = combineTwoAnnotations(a, aTypeMirror, b, bTypeMirror, top);
        } else if (a != null) {
          result = combineAnnotationWithTypeVar(a, bAtv, top, canCombinedSetBeMissingAnnos);
        } else if (b != null) {
          result = combineAnnotationWithTypeVar(b, aAtv, top, canCombinedSetBeMissingAnnos);
        } else {
          result = combineTwoTypeVars(aAtv, bAtv, top, canCombinedSetBeMissingAnnos);
        }
        if (result != null) {
          combinedSets.add(result);
        }
      }
      return combinedSets;
    }

    /**
     * Returns the result of combining the two annotations. This method is called when an annotation
     * exists in both sets for the hierarchy whose top is {@code top}.
     *
     * @param a an annotation in the hierarchy
     * @param aTypeMirror the type that is annotated by {@code a}
     * @param b an annotation in the hierarchy
     * @param bTypeMirror the type that is annotated by {@code b}
     * @param top the top annotation in the hierarchy
     * @return the result of combining the two annotations or null if no combination exists
     */
    protected abstract @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a,
        TypeMirror aTypeMirror,
        AnnotationMirror b,
        TypeMirror bTypeMirror,
        AnnotationMirror top);

    /**
     * Returns the primary annotation that result from of combining the two {@link
     * AnnotatedTypeVariable}. If the result has no primary annotation, {@code null} is returned.
     * This method is called when no annotation exists in either sets for the hierarchy whose top is
     * {@code top}.
     *
     * @param aAtv a type variable that does not have a primary annotation in {@code top} hierarchy
     * @param bAtv a type variable that does not have a primary annotation in {@code top} hierarchy
     * @param top the top annotation in the hierarchy
     * @param canCombinedSetBeMissingAnnos whether or not
     * @return the result of combining the two type variables, which may be null
     */
    protected abstract @Nullable AnnotationMirror combineTwoTypeVars(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos);

    /**
     * Returns the result of combining {@code annotation} with {@code typeVar}.
     *
     * <p>This is called when an annotation exists for the hierarchy in one set, but not the other.
     *
     * @param annotation an annotation
     * @param typeVar a type variable that does not have a primary annotation in the hierarchy
     * @param top the top annotation of the hierarchy
     * @param canCombinedSetBeMissingAnnos whether or not
     * @return the result of combining {@code annotation} with {@code typeVar}
     */
    protected abstract @Nullable AnnotationMirror combineAnnotationWithTypeVar(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos);
  }

  /**
   * Returns the AnnotatedTypeVariable associated with the given TypeMirror or null.
   *
   * <p>If {@code typeMirror} is a type variable, then the {@link AnnotatedTypeVariable} of its
   * declaration is returned. If {@code typeMirror} is a wildcard whose extends bounds is a type
   * variable, then the {@link AnnotatedTypeVariable} for its declaration is returned. Otherwise,
   * {@code null} is returned.
   *
   * @param typeMirror a type mirror
   * @return the AnnotatedTypeVariable associated with the given TypeMirror or null
   */
  private @Nullable AnnotatedTypeVariable getEffectiveTypeVar(@Nullable TypeMirror typeMirror) {
    if (typeMirror == null) {
      return null;
    } else if (typeMirror.getKind() == TypeKind.WILDCARD) {
      return getEffectiveTypeVar(((WildcardType) typeMirror).getExtendsBound());

    } else if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      TypeVariable typevar = ((TypeVariable) typeMirror);
      AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(typevar.asElement());
      return (AnnotatedTypeVariable) atm;
    } else {
      return null;
    }
  }
}
