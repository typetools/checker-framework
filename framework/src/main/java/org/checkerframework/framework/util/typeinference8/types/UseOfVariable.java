package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * A use of an inference variable. This class keeps track of whether the use of this variable has a
 * primary annotation.
 */
public class UseOfVariable extends AbstractType {

  /** The variable that this is a use of. */
  private final Variable variable;

  /** True if this use has a primary annotation. */
  private final boolean hasPrimaryAnno;

  /** The bottom annotations for each hierarchy that has a primary annotation on this use. */
  private final Set<AnnotationMirror> bots;

  /** The top annotations for each hierarchy that has a primary annotation on this use. */
  private final Set<AnnotationMirror> tops;

  /** The annotated type variable for this use. */
  private final AnnotatedTypeVariable type;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  private final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates a use of a variable.
   *
   * @param type annotated type variable for this use
   * @param variable variable that this is a use of
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   */
  public UseOfVariable(
      AnnotatedTypeVariable type,
      Variable variable,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    super(context, ignoreAnnotations);
    QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
    this.qualifierVars = qualifierVars;
    this.variable = variable;
    this.type = type.deepCopy();
    this.hasPrimaryAnno = !type.getPrimaryAnnotations().isEmpty();
    this.bots = new AnnotationMirrorSet();
    this.tops = new AnnotationMirrorSet();
    if (hasPrimaryAnno) {
      for (AnnotationMirror anno : type.getPrimaryAnnotations()) {
        bots.add(qh.getBottomAnnotation(anno));
        tops.add(qh.getTopAnnotation(anno));
      }
    }
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror atm, boolean ignoreAnnotations) {
    return InferenceType.create(atm, variable.map, qualifierVars, context, ignoreAnnotations);
  }

  @Override
  public boolean isObject() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * <p>An inference variable is not a declared type, so it has no type parameters; this
   * implementation returns null.
   *
   * @return null, because this type is a use of an inference variable
   */
  @Override
  public @Nullable List<ProperType> getTypeParameterBounds() {
    return null;
  }

  @Override
  public UseOfVariable capture(Java8InferenceContext context) {
    return this;
  }

  @Override
  public UseOfVariable getErased() {
    return this;
  }

  @Override
  public TypeVariable getJavaType() {
    return variable.typeVariable.getUnderlyingType();
  }

  @Override
  public AnnotatedTypeVariable getAnnotatedType() {
    return type;
  }

  @Override
  public Kind getKind() {
    return Kind.USE_OF_VARIABLE;
  }

