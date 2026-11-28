package org.checkerframework.dataflow.util;

/** Varieties of purity. */
public enum PurityKind {
  /** The method has no visible side effects. */
  SIDE_EFFECT_FREE,

  /** The method has limited visible side effects. This value does not indicate what they are. */
  SIDE_EFFECTS_ONLY,

  /** The method returns exactly the same value when called in the same environment. */
  DETERMINISTIC
}
