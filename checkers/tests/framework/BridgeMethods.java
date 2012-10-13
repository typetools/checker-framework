import tests.util.Odd;

abstract class C<T> { abstract T id(T x); }
class D extends C<@Odd String> { @Odd String id(@Odd String x) { return x; } }

class Usage {
    void use() {
        C c = null; // new D();  D is not a subtype of C<Object>
        // TODO: we replace the non-present type argument with
        // ? extends Object, so the call does not work.
        // If we allowed this (via the wildcard hack used for method type
        // variable inference), then unsound constructs would pass.
        // So just don't use raw types and all is well.
        //:: error: (argument.type.incompatible)
        c.id(new Object()); // fails with a ClassCastException
    }
}
