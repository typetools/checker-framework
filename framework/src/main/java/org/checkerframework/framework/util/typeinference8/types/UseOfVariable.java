package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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

  /** Whether this use has a primary annotation. */
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
   */
  public UseOfVariable(
      AnnotatedTypeVariable type,
      Variable variable,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context) {
    super(context);
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
  public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type) {
    return InferenceType.create(atm, type, variable.map, qualifierVars, context);
  }

  @Override
  public boolean isObject() {
    return false;
  }

  @Override
  public List<ProperType> getTypeParameterBounds() {
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

  @Override
  public AbstractType applyInstantiations() {
    if (this.variable.getInstantiation() != null) {
      return this.variable.getInstantiation();
    }

    return this;
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
   * @param hasThrowsBound whether this use has a throws bound
   */
  public void setHasThrowsBound(boolean hasThrowsBound) {
    variable.getBounds().setHasThrowsBound(hasThrowsBound);
  }

  /**
   * Adds a qualifier bound for this variable, is this use does not have a primary annotation.
   *
   * @param kind the kind of bound
   * @param annotations the qualifiers to add
   */
  public void addQualifierBound(BoundKind kind, Set<AbstractQualifier> annotations) {
    if (!hasPrimaryAnno) {
      variable.getBounds().addQualifierBound(kind, annotations);
    }
  }

  /**
   * Adds a bound for this variable, is this use does not have a primary annotation.
   *
   * @param parent the constraint whose reduction created this bound
   * @param kind the kind of bound
   * @param bound the type of the bound
   */
  public void addBound(Constraint parent, BoundKind kind, AbstractType bound) {
    if (!hasPrimaryAnno) {
      variable.getBounds().addBound(parent, kind, bound);
    } else {
      // If the use has a primary annotation, then add the bound but with that annotations
      // set to bottom or top.  This makes it so that the java type is still a bound, but
      // the qualifiers do not change the results of inference.
      if (kind == BoundKind.LOWER) {
        bound.getAnnotatedType().replaceAnnotations(bots);
        variable.getBounds().addBound(parent, kind, bound);
      } else if (kind == BoundKind.UPPER) {
        bound.getAnnotatedType().replaceAnnotations(tops);
        variable.getBounds().addBound(parent, kind, bound);
      } else {
        AnnotatedTypeMirror copyATM = bound.getAnnotatedType().deepCopy();
        AbstractType boundCopy = bound.create(copyATM, bound.getJavaType());

        bound.getAnnotatedType().replaceAnnotations(tops);
        variable.getBounds().addBound(parent, BoundKind.UPPER, bound);

        boundCopy.getAnnotatedType().replaceAnnotations(bots);
        variable.getBounds().addBound(parent, BoundKind.LOWER, boundCopy);
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
    if (!super.equals(o)) {
      return false;
    }

    UseOfVariable that = (UseOfVariable) o;

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

    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + variable.hashCode();
    result = 31 * result + (hasPrimaryAnno ? 1 : 0);
    result = 31 * result + bots.hashCode();
    result = 31 * result + tops.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}
