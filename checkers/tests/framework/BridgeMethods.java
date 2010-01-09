import checkers.util.test.Odd;

abstract class C<T> { abstract T id(T x); }
class D extends C<@Odd String> { @Odd String id(@Odd String x) { return x; } }

class Usage {
    void use() {
        C c = null; // new D();  D is not a subtype of C<Object>
        c.id(new Object()); // fails with a ClassCastException
    }
}
