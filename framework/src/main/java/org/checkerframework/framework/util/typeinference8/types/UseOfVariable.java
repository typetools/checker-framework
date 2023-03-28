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
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class UseOfVariable extends AbstractType {
  private final Variable variable;
  private final boolean hasPrimaryAnno;
  private final Set<AnnotationMirror> bots;
  private final Set<AnnotationMirror> tops;
  private final AnnotatedTypeVariable type;

  public UseOfVariable(
      AnnotatedTypeVariable type, Variable variable, Java8InferenceContext context) {
    super(context);
    QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
    this.variable = variable;
    this.type = type;
    this.hasPrimaryAnno = !type.getAnnotations().isEmpty();
    this.bots = new AnnotationMirrorSet();
    this.tops = new AnnotationMirrorSet();
    if (hasPrimaryAnno) {
      for (AnnotationMirror anno : type.getAnnotations()) {
        bots.add(qh.getBottomAnnotation(anno));
        tops.add(qh.getTopAnnotation(anno));
      }
    }
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type) {
    return InferenceType.create(atm, type, variable.map, context);
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
    return variable.typeVariableJava;
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
  public AbstractType applyInstantiations(List<Variable> instantiations) {
    for (Variable inst : instantiations) {
      if (inst.equals(this.variable)) {
        return inst.getBounds().getInstantiation();
      }
    }
    return this;
  }

  public Variable getVariable() {
    return variable;
  }

  public void setHasThrowsBound(boolean hasThrowsBound) {
    variable.getBounds().setHasThrowsBound(hasThrowsBound);
  }

  public void addQualifierBound(BoundKind kind, Set<AnnotationMirror> annotations) {
    if (!hasPrimaryAnno) {
      variable.getBounds().addQualifierBound(kind, annotations);
    }
  }

  public void addBound(BoundKind kind, AbstractType bound) {
    if (!hasPrimaryAnno) {
      variable.getBounds().addBound(kind, bound);
    } else {
      if (kind == BoundKind.LOWER) {
        bound.getAnnotatedType().replaceAnnotations(bots);
        variable.getBounds().addBound(kind, bound);
      } else if (kind == BoundKind.UPPER) {
        bound.getAnnotatedType().replaceAnnotations(tops);
        variable.getBounds().addBound(kind, bound);
      } else {
        AnnotatedTypeMirror copyATM = bound.getAnnotatedType().deepCopy();
        AbstractType boundCopy = bound.create(copyATM, bound.getJavaType());

        bound.getAnnotatedType().replaceAnnotations(tops);
        variable.getBounds().addBound(BoundKind.UPPER, bound);

        boundCopy.getAnnotatedType().replaceAnnotations(bots);
        variable.getBounds().addBound(BoundKind.LOWER, boundCopy);
      }
    }
  }

  @Override
  public String toString() {
    return "use of " + variable + (hasPrimaryAnno ? "with primary" : "");
  }
}
