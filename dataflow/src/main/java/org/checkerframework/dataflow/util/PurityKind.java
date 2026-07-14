package org.checkerframework.dataflow.util;

/** Varieties of purity. */
public enum PurityKind {
  /** The method has no visible side effects. */
  SIDE_EFFECT_FREE,

  /** The method returns exactly the same value when called in the same environment. */
  DETERMINISTIC
}
