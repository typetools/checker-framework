package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/** An inference variable. */
public class Variable extends AbstractType {

  protected final VariableBounds variableBounds;
  /** Identification number. Used only to make debugging easier. */
  protected final int id;

  /**
   * The expression for which this variable is being solved. Used to differentiate inference
   * variables for two different invocations of the same method.
   */
  protected final ExpressionTree invocation;

  /** Type variable for which the instantiation of this variable is a type argument, */
  protected final TypeVariable typeVariableJava;

  /** Type variable for which the instantiation of this variable is a type argument, */
  protected final AnnotatedTypeVariable typeVariable;

  protected final Theta map;

  Variable(
      AnnotatedTypeVariable typeVariable,
      TypeVariable typeVariableJava,
      ExpressionTree invocation,
      Java8InferenceContext context,
      Theta map) {
    this(typeVariable, typeVariableJava, invocation, context, map, context.getNextVariableId());
  }

  Variable(
      AnnotatedTypeVariable typeVariable,
      TypeVariable typeVariableJava,
      ExpressionTree invocation,
      Java8InferenceContext context,
      Theta map,
      int id) {
    super(context);
    assert typeVariable != null;
    this.variableBounds = new VariableBounds(context);
    this.typeVariableJava = typeVariableJava;
    this.typeVariable = typeVariable;
    this.invocation = invocation;
    this.map = map;
    this.id = id;
  }

  /**
   * REturn this variable's current bounds.
   *
   * @return this variable's current bounds
   */
  public VariableBounds getBounds() {
    return variableBounds;
  }

  /**
   * Adds the initial bounds to this variable. These are the bounds implied by the upper bounds of
   * the type variable. See end of JLS 18.1.3.
   *
   * @param map used to determine if the bounds refer to another variable
   */
  public void initialBounds(Theta map) {
    TypeMirror upperBound = typeVariableJava.getUpperBound();
    // If Pl has no TypeBound, the bound {@literal al <: Object} appears in the set. Otherwise,
    // for each type T delimited by & in the TypeBound, the bound {@literal al <: T[P1:=a1,...,
    // Pp:=ap]} appears in the set; if this results in no proper upper bounds for al (only
    // dependencies), then the bound {@literal al <: Object} also appears in the set.
    switch (upperBound.getKind()) {
      case INTERSECTION:
        Iterator<? extends TypeMirror> iter =
            ((IntersectionType) upperBound).getBounds().iterator();
        for (AnnotatedTypeMirror bound : typeVariable.getUpperBound().directSupertypes()) {
          AbstractType t1 = InferenceType.create(bound, iter.next(), map, context);
          variableBounds.addBound(BoundKind.UPPER, t1);
        }
        break;
      default:
        AbstractType t1 =
            InferenceType.create(typeVariable.getUpperBound(), upperBound, map, context);
        variableBounds.addBound(BoundKind.UPPER, t1);
        break;
    }
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type) {
    return InferenceType.create(atm, type, map, context);
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
  public Variable capture(Java8InferenceContext context) {
    return this;
  }

  @Override
  public Variable getErased() {
    return this;
  }

  @Override
  public TypeVariable getJavaType() {
    return typeVariableJava;
  }

  @Override
  public AnnotatedTypeVariable getAnnotatedType() {
    return typeVariable;
  }

  public ExpressionTree getInvocation() {
    return invocation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Variable variable = (Variable) o;
    return context.modelTypes.isSameType(typeVariableJava, variable.typeVariableJava)
        && invocation == variable.invocation;
  }

  @Override
  public int hashCode() {
    int result = typeVariableJava.toString().hashCode();
    result = 31 * result + Kind.VARIABLE.hashCode();
    result = 31 * result + invocation.hashCode();
    return result;
  }

  @Override
  public Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public Collection<Variable> getInferenceVariables() {
    return Collections.singleton(this);
  }

  @Override
  public AbstractType applyInstantiations(List<Variable> instantiations) {
    for (Variable inst : instantiations) {
      if (inst.equals(this)) {
        return inst.getBounds().getInstantiation();
      }
    }
    return this;
  }

  @Override
  public String toString() {
    if (variableBounds.hasInstantiation()) {
      return "a" + id + " := " + variableBounds.getInstantiation();
    }
    return "a" + id;
  }

  /** in case the first attempt at resolution fails. */
  public void save() {
    variableBounds.save();
  }

  /**
   * Restore the bounds to the state previously saved. This method is called if the first attempt at
   * resolution fails.
   */
  public void restore() {
    variableBounds.restore();
  }

  /**
   * Returns whether or not this variable was created for a capture bound.
   *
   * @return whether or not this variable was created for a capture bound
   */
  public boolean isCaptureVariable() {
    return false;
  }
}
