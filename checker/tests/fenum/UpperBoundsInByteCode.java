import org.checkerframework.checker.fenum.qual.Fenum;
import testlib.lib.UncheckedByteCode;

public class UpperBoundsInByteCode {
    UncheckedByteCode<@Fenum("Foo") String> foo;
    UncheckedByteCode<@Fenum("Bar") Object> bar;

    void typeVarWithNonObjectUpperBound(@Fenum("A") int a) {
        // :: error: (type.argument.type.incompatible)
        UncheckedByteCode.methodWithTypeVarBoundedByNumber(a);
        UncheckedByteCode.methodWithTypeVarBoundedByNumber(1);
    }

    void wildcardsInByteCode() {
        UncheckedByteCode.unboundedWildcardParam(foo);
        UncheckedByteCode.lowerboundedWildcardParam(bar);
        // :: error: (argument.type.incompatible)
        UncheckedByteCode.upperboundedWildcardParam(foo);
    }

    // :: error: (type.argument.type.incompatible)
    SourceCode<@Fenum("Foo") String> foo2;

    class SourceCode<T extends Object> {}
}