  @Override
  public Collection<Variable> getInferenceVariables() {
    return Collections.singleton(variable);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the variable is instantiated, then the result is its instantiation, except that each
   * primary annotation on this use replaces the instantiation's annotation in the same hierarchy. A
   * polymorphic qualifier is not copied, because a proper type's annotations are compared as
   * written, so a polymorphic qualifier on one would be treated as a concrete qualifier rather than
   * as its {@link QualifierVar}.
   *
   * <p>The result ignores annotations if this use does, or if the instantiation does and the
   * annotations copied from this use do not cover every qualifier hierarchy.
   */
  @Override
  public AbstractType applyInstantiations() {
    ProperType instantiation = variable.getInstantiation();
    if (instantiation == null) {
      return this;
    }
    QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
    AnnotationMirrorSet annosToCopy = new AnnotationMirrorSet();
    for (AnnotationMirror anno : type.getPrimaryAnnotations()) {
      if (!qh.isPolymorphicQualifier(anno)) {
        annosToCopy.add(anno);
      }
    }
    boolean someHierarchyIgnored =
        instantiation.ignoreAnnotations && annosToCopy.size() < qh.getTopAnnotations().size();
    boolean ignore = ignoreAnnotations || someHierarchyIgnored;
    if (annosToCopy.isEmpty() && ignore == instantiation.ignoreAnnotations) {
      return instantiation;
    }
    // Copy, because the instantiation is stored as a bound of `variable`.
    AnnotatedTypeMirror atm = instantiation.getAnnotatedType().deepCopy();
    atm.replaceAnnotations(annosToCopy);
    // Only the root annotations change, so the result keeps the instantiation's record of its
    // ignored substitutions; see ProperType#origin.
    return instantiation.withAnnotatedType(atm, ignore);
  }

  /**
   * Returns the variable that this is a use of.
   *
   * @return the variable that this is a use of
   */
  public Variable getVariable() {
    return variable;
  }

  /**
   * Set whether this use has a throws bound.
   *
   * @param hasThrowsBound true if this use has a throws bound
   */
  public void setHasThrowsBound(boolean hasThrowsBound) {
    variable.getBounds().setHasThrowsBound(hasThrowsBound);
  }

  /**
   * Adds a qualifier bound for this variable, if this use's annotations are the variable's: that
   * is, if this use has no primary annotation and does not ignore annotations.
   *
   * @param kind the kind of bound
   * @param annotations the qualifiers to add
   */
  public void addQualifierBound(BoundKind kind, Set<AbstractQualifier> annotations) {
    if (!hasPrimaryAnno && !ignoreAnnotations) {
      variable.getBounds().addQualifierBound(kind, annotations);
    }
  }

  /**
   * Returns a copy of {@code bound} that ignores its root annotations, and whose annotated type is
   * a deep copy that may be mutated. A proper type keeps its record of its ignored substitutions;
   * see {@link ProperType#getOrigin}.
   *
   * @param bound a type
   * @return a copy of {@code bound} that ignores its root annotations
   */
  private static AbstractType copyIgnoringRoot(AbstractType bound) {
    AnnotatedTypeMirror atm = bound.getAnnotatedType().deepCopy();
    if (bound instanceof ProperType properBound) {
      return properBound.withAnnotatedType(atm, true);
    }
    return bound.create(atm, true);
  }

  /**
   * Adds a bound for this variable. If this use has a primary annotation, or ignores annotations,
   * then the bound says nothing about the variable's annotations, so it ignores annotations.
   *
   * @param parent the constraint whose reduction created this bound
   * @param kind the kind of bound
   * @param bound the type of the bound
   */
  public void addBound(Constraint parent, BoundKind kind, AbstractType bound) {
    if (!hasPrimaryAnno && !ignoreAnnotations) {
      variable.getBounds().addBound(parent, kind, bound);
    } else if (!hasPrimaryAnno) {
      // This use ignores annotations: it is, for example, the lower bound `U` of a variable `T`
      // that comes from the formula `U <: @Nullable T`.  Its relation to `bound` is a relation of
      // Java types only, so the bound ignores its root annotations.  As in the next case, a lower
      // or upper bound gets bottom or top root annotations, so that it does not narrow the
      // variable's instantiation.  An EQUAL bound gives only the variable's Java type; see
      // AbstractType#ignoreAnnotations.
      AbstractType boundCopy = copyIgnoringRoot(bound);
      QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
      if (kind == BoundKind.LOWER) {
        boundCopy.getAnnotatedType().replaceAnnotations(qh.getBottomAnnotations());
      } else if (kind == BoundKind.UPPER) {
        boundCopy.getAnnotatedType().replaceAnnotations(qh.getTopAnnotations());
      }
      variable.getBounds().addBound(parent, kind, boundCopy);
    } else {
      // If the use has a primary annotation, then mark the bound so that the annotations will be
      // ignored. Also, set to bottom or top, unless the bound is a type variable. This way if all
      // the bounds of a variable have annotations to be ignored, the instantiation of that variable
      // is as flexible as possible.
      AbstractType boundCopy = copyIgnoringRoot(bound);
      // `create` may copy its argument rather than storing it (`UseOfVariable`'s constructor
      // deep-copies, and `InferenceType`'s calls `asUse()`), so mutate the annotated type that
      // `boundCopy` actually holds.  It is already a fresh copy, so mutating it is safe.
      AnnotatedTypeMirror boundCopyATM = boundCopy.getAnnotatedType();
      if (boundCopyATM.getKind() == TypeKind.TYPEVAR && kind == BoundKind.EQUAL) {
        variable.getBounds().addBound(parent, kind, boundCopy);
      } else if (kind == BoundKind.LOWER) {
        boundCopyATM.replaceAnnotations(bots);
        variable.getBounds().addBound(parent, kind, boundCopy);
      } else if (kind == BoundKind.UPPER) {
        boundCopyATM.replaceAnnotations(tops);
        variable.getBounds().addBound(parent, kind, boundCopy);
      } else {
        boundCopyATM.replaceAnnotations(tops);
        variable.getBounds().addBound(parent, BoundKind.UPPER, boundCopy);

        AbstractType boundCopy2 = copyIgnoringRoot(bound);
        AnnotatedTypeMirror boundCopyATM2 = boundCopy2.getAnnotatedType();
        boundCopyATM2.replaceAnnotations(bots);
        variable.getBounds().addBound(parent, BoundKind.LOWER, boundCopy2);
      }
    }
  }

  @Override
  public Set<AbstractQualifier> getQualifiers() {
    if (hasPrimaryAnno) {
      return AbstractQualifier.create(
          getAnnotatedType().getPrimaryAnnotations(), qualifierVars, context);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public String toString() {
    return "use of " + variable + (hasPrimaryAnno ? " with primary" : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UseOfVariable that = (UseOfVariable) o;

    if (!sameInferenceProblem(that)) {
      return false;
    }
    if (hasPrimaryAnno != that.hasPrimaryAnno) {
      return false;
    }
    if (variable != that.variable) {
      return false;
    }
    if (!bots.equals(that.bots)) {
      return false;
    }
    if (!tops.equals(that.tops)) {
      return false;
    }
    // Two types with different qualifierVars have different qualifiers, as getQualifiers() shows.
    if (!qualifierVars.equals(that.qualifierVars)) {
      return false;
    }

    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    // This is Objects.hash() expanded, to avoid allocating an array and boxing.  This method is
    // hot: inference puts these types in hash sets and rebuilds those sets repeatedly.
    int result = 31 + inferenceProblemHashCode();
    result = 31 * result + Objects.hashCode(variable);
    result = 31 * result + Boolean.hashCode(hasPrimaryAnno);
    result = 31 * result + Objects.hashCode(bots);
    result = 31 * result + Objects.hashCode(tops);
    result = 31 * result + Objects.hashCode(qualifierVars);
    return 31 * result + Objects.hashCode(type);
  }
}
