package org.checkerframework.afu.scenelib.el;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/** A field or method formal parameter. */
public class AField extends ADeclaration {

  /** The name of this field or formal parameter. */
  private String name;

  /** Javac's representation of the type of this. */
  private TypeMirror typeMirror;

  /** The field's initializer. */
  public AExpression init;

  /**
   * Create an AField of the given name.
   *
   * @param name the name of the field or formal parameter
   */
  public AField(String name) {
    super(name);
    this.name = name;
    this.typeMirror = null;
    this.init = null;
  }

  /**
   * Create an AField of the given name and type.
   *
   * @param name the name of the field or formal parameter
   * @param typeMirror javac's representation of the type of the wrapped field
   */
  public AField(String name, TypeMirror typeMirror) {
    super(name);
    this.name = name;
    this.typeMirror = typeMirror;
    this.init = null;
  }

  /**
   * Create an AField that is a copy of the given one.
   *
   * @param field the AField to copy
   */
  public AField(AField field) {
    super(field.description, field);
    name = field.name;
    typeMirror = field.typeMirror;
    init = field.init == null ? null : field.init.clone();
  }

  /**
   * Returns the name of this field or formal parameter.
   *
   * @return the name of this field or formal parameter
   */
  public String getName() {
    return name;
  }

  /** A pattern that matches a string consisting only of digits. */
  private Pattern digits = Pattern.compile("^[0-9]+$");

  /**
   * Set the name of this field or formal parameter.
   *
   * @param newName the new name of this field or formal parameter
   */
  public void setName(String newName) {
    if (name.equals(newName)) {
      return;
    }
    if (digits.matcher(name).matches()) {
      name = newName;
      description = newName;
      return;
    }
    throw new Error(String.format("old name=%s, new name=%s", name, newName));
  }

  /**
   * Returns javac's representation of the type of this.
   *
   * @return javac's representation of the type of this
   */
  public TypeMirror getTypeMirror() {
    return typeMirror;
  }

  /**
   * Set the TypeMirror this.
   *
   * @param typeMirror javac's representation of the type of this
   */
  public void setTypeMirror(TypeMirror typeMirror) {
    this.typeMirror = typeMirror;
  }

  @Override
  public AField clone() {
    return new AField(this);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AField && equalsField((AField) o);
  }

  /**
   * Returns true if this is equal to the given AField.
   *
   * @param o the other AField to compare to
   * @return true if this is equal to the given AField
   */
  final boolean equalsField(AField o) {
    // With this implementation, tests fail (!).
    // return o != null && super.equals(o) && name.equals(o.name) && typeMirror.equals(o.typeMirror)
    // && init.equals(o.init);
    return super.equals(o);
  }

  // Just use super.hashCode(), since equals() is just super.equals().
  // @Override
  // public int hashCode() {
  //   return super.hashCode() + Objects.hash(name, typeMirror, init);
  // }

  /**
   * Returns a printed representation of this.
   *
   * @return a printed representation of this
   */
  @Override // TODO: remove?
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("AField ");
    sb.append(name);
    sb.append(" [typeMirror=" + typeMirror + "; init=" + init + "; annos=");
    tlAnnotationsHereFormatted(sb);
    sb.append("; type=");
    type.tlAnnotationsHereFormatted(sb);
    sb.append("] ");
    sb.append(super.toString());
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitField(this, t);
  }

  static <K extends Object> VivifyingMap<K, AField> newVivifyingLHMap_AF() {
    return new VivifyingMap<K, AField>(new LinkedHashMap<>()) {
      @Override
      public AField createValueFor(K k) {
        return new AField("" + k);
      }

      @Override
      public boolean isEmptyValue(AField v) {
        return v.isEmpty();
      }
    };
  }
}
