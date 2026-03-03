package org.checkerframework.afu.scenelib.el;

// Gives an index into a class's set of supertypes (-1 = superclass,
// non-negative integers for implemented interfaces) or into a
// method's set of thrown exceptions.
public class TypeIndexLocation {
  public final int typeIndex;

  public TypeIndexLocation(int typeIndex) {
    this.typeIndex = typeIndex;
  }

  public boolean equals(TypeIndexLocation l) {
    return typeIndex == l.typeIndex;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TypeIndexLocation && equals((TypeIndexLocation) o);
  }

  @Override
  public int hashCode() {
    return typeIndex;
  }

  @Override
  public String toString() {
    return "TypeIndexLocation(" + typeIndex + ")";
  }
}
