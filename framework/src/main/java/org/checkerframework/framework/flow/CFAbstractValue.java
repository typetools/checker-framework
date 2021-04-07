package org.checkerframework.framework.flow;

import java.util.Objects;
import java.util.Set;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
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
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements AbstractValue<V> {

  /** The analysis class this value belongs to. */
  protected final CFAbstractAnalysis<V, ?, ?> analysis;

  /** The underlying (Java) type in this abstract value. */
  protected final TypeMirror underlyingType;
  /** The annotations in this abstract value. */
  protected final Set<AnnotationMirror> annotations;

  /**
   * Creates a new CFAbstractValue.
   *
   * @param analysis the analysis class this value belongs to
   * @param annotations the annotations in this abstract value
   * @param underlyingType the underlying (Java) type in this abstract value
   */
  protected CFAbstractValue(
      CFAbstractAnalysis<V, ?, ?> analysis,
      Set<AnnotationMirror> annotations,
      TypeMirror underlyingType) {
    this.analysis = analysis;
    this.annotations = annotations;
    this.underlyingType = underlyingType;

    assert validateSet(
            this.getAnnotations(),
            this.getUnderlyingType(),
            analysis.getTypeFactory().getQualifierHierarchy())
        : "Encountered invalid type: "
            + underlyingType
            + " annotations: "
            + StringsPlume.join(", ", annotations);
  }

  /**
   * Returns true if the set has an annotation from every hierarchy (or if it doesn't need to);
   * returns false if the set is missing an annotation from some hierarchy.
   *
   * @param annos set of annotations
   * @param typeMirror where the annotations are written
   * @param hierarchy the qualifier hierarchy
   * @return true if no annotations are missing
   */
  public static boolean validateSet(
      Set<AnnotationMirror> annos, TypeMirror typeMirror, QualifierHierarchy hierarchy) {

    if (canBeMissingAnnotations(typeMirror)) {
      return true;
    }

    Set<AnnotationMirror> missingHierarchy = null;
    for (AnnotationMirror top : hierarchy.getTopAnnotations()) {
      AnnotationMirror anno = hierarchy.findAnnotationInHierarchy(annos, top);
      if (anno == null) {
        if (missingHierarchy == null) {
          missingHierarchy = AnnotationUtils.createAnnotationSet();
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
  public Set<AnnotationMirror> getAnnotations() {
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
   * Returns the more specific version of two values {@code this} and {@code other}. If they do not
   * contain information for all hierarchies, then it is possible that information from both {@code
   * this} and {@code other} are taken.
   *
   * <p>If neither of the two is more specific for one of the hierarchies (i.e., if the two are
   * incomparable as determined by {@link QualifierHierarchy#isSubtype(AnnotationMirror,
   * AnnotationMirror)}, then the respective value from {@code backup} is used.
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

    MostSpecificVisitor ms =
        new MostSpecificVisitor(this.getUnderlyingType(), other.getUnderlyingType(), backup);
    Set<AnnotationMirror> mostSpecific =
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
    final Set<AnnotationMirror> backupSet;

    /** TypeMirror for the "a" value. */
    final TypeMirror aTypeMirror;

    /** TypeMirror for the "b" value. */
    final TypeMirror bTypeMirror;

    /**
     * Create a {@link MostSpecificVisitor}.
     *
     * @param aTypeMirror type of the "a" value
     * @param bTypeMirror type of the "b" value
     * @param backup value to use if no most specific value is found
     */
    public MostSpecificVisitor(TypeMirror aTypeMirror, TypeMirror bTypeMirror, V backup) {
      this.aTypeMirror = aTypeMirror;
      this.bTypeMirror = bTypeMirror;
      if (backup != null) {
        this.backupSet = backup.getAnnotations();
        // this.backupTypeMirror = backup.getUnderlyingType();
        // this.backupAtv = getEffectTypeVar(backupTypeMirror);
      } else {
        // this.backupAtv = null;
        // this.backupTypeMirror = null;
        this.backupSet = null;
      }
    }

    private AnnotationMirror getBackUpAnnoIn(AnnotationMirror top) {
      if (backupSet == null) {
        // If there is no back up value, but one is required then the resulting set will
        // not be the most specific.  Indicate this with the error.
        error = true;
        return null;
      }
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      return hierarchy.findAnnotationInHierarchy(backupSet, top);
    }

    @Override
    protected @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      if (analysis
              .getTypeFactory()
              .hasQualifierParameterInHierarchy(TypesUtils.getTypeElement(aTypeMirror), top)
          && analysis
              .getTypeFactory()
              .hasQualifierParameterInHierarchy(TypesUtils.getTypeElement(bTypeMirror), top)) {
        // Both types have qualifier parameters, so the annotations must be exact.
        if (hierarchy.isSubtype(a, b) && hierarchy.isSubtype(b, a)) {
          return b;
        }
      } else if (hierarchy.isSubtype(a, b)) {
        return a;
      } else if (hierarchy.isSubtype(b, a)) {
        return b;
      }
      return getBackUpAnnoIn(top);
    }

    @Override
    protected @Nullable AnnotationMirror combineNoAnnotations(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      if (canCombinedSetBeMissingAnnos) {
        return null;
      } else {
        AnnotationMirror aUB = aAtv.getEffectiveAnnotationInHierarchy(top);
        AnnotationMirror bUB = bAtv.getEffectiveAnnotationInHierarchy(top);
        return combineTwoAnnotations(aUB, bUB, top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineOneAnnotation(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {

      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      AnnotationMirror upperBound = typeVar.getEffectiveAnnotationInHierarchy(top);

      if (!canCombinedSetBeMissingAnnos) {
        return combineTwoAnnotations(annotation, upperBound, top);
      }
      Set<AnnotationMirror> lBSet =
          AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, typeVar);
      AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
      if (hierarchy.isSubtype(upperBound, annotation)) {
        // no anno is more specific than anno
        return null;
      } else if (hierarchy.isSubtype(annotation, lowerBound)) {
        return annotation;
      } else {
        return getBackUpAnnoIn(top);
      }
    }
  }

  @Override
  public V leastUpperBound(@Nullable V other) {
    return upperBound(other, false);
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
   * @param previous must be the previous value
   * @return an upper bound of two values that is wider than the least upper bound of the two values
   */
  public V widenUpperBound(@Nullable V previous) {
    return upperBound(previous, true);
  }

  private V upperBound(@Nullable V other, boolean shouldWiden) {
    if (other == null) {
      @SuppressWarnings("unchecked")
      V v = (V) this;
      return v;
    }
    ProcessingEnvironment processingEnv = analysis.getTypeFactory().getProcessingEnv();
    TypeMirror lubTypeMirror =
        TypesUtils.leastUpperBound(
            this.getUnderlyingType(), other.getUnderlyingType(), processingEnv);

    ValueLub valueLub = new ValueLub(shouldWiden);
    Set<AnnotationMirror> lub =
        valueLub.combineSets(
            this.getUnderlyingType(),
            this.getAnnotations(),
            other.getUnderlyingType(),
            other.getAnnotations(),
            canBeMissingAnnotations(lubTypeMirror));
    return analysis.createAbstractValue(lub, lubTypeMirror);
  }

  /**
   * Computes the least upper bound or, if {@code shouldWiden} is true, an upper bounds of two sets
   * of annotations. The computation accounts for sets that are missing annotations in hierarchies.
   */
  protected class ValueLub extends AnnotationSetCombiner {
    boolean widen;

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
        AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      if (widen) {
        return hierarchy.widenedUpperBound(a, b);
      } else {
        return hierarchy.leastUpperBound(a, b);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineNoAnnotations(
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
        return combineTwoAnnotations(aUB, bUB, top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineOneAnnotation(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      if (canCombinedSetBeMissingAnnos) {
        // anno is the primary annotation on the use of a type variable. typeVar is a use of the
        // same type variable that does not have a primary annotation. The lub of the two type
        // variables is computed as follows. If anno is a subtype (or equal) to the annotation on
        // the lower bound of typeVar, then typeVar is the lub, so no annotation is added to lubset.
        // If anno is a supertype of the annotation on the lower bound of typeVar, then the lub is
        // typeVar with a primary annotation of lub(anno, upperBound), where upperBound is the
        // annotation on the upper bound of typeVar.
        Set<AnnotationMirror> lBSet =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, typeVar);
        AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
        if (hierarchy.isSubtype(annotation, lowerBound)) {
          return null;
        } else {
          return combineTwoAnnotations(
              annotation, typeVar.getEffectiveAnnotationInHierarchy(top), top);
        }
      } else {
        return combineTwoAnnotations(
            annotation, typeVar.getEffectiveAnnotationInHierarchy(top), top);
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
    ProcessingEnvironment processingEnv = analysis.getTypeFactory().getProcessingEnv();
    TypeMirror glbTypeMirror =
        TypesUtils.greatestLowerBound(
            this.getUnderlyingType(), other.getUnderlyingType(), processingEnv);

    ValueGlb valueGlb = new ValueGlb();
    Set<AnnotationMirror> glb =
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
        AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      return hierarchy.greatestLowerBound(a, b);
    }

    @Override
    protected @Nullable AnnotationMirror combineNoAnnotations(
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
        return combineTwoAnnotations(aUB, bUB, top);
      }
    }

    @Override
    protected @Nullable AnnotationMirror combineOneAnnotation(
        AnnotationMirror annotation,
        AnnotatedTypeVariable typeVar,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos) {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      if (canCombinedSetBeMissingAnnos) {
        // anno is the primary annotation on the use of a type variable. typeVar is a use of the
        // same type variable that does not have a primary annotation. The glb of the two type
        // variables is computed as follows. If anno is a supertype (or equal) to the annotation on
        // the upper bound of typeVar, then typeVar is the glb, so no annotation is added to glbset.
        // If anno is a subtype of the annotation on the upper bound of typeVar, then the glb is
        // typeVar with a primary annotation of glb(anno, lowerBound), where lowerBound is the
        // annotation on the lower bound of typeVar.
        AnnotationMirror upperBound = typeVar.getEffectiveAnnotationInHierarchy(top);
        if (hierarchy.isSubtype(upperBound, annotation)) {
          return null;
        } else {
          Set<AnnotationMirror> lBSet =
              AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, typeVar);
          AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
          return combineTwoAnnotations(annotation, lowerBound, top);
        }
      } else {
        return combineTwoAnnotations(
            annotation, typeVar.getEffectiveAnnotationInHierarchy(top), top);
      }
    }
  }

  /**
   * Combines two sets of AnnotationMirrors by hierarchy.
   *
   * <p>Subclasses must define how to combine sets by implementing the following methods:
   *
   * <ol>
   *   <li>{@link #combineTwoAnnotations(AnnotationMirror, AnnotationMirror, AnnotationMirror)}
   *   <li>{@link #combineOneAnnotation(AnnotationMirror, AnnotatedTypeVariable, AnnotationMirror,
   *       boolean)}
   *   <li>{@link #combineNoAnnotations(AnnotatedTypeVariable, AnnotatedTypeVariable,
   *       AnnotationMirror, boolean)}
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
    protected Set<AnnotationMirror> combineSets(
        TypeMirror aTypeMirror,
        Set<AnnotationMirror> aSet,
        TypeMirror bTypeMirror,
        Set<AnnotationMirror> bSet,
        boolean canCombinedSetBeMissingAnnos) {

      AnnotatedTypeVariable aAtv = getEffectTypeVar(aTypeMirror);
      AnnotatedTypeVariable bAtv = getEffectTypeVar(bTypeMirror);
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      Set<? extends AnnotationMirror> tops = hierarchy.getTopAnnotations();
      Set<AnnotationMirror> combinedSets = AnnotationUtils.createAnnotationSet();
      for (AnnotationMirror top : tops) {
        AnnotationMirror a = hierarchy.findAnnotationInHierarchy(aSet, top);
        AnnotationMirror b = hierarchy.findAnnotationInHierarchy(bSet, top);
        AnnotationMirror result;
        if (a != null && b != null) {
          result = combineTwoAnnotations(a, b, top);
        } else if (a != null) {
          result = combineOneAnnotation(a, bAtv, top, canCombinedSetBeMissingAnnos);
        } else if (b != null) {
          result = combineOneAnnotation(b, aAtv, top, canCombinedSetBeMissingAnnos);
        } else {
          result = combineNoAnnotations(aAtv, bAtv, top, canCombinedSetBeMissingAnnos);
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
     * @param b an annotation in the hierarchy
     * @param top the top annotation in the hierarchy
     * @return the result of combining the two annotations or null if no combination exists
     */
    protected abstract @Nullable AnnotationMirror combineTwoAnnotations(
        AnnotationMirror a, AnnotationMirror b, AnnotationMirror top);

    /**
     * Returns the primary annotation that result from of combining the two {@link
     * AnnotatedTypeVariable}. If the result has not primary annotation, the {@code null} is
     * returned. This method is called when no annotation exists in either sets for the hierarchy
     * whose top is {@code top}.
     *
     * @param aAtv a type variable that does not have a primary annotation in {@code top} hierarchy
     * @param bAtv a type variable that does not have a primary annotation in {@code top} hierarchy
     * @param top the top annotation in the hierarchy
     * @param canCombinedSetBeMissingAnnos whether or not
     * @return the result of combining the two type variables, which may be null
     */
    protected abstract @Nullable AnnotationMirror combineNoAnnotations(
        AnnotatedTypeVariable aAtv,
        AnnotatedTypeVariable bAtv,
        AnnotationMirror top,
        boolean canCombinedSetBeMissingAnnos);

    /**
     * Returns the result of combining {@code annotation} with {@code typeVar}.
     *
     * <p>This is called when an annotation exists for the hierarchy in on set, but not the other.
     *
     * @param annotation an annotation
     * @param typeVar a type variable that does not have a primary annotation in the hierarchy
     * @param top the top annotation of the hierarchy
     * @param canCombinedSetBeMissingAnnos whether or not
     * @return the result of combining {@code annotation} with {@code typeVar}
     */
    protected abstract @Nullable AnnotationMirror combineOneAnnotation(
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
  private @Nullable AnnotatedTypeVariable getEffectTypeVar(@Nullable TypeMirror typeMirror) {
    if (typeMirror == null) {
      return null;
    } else if (typeMirror.getKind() == TypeKind.WILDCARD) {
      return getEffectTypeVar(((WildcardType) typeMirror).getExtendsBound());

    } else if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      TypeVariable typevar = ((TypeVariable) typeMirror);
      AnnotatedTypeMirror atm = analysis.getTypeFactory().getAnnotatedType(typevar.asElement());
      return (AnnotatedTypeVariable) atm;
    } else {
      return null;
    }
  }
}
