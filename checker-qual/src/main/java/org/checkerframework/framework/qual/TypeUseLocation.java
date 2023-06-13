package org.checkerframework.framework.qual;

/**
 * Specifies the locations to which a {@link DefaultQualifier} annotation applies.
 *
 * <p>The order of enums is important. Defaults are applied in this order. In particular, this means
 * that OTHERWISE and ALL should be last.
 *
 * @see DefaultQualifier
 * @see javax.lang.model.element.ElementKind
 */
public enum TypeUseLocation {

  /** Apply default annotations to all unannotated raw types of fields. */
  FIELD,

  /**
   * Apply default annotations to all unannotated raw types of local variables, casts, and
   * instanceof.
   */
  LOCAL_VARIABLE,

  /** Apply default annotations to all unannotated raw types of resource variables. */
  RESOURCE_VARIABLE,

  /** Apply default annotations to all unannotated raw types of exception parameters. */
  EXCEPTION_PARAMETER,

  /** Apply default annotations to all unannotated raw types of receiver types. */
  RECEIVER,

  /**
   * Apply default annotations to all unannotated raw types of formal parameter types, excluding the
   * receiver.
   */
  PARAMETER,

  /** Apply default annotations to all unannotated raw types of return types. */
  RETURN,

  /** Apply default annotations to all unannotated raw types of constructor result types. */
  CONSTRUCTOR_RESULT,

  /**
   * Apply default annotations to unannotated lower bounds for type variables and wildcards both
   * explicit ones in {@code extends} clauses, and implicit upper bounds when no explicit {@code
   * extends} or {@code super} clause is present.
   */
  LOWER_BOUND,

  /**
   * Apply default annotations to unannotated, but explicit lower bounds: {@code <? super Object>}
   */
  EXPLICIT_LOWER_BOUND,

  /**
   * Apply default annotations to unannotated, but implicit lower bounds: {@code <T>} {@code <?>}.
   */
  IMPLICIT_LOWER_BOUND,

  /**
   * Apply default annotations to unannotated upper bounds: both explicit ones in {@code extends}
   * clauses, and implicit upper bounds when no explicit {@code extends} or {@code super} clause is
   * present.
   *
   * <p>Especially useful for parametrized classes that provide a lot of static methods with the
   * same generic parameters as the class.
   */
  UPPER_BOUND,

  /**
   * Apply default annotations to unannotated, but explicit upper bounds: {@code <T extends
   * Object>}.
   */
  EXPLICIT_UPPER_BOUND,

  /** Apply default annotations to unannotated type variables: {@code <T>}. */
  IMPLICIT_UPPER_BOUND,

  /** Apply if nothing more concrete is provided. TODO: clarify relation to ALL. */
  OTHERWISE,

  /**
   * Apply default annotations to all type uses other than uses of type parameters. Does not allow
   * any of the other constants. Usually you want OTHERWISE.
   */
  ALL;
}
