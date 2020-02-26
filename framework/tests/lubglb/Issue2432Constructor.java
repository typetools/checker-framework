// Test case for issue 2432, constructor part:
// https://github.com/typetools/checker-framework/issues/2432

import lubglb.quals.*;

class Issue2432C {

    // super.invocation.invalid: Object is @A by default and it is unreasonable to change jdk stub
    // just because of this.
    // inconsistent.constructor.type: just because the qualifier on the returning type is not top.
    @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
    @Poly Issue2432C(@Poly Object dummy) {}

    @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
    @Poly Issue2432C(@Poly Object dummy1, @Poly Object dummy2) {}

    static class TypeParamClass<T> {
        // @Poly on T shouldn't be in the poly resolving process
        @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
        @Poly TypeParamClass(@Poly Object dummy, T t) {}

        // 2 poly param for testing lub
        @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
        @Poly TypeParamClass(@Poly Object dummy1, @Poly Object dummy2, T t) {}
    }

    // This class is useful because only non-static inner class have a recevier in constructors
    // Useful links on how to use the explicit receiver type on nested class constructors:
    // https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html
    // https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-ReceiverParameter
    // "In an inner class's constructor, the type of the receiver parameter must be the class or
    // interface which is the immediately enclosing type declaration of the inner class, and the
    // name of the receiver parameter must be Identifier . this where Identifier is the simple name
    // of the class or interface which is the immediately enclosing type declaration of the inner
    // class; otherwise, a compile-time error occurs."
    class ReceiverClass {

        // if the qualifier on receiver is @Poly, it should not be involved in poly resolve process
        @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
        @Poly ReceiverClass(Issue2432C Issue2432C.this, @Poly Object dummy) {}

        // 2 poly param for testing lub
        @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
        @Poly ReceiverClass(Issue2432C Issue2432C.this, @Poly Object dummy1, @Poly Object dummy2) {}
    }

    void invokeConstructors(@A Object top, @F Object bottom, @Poly Object poly) {
        // :: error: (assignment.type.incompatible)
        @F Issue2432C bottomOuter = new Issue2432C(top); // B4C  // AFC
        @A Issue2432C topOuter = new Issue2432C(top);

        // lub test
        @A Issue2432C bottomOuter2 = new Issue2432C(top, bottom);
        // :: error: (assignment.type.incompatible)
        @B Issue2432C bottomOuter3 = new Issue2432C(top, bottom);

        @F Issue2432C bottomOuter4 = new Issue2432C(bottom, bottom);
    }

    // invoke constructors with a receiver to test poly resolving
    // note: seems CF already works well on these before changes
    void invokeReceiverConstructors(
            @A Issue2432C topOuter, @Poly Issue2432C polyOuter, @F Object bottom, @A Object top) {
        Issue2432C.@F ReceiverClass ref1 = polyOuter.new ReceiverClass(bottom);
        // :: error: (assignment.type.incompatible)
        Issue2432C.@B ReceiverClass ref2 = polyOuter.new ReceiverClass(top); // B4C  // AFC

        // lub tests
        Issue2432C.@A ReceiverClass ref3 = polyOuter.new ReceiverClass(top, bottom);
        // :: error: (assignment.type.incompatible)
        Issue2432C.@B ReceiverClass ref4 = polyOuter.new ReceiverClass(top, bottom);

        Issue2432C.@F ReceiverClass ref5 = polyOuter.new ReceiverClass(bottom, bottom);
    }

    // invoke constructors with a type parameter to test poly resolving
    void invokeTypeVarConstructors(@A Object top, @F Object bottom, @Poly Object poly) {
        @F TypeParamClass<@Poly Object> ref1 = new TypeParamClass<>(bottom, poly); // B4C
        // :: error: (assignment.type.incompatible)
        @B TypeParamClass<@Poly Object> ref2 = new TypeParamClass<>(top, poly); // B4C  // AFC

        // lub tests
        @A TypeParamClass<@Poly Object> ref3 = new TypeParamClass<>(bottom, top, poly);
        // :: error: (assignment.type.incompatible)
        @B TypeParamClass<@Poly Object> ref4 = new TypeParamClass<>(bottom, top, poly);

        @F TypeParamClass<@Poly Object> ref5 = new TypeParamClass<>(bottom, bottom, poly);
    }
}
