
/**
 * Bound and unbound constraints.
 */
interface Unbound {
    void consume(/*1*/ Unbound this , /*2*/ MyClass my, String s);
}
interface Bound {
    void consume(/*4*/ Bound this, String s);
}
interface Supplier<R> {
    R supply();
}

class MyClass {

    void take(/*6*/ MyClass this, String s) { }

    void context(/*7*/ MyClass my) {
        /*8*/ Unbound u1 = /*9*/ MyClass::take;
        // 2 <: 6 -- like an override
        // No relation to 1 or 2
        // No relationship or check for 8 / 9?
        // Need to check on this.

        u1.consume(my, "");
        // 7 <: 2
        // 8 <: 1

        /*10*/ Bound b1 = /*11*/ my::take;
        // 7 <: 6 -- like an invocation
        // No Relationship for 10 / 11?

        b1.consume("");
        // 10 <: 4
    }
}

/**
 * Constraints for implicit inner constraints
 * and super
 */
@SuppressWarnings({"javari", "oigj"})
class Outer {
    class Inner {
        Inner(/*1*/ Outer Outer.this) { }
        void context() {
            Supplier<String> o = Outer.super::toString;
        }
    }
    void context(/*2*/ Outer this) {
        // This one is unbound and needs an Outer as a param
        Supplier</*3*/Inner> f = /*4*/Inner::new;
    }
}
