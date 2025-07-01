package org.checkerframework.afu.scenelib.type;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a Java type. Handles type parameters, bounded types, arrays and inner types.
 */
public abstract class Type {

  /** The different kinds of {@link Type}s. */
  public enum Kind {
    ARRAY,
    BOUNDED,
    DECLARED
  }

  /** The annotations on the outer type. Empty if there are none. */
  private List<String> annotations;

  /** Constructs a new type with no outer annotations. */
  public Type() {
    annotations = new ArrayList<String>();
  }

  /**
   * Adds an outer annotation to this type.
   *
   * @param annotation the annotation to add
   */
  public void addAnnotation(String annotation) {
    annotations.add(annotation);
  }

  /**
   * Replaces the annotations on this type with the given annotations.
   *
   * @param annotations the new annotations to be placed on this type
   */
  public void setAnnotations(List<String> annotations) {
    this.annotations = annotations;
  }

  /**
   * Gets an outer annotation on this type at the given index.
   *
   * @param index the index
   * @return the annotation
   */
  public String getAnnotation(int index) {
    return annotations.get(index);
  }

  /**
   * Gets a copy of the outer annotations on this type. This will be empty if there are none.
   *
   * @return the annotations
   */
  public List<String> getAnnotations() {
    return new ArrayList<String>(annotations);
  }

  /** Removes the annotations from this type. */
  public void clearAnnotations() {
    annotations.clear();
  }

  /**
   * Gets the {@link Kind} of this {@link Type}.
   *
   * @return the kind
   */
  public abstract Kind getKind();
}
