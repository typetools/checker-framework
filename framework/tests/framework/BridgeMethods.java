import testlib.util.Even;
import testlib.util.Odd;

abstract class C<T extends @Odd Object> {
    abstract T id(T x);
}

class D extends C<@Odd String> {
    @Odd String id(@Odd String x) {
        return x;
    }
}

class Usage {
    void use() {
        C c = new D(); // C<@Odd String>();
        // Oddness is OK, will fail with ClassCastException
        // :: warning: [unchecked] unchecked call to id(T) as a member of the raw type C
        // :: warning: (cast.unsafe.constructor.invocation)
        c.id(new @Odd Object());

        // Oddness is wrong! Would also fail with ClassCastException.
        // TODO: false negative. See #635.
        //// :: error: (argument.type.incompatible)
        // :: warning: [unchecked] unchecked call to id(T) as a member of the raw type C
        // :: warning: (cast.unsafe.constructor.invocation)
        c.id(new @Even Object());
    }
}
