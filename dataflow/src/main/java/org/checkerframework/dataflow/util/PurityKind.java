package org.checkerframework.dataflow.qual;

/** Varieties of purity, defined here for convenience and not user-visible. */
public enum PurityKind {
  /** The method has limited visible side effects. This value does not indicate what they are. */
  SIDE_EFFECT_FREE,

  /** The method has no visible side effects. */
  SIDE_EFFECT_FREE,

  /** The method returns exactly the same value when called in the same environment. */
  DETERMINISTIC
}
