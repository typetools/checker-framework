package org.checkerframework.afu.scenelib.el;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An annotated method; contains bounds, return, parameters, receiver, and throws. */
public class AMethod extends ADeclaration {
  /**
   * The method's simple name followed by its erased signature in JVML format. For example, {@code
   * foo()V} or {@code bar(B[I[[Ljava/lang/String;)I}.
   */
  public final String methodSignature;

  /** The type parameters of this method. */
  private /*@Nullable*/ List<? extends TypeParameterElement> typeParameters = null;

  /** The method's annotated type parameter bounds. */
  public final VivifyingMap<BoundLocation, ATypeElement> bounds =
      ATypeElement.<BoundLocation>newVivifyingLHMap_ATE();

  /** The return type of the method, or null if the method's return type is unknown or void. */
  private /*@Nullable*/ TypeMirror returnTypeMirror;

  /** The method's annotated return type. Non-null even if returnTypeMirror is null. */
  public final ATypeElement returnType; // initialized in constructor

  /** The method's annotated receiver parameter type. */
  public final AField receiver; // initialized in constructor

  /** The method's annotated parameters; map key is parameter index, starting at 0. */
  public final VivifyingMap<Integer, AField> parameters = AField.<Integer>newVivifyingLHMap_AF();

  /** Exceptions that are thrown. */
  public final VivifyingMap<TypeIndexLocation, ATypeElement> throwsException =
      ATypeElement.<TypeIndexLocation>newVivifyingLHMap_ATE();

  /**
   * Types of expressions at entry to the method. The map key is the string representation of the
   * expression.
   */
  // TODO: The map value type should probably be ATypeElement instead.
  public final VivifyingMap<String, AField> preconditions = AField.newVivifyingLHMap_AF();

  /**
   * Types of expressions at exit from the method. The map key is the string representation of the
   * expression.
   */
  // TODO: The map value type should probably be ATypeElement instead.
  public final VivifyingMap<String, AField> postconditions = AField.newVivifyingLHMap_AF();

  /**
   * Clients set this before printing the AMethod.
   *
   * <p>These annotations are not stored in tlAnnotationsHere because whole-program inference
   * assumes that inferred annotations only become stronger, but these annotations might disappear
   * as other annotations become stronger.
   *
   * <p>These annotations are not part of the abstract state of this AMethod (but are derived from
   * it).
   */
  public List<Annotation> contracts = Collections.emptyList();

  /** The body of the method. */
  public ABlock body;

  /**
   * Create an AMethod.
   *
   * @param methodSignature the method's name, plus its erased signature in JVML format within
   *     parentheses
   */
  AMethod(String methodSignature) {
    super("method: " + methodSignature);
    this.methodSignature = methodSignature;
    this.returnType = new ATypeElement("return type of " + methodSignature);
    this.receiver = new AField("receiver parameter type of " + methodSignature);
    this.body = new ABlock(methodSignature);
  }

  /**
   * Create a copy of an AMethod.
   *
   * @param other the AMethod to copy
   */
  AMethod(AMethod other) {
    super("method: " + other.methodSignature, other);
    this.methodSignature = other.methodSignature;
    this.typeParameters =
        other.typeParameters == null ? null : new ArrayList<>(other.typeParameters);
    copyMapContents(other.bounds, bounds);
    this.returnTypeMirror = other.returnTypeMirror == null ? null : other.returnTypeMirror;
    this.returnType = other.returnType.clone();
    this.receiver = other.receiver.clone();
    copyMapContents(other.parameters, parameters);
    copyMapContents(other.throwsException, throwsException);
    copyMapContents(other.preconditions, preconditions);
    copyMapContents(other.postconditions, postconditions);
    this.body = other.body.clone();
  }

  /**
   * Sets return type, type parameters, and formal parameters in this AMethod, from information in
   * the given method element.
   *
   * @param methodElt the element whose information to propagate into this
   */
  public void setFieldsFromMethodElement(ExecutableElement methodElt) {
    // TODO: Why doesn't this method set bounds, throwsException, and maybe other fields of this?
    setReturnTypeMirror(methodElt.getReturnType());
    setTypeParameters(methodElt.getTypeParameters());
    vivifyAndAddTypeMirrorToParameters(methodElt);
  }

  /**
   * Returns the method's simple name.
   *
   * @return the method's simple name
   */
  public String getMethodName() {
    return methodSignature.substring(0, methodSignature.indexOf("("));
  }

  /**
   * Get the type parameters of this method.
   *
   * @return the list of type parameters
   */
  public List<? extends TypeParameterElement> getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set the type parameters of this method.
   *
   * @param typeParameters the list of type parameters
   */
  public void setTypeParameters(List<? extends TypeParameterElement> typeParameters) {
    if (typeParameters == null) {
      return;
    }
    if (this.typeParameters != null && !this.typeParameters.equals(typeParameters)) {
      throw new Error(
          String.format(
              "setTypeParameters(%s): already is %s%n", typeParameters, this.typeParameters));
    }
    this.typeParameters = typeParameters;
  }

  /**
   * Populates the method parameter map for the method. Ensures that the method parameter map always
   * has an entry for each parameter.
   *
   * @param methodElt the method whose parameters should be vivified
   */
  private void vivifyAndAddTypeMirrorToParameters(ExecutableElement methodElt) {
    for (int i = 0; i < methodElt.getParameters().size(); i++) {
      VariableElement ve = methodElt.getParameters().get(i);
      TypeMirror type = ve.asType();
      Name name = ve.getSimpleName();
      vivifyAndAddTypeMirrorToParameter(i, type, name);
    }
  }

