import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

public class ParameterExpression {
    public void m1(
            @Nullable Object o, @Nullable Object o1, @Nullable Object o2, @Nullable Object o3) {
        // :: error: (flowexpr.parse.error.postcondition)
        m2(o);
        // :: error: (dereference.of.nullable)
        o.toString();
        m3(o);
        o.toString();
        m4(o1, o2, o3);
        // :: error: (dereference.of.nullable)
        o1.toString();
        // :: error: (dereference.of.nullable)
        o2.toString();
        o3.toString();
    }

    @SuppressWarnings("assert.postcondition.not.satisfied")
    // "#0" is illegal syntax; it should be "#1"
    @EnsuresNonNull("#0")
    // :: error: (flowexpr.parse.error)
    public void m2(final @Nullable Object o) {}

    @SuppressWarnings("contracts.postcondition.not.satisfied")
    @EnsuresNonNull("#1")
    public void m3(final @Nullable Object o) {}

    @SuppressWarnings("contracts.postcondition.not.satisfied")
    @EnsuresNonNull("#3")
    public void m4(@Nullable Object x1, @Nullable Object x2, final @Nullable Object x3) {}

    // Formal parameter names should not be used on pre/postcondition, conditional postcondition
    // and formal parameter annotations in the same method declaration as the formal parameter
    // being referred. In this case, "#paramNum" should be used. This is because
    // the parameter names are not saved in bytecode.

    @Nullable Object field = null;

    // Postconditions
    @EnsuresNonNull("field") // OK
    public void m5() {
        field = new Object();
    }

    @EnsuresNonNull("param")
    // :: error: (flowexpr.parse.error)
    // :: warning: (contracts.postcondition.expression.parameter.name)
    public void m6(Object param) {
        param = new Object();
    }

    // Warning issued. 'field' is a field, but in this case what matters is that it is the name of a
    // formal parameter.
    @EnsuresNonNull("field")
    // The user can write "#1" if they meant the formal parameter, and "this.field" if they meant
    // the field.
    // :: error: (contracts.postcondition.not.satisfied)
    // :: warning: (contracts.postcondition.expression.parameter.name)
    public void m7(Object field) {
        field = new Object();
    }

    // Preconditions
    @RequiresNonNull("field") // OK
    public void m8() {}

    @RequiresNonNull("param")
    // :: error: (flowexpr.parse.error)
    // :: warning: (contracts.precondition.expression.parameter.name)
    public void m9(Object param) {}

    // Warning issued. 'field' is a field, but in this case what matters is that it is the name of a
    // formal parameter.
    @RequiresNonNull("field")
    // The user can write "#1" if they meant the formal parameter, and "this.field" if they meant
    // the field.
    // :: warning: (contracts.precondition.expression.parameter.name)
    public void m10(Object field) {}

    // Conditional postconditions
    @EnsuresNonNullIf(result = true, expression = "field") // OK
    public boolean m11() {
        field = new Object();
        return true;
    }

    @EnsuresNonNullIf(result = true, expression = "param")
    // :: error: (flowexpr.parse.error)
    // :: warning: (contracts.conditional.postcondition.expression.parameter.name)
    public boolean m12(Object param) {
        param = new Object();
        return true;
    }

    // Warning issued. 'field' is a field, but in this case what matters is that it is the name of a
    // formal parameter.
    @EnsuresNonNullIf(result = true, expression = "field")
    // The user can write "#1" if they meant the formal parameter, and "this.field" if they meant
    // the field.
    // :: warning: (contracts.conditional.postcondition.expression.parameter.name)
    public boolean m13(Object field) {
        field = new Object();
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    // Annotations on formal parameters referring to a formal parameter of the same method.
    // :: error: (expression.unparsable.type.invalid)
    public void m14(@KeyFor("param2") Object param1, Map<Object, Object> param2) {}
}
