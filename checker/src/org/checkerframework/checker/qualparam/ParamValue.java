package org.checkerframework.checker.qualparam;

/** A <code>ParamValue&lt;Q&gt;</code> is anything that can be assigned to a
 * qualifier parameter.  Example subtypes include qualifiers from the
 * underlying hierarchy (wrapped in <code>BaseQual</code>), wildcards
 * (<code>WildcardQual</code>), and qualifier variables (<code>QualVar</code>).
 */
public abstract class ParamValue<Q> {
    /** Substitute in a <code>ParamValue</code> for all occurrences of a
     * particular variable in this <code>ParamValue</code>.
     */
    public abstract ParamValue<Q> substitute(String name, ParamValue<Q> value);

    /** Apply capture conversion to this <code>ParamValue</code>.  Capture
     * conversion replaces each wildcard of the form <code>? extends U super
     * L</code> within this <code>ParamValue</code> with a fresh type variable
     * with upper bound <code>U</code> and lower bound <code>L</code>.
     */
    public abstract ParamValue<Q> capture();

    /** Returns the GLB of all qualifiers which this <code>ParamValue</code>
     * may represent under some assignment of qualifiers to variables.
     */
    public abstract BaseQual<Q> getMinimum();

    /** Returns the LUB of all qualifiers which this <code>ParamValue</code>
     * may represent under some assignment of qualifiers to variables.
     */
    public abstract BaseQual<Q> getMaximum();
}
