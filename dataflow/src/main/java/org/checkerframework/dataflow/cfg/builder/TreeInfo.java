package org.checkerframework.dataflow.cfg.builder;

import javax.lang.model.type.TypeMirror;

/** A tuple with 4 named elements. */
/*package-private*/ interface TreeInfo {
  /**
   * Returns true if unboxing conversion applies to this: it is a boxed primitive, or a type
   * variable or intersection type whose erasure is a boxed primitive.
   *
   * @return true if unboxing conversion applies to this
   */
  boolean isUnboxable();

  /**
   * Returns true if this is numeric.
   *
   * @return true if this is numeric
   */
  boolean isNumeric();

  /**
   * Returns true if this is boolean.
   *
   * @return true if this is boolean
   */
  boolean isBoolean();

  /**
   * Returns the unboxed type that this wraps.
   *
   * @return the unboxed type that this wraps
   */
  TypeMirror unboxedType();
}
