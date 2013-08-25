package checkers.quals;

/**
 * Specifies the locations to which a {@link DefaultQualifier} annotation applies.
 *
 * The order of enums is important. Defaults are applied in this order.
 * In particular, this means that OTHERWISE and ALL should be last.
 *
 * @see DefaultQualifier
 */
public enum DefaultLocation {

    /**
     * Apply default annotations to all unannotated raw types
     * of local types (local variables, casts, and instanceof).
     */
    LOCALS,
    /**
     * Apply default annotations to all unannotated raw types
     * of receiver types.
     */
    RECEIVERS,
    /**
     * Apply default annotations to all unannotated raw types
     * of formal parameter types.
     */
    PARAMETERS,

    /**
     * Apply default annotations to all unannotated raw types
     * of return types.
     */
    RETURNS,

    /**
     * Apply default annotations to unannotated upper bounds:  both
     * explicit ones in <tt>extends</tt> clauses, and implicit upper bounds
     * when no explicit <tt>extends</tt> or <tt>super</tt> clause is
     * present.
     *
     * Especially useful for parametrized classes that provide a lot of
     * static methods with the same generic parameters as the class.
     *
     * TODO: more doc, relation to other UPPER_BOUNDS
     */
    UPPER_BOUNDS,
    /**
     * Apply default annotations to unannotated, but explicit upper bounds:
     * @code{&lt;T extends Object&gt;}
     *
     * TODO: more doc, relation to other UPPER_BOUNDS
     */
    EXPLICIT_UPPER_BOUNDS,
    /**
     * Apply default annotations to unannotated type variables:
     * @code{&lt;T&gt;}
     *
     * TODO: more doc, relation to other UPPER_BOUNDS
     */
    IMPLICIT_UPPER_BOUNDS,

    /**
     * Apply if nothing more concrete is provided.
     * TODO: clarify relation to ALL.
     */
    OTHERWISE,

    /**
     * Apply default annotations to all type uses other than uses of type parameters.
     * Does not allow any of the other constants. Usually you want OTHERWISE.
     */
    ALL;
}