  /**
   * Obtain the parameter at the given index, which can be further operated on to e.g. add a type
   * annotation.
   *
   * @param i the parameter index (first parameter is zero)
   * @param type the type of the parameter
   * @param simpleName the name of the parameter
   * @return an AFieldWrapper representing the parameter
   */
  public AField vivifyAndAddTypeMirrorToParameter(int i, TypeMirror type, Name simpleName) {
    AField param = parameters.getVivify(i);
    param.setName(simpleName.toString());
    if (param.getTypeMirror() == null) {
      param.setTypeMirror(type);
    }
    return param;
  }

  /**
   * Obtain information about an expression at method entry. It can be further operated on to e.g.
   * add a type annotation.
   *
   * @param expression the expression
   * @param type the type of the expression
   * @return an AField representing the expression
   */
  public AField vivifyAndAddTypeMirrorToPrecondition(String expression, TypeMirror type) {
    AField result = preconditions.getVivify(expression);
    result.setName(expression);
    if (result.getTypeMirror() == null) {
      result.setTypeMirror(type);
    }
    return result;
  }

  // TODO: `type` is ignored if information is already present.  Is that desirable??
  /**
   * Obtain information about an expression at method exit. It can be further operated on to e.g.
   * add a type annotation.
   *
   * @param expression the expression
   * @param type the type of the expression
   * @return an AField representing the expression
   */
  public AField vivifyAndAddTypeMirrorToPostcondition(String expression, TypeMirror type) {
    AField result = postconditions.getVivify(expression);
    result.setName(expression);
    if (result.getTypeMirror() == null) {
      result.setTypeMirror(type);
    }
    return result;
  }

  /**
   * Get the return type.
   *
   * @return the return type, or null if the return type is unknown or void
   */
  public /*@Nullable*/ TypeMirror getReturnTypeMirror() {
    return returnTypeMirror;
  }

  /**
   * Set the return type. Does nothing if the argument is null. Errs if called twice with different
   * non-null arguments.
   *
   * @param returnTypeMirror the return type
   */
  public void setReturnTypeMirror(@Nullable @FindDistinct TypeMirror returnTypeMirror) {
    if (returnTypeMirror == null) {
      return;
    }
    if (this.returnTypeMirror != null && this.returnTypeMirror != returnTypeMirror) {
      throw new Error(
          String.format(
              "setReturnTypeMirror(%s): already is %s%n", returnTypeMirror, this.returnTypeMirror));
    }
    this.returnTypeMirror = returnTypeMirror;
  }

  /**
   * Get the parameters, as a map from parameter index (0-indexed) to representation.
   *
   * @return an immutable copy of the vivified parameters, as a map from index to representation
   */
  public Map<Integer, AField> getParameters() {
    return ImmutableMap.copyOf(parameters);
  }

  /**
   * Get the preconditions: annotations that apply to fields at method entry.
   *
   * @return an immutable copy of the vivified preconditions
   */
  public Map<String, AField> getPreconditions() {
    return ImmutableMap.copyOf(preconditions);
  }

  /**
   * Get the postconditions: annotations that apply to fields at method exit.
   *
   * @return an immutable copy of the vivified postconditions
   */
  public Map<String, AField> getPostconditions() {
    return ImmutableMap.copyOf(postconditions);
  }

  @Override
  public AMethod clone() {
    return new AMethod(this);
  }

  @Override
  public boolean equals(AElement o) {
    return o instanceof AMethod && ((AMethod) o).equalsMethod(this);
  }

  boolean equalsMethod(AMethod o) {
    parameters.prune();
    o.parameters.prune();

    return super.equals(o)
        && methodSignature.equals(o.methodSignature)
        // unneeded: && typeParameters.equals(o.typeParameters)
        && bounds.equals(o.bounds)
        // unneeded: && returnTypeMirror.equals
        && returnType.equalsTypeElement(o.returnType)
        && receiver.equals(o.receiver)
        && parameters.equals(o.parameters)
        && throwsException.equals(o.throwsException)
        && preconditions.equals(o.preconditions)
        && postconditions.equals(o.postconditions)
        && body.equals(o.body);
  }

  @Override
  public int hashCode() {
    parameters.prune();
    return Objects.hash(
        super.hashCode(),
        methodSignature,
        // unneeded: typeParameters,
        bounds,
        // unneeded: returnTypeMirror,
        returnType,
        receiver,
        parameters,
        throwsException,
        preconditions,
        postconditions,
        body);
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty()
        && bounds.isEmpty()
        && returnType.isEmpty()
        && receiver.isEmpty()
        && parameters.isEmpty()
        && throwsException.isEmpty()
        && preconditions.isEmpty()
        && postconditions.isEmpty()
        && body.isEmpty();
  }

  @Override
  public void prune() {
    super.prune();
    bounds.prune();
    returnType.prune();
    receiver.prune();
    parameters.prune();
    throwsException.prune();
    preconditions.prune();
    postconditions.prune();
    body.prune();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("AMethod ");
    sb.append(methodSignature);
    sb.append(": (");
    sb.append(" -1:");
    sb.append(receiver.toString());
    // int size = parameters.size();
    for (Map.Entry<Integer, AField> em : parameters.entrySet()) {
      Integer i = em.getKey();
      sb.append(" ");
      sb.append(i);
      sb.append(":");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(" ");
      ATypeElement ate = ae.type;
      sb.append(ate.toString());
    }
    sb.append(" ");
    sb.append("ret:");
    sb.append(returnType.toString());
    sb.append(") ");
    sb.append(body.toString());
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitMethod(this, t);
  }
}
