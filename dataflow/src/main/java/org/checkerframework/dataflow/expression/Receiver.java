package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.dataflow.analysis.Store;

// The syntax that the Checker Framework uses for Java expressions also includes "<self>" and
// "#1" for formal parameters.  However, there are no special subclasses (AST nodes) for those
// extensions.
/**
 * This class represents a Java expression and its type. It does not represent all possible Java
 * expressions (for example, it does not represent a ternary conditional expression {@code ?:}; use
 * {@link org.checkerframework.dataflow.expression.Unknown} for unrepresentable expressions).
 *
 * <p>This class's representation is like an AST: subparts are also expressions. For declared names
 * (fields, local variables, and methods), it also contains an Element.
 *
 * <p>Each subclass represents a different type of expression, such as {@link
 * org.checkerframework.dataflow.expression.MethodCall}, {@link
 * org.checkerframework.dataflow.expression.ArrayAccess}, {@link
 * org.checkerframework.dataflow.expression.LocalVariable}, etc.
 *
 * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">the syntax of
 *     Java expressions supported by the Checker Framework</a>
 */
public abstract class Receiver {
    /** The type of this expression. */
    protected final TypeMirror type;

    /**
     * Create a Receiver (a Java AST node representing an expression).
     *
     * @param type the type of the expression
     */
    protected Receiver(TypeMirror type) {
        assert type != null;
        this.type = type;
    }

    public TypeMirror getType() {
        return type;
    }

    public abstract boolean containsOfClass(Class<? extends Receiver> clazz);

    public boolean containsUnknown() {
        return containsOfClass(Unknown.class);
    }

    /**
     * Returns true if and only if the value this expression stands for cannot be changed (with
     * respect to ==) by a method call. This is the case for local variables, the self reference,
     * final field accesses whose receiver is {@link #isUnassignableByOtherCode}, and binary
     * operations whose left and right operands are both {@link #isUnmodifiableByOtherCode}.
     *
     * @see #isUnmodifiableByOtherCode
     */
    public abstract boolean isUnassignableByOtherCode();

    /**
     * Returns true if and only if the value this expression stands for cannot be changed by a
     * method call, including changes to any of its fields.
     *
     * <p>Approximately, this returns true if the expression is {@link #isUnassignableByOtherCode}
     * and its type is immutable.
     *
     * @see #isUnassignableByOtherCode
     */
    public abstract boolean isUnmodifiableByOtherCode();

    /**
     * Returns true if and only if the two receivers are syntactically identical.
     *
     * @param other the other object to compare to this one
     * @return true if and only if the two receivers are syntactically identical
     */
    @EqualsMethod
    public boolean syntacticEquals(Receiver other) {
        return other == this;
    }

    /**
     * Returns true if and only if this receiver contains a receiver that is syntactically equal to
     * {@code other}.
     *
     * @return true if and only if this receiver contains a receiver that is syntactically equal to
     *     {@code other}
     */
    public boolean containsSyntacticEqualReceiver(Receiver other) {
        return syntacticEquals(other);
    }

    /**
     * Returns true if and only if {@code other} appears anywhere in this receiver or an expression
     * appears in this receiver such that {@code other} might alias this expression, and that
     * expression is modifiable.
     *
     * <p>This is always true, except for cases where the Java type information prevents aliasing
     * and none of the subexpressions can alias 'other'.
     */
    public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
        return this.equals(other) || store.canAlias(this, other);
    }

    /**
     * Print this verbosely, for debugging.
     *
     * @return a verbose string representation of this
     */
    public String toStringDebug() {
        return String.format(
                "Receiver (%s) %s type=%s", getClass().getSimpleName(), toString(), type);
    }
}
