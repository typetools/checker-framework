package org.checkerframework.afu.scenelib.type;

/** A representation of an array type. */
public class ArrayType extends Type {

  /** The type of elements this array holds. */
  private Type componentType;

  /**
   * Constructs a new array type.
   *
   * @param componentType the type of elements this array holds
   */
  public ArrayType(Type componentType) {
    super();
    this.componentType = componentType;
  }

  /**
   * Gets the component type.
   *
   * @return the component type
   */
  public Type getComponentType() {
    return componentType;
  }

  @Override
  public Kind getKind() {
    return Kind.ARRAY;
  }
}
