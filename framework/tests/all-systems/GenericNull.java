// The test cases GenericNull, FieldAccess, and InferTypeArgs often fail together.
// Here are some things that might be wrong if they are failing.
//  * Did you write @ImplicitFor(literals = LiteralKind.NULL) on the bottom type in your type
//    hierarchy?
//  * If you have overridden AnnotatedTypeFactory.createTreeAnnotator(), the body should return a
//    list that contains the result of running the overridden implementation, as in:
//     public TreeAnnotator createTreeAnnotator() {
//         return new ListTreeAnnotator(super.createTreeAnnotator(), new MyOwnTreeAnnotator(this));
//     }

class GenericNull {
    /**
     * In most type systems, null's type is bottom and therefore the generic return type T is a
     * supertype of null's type.
     *
     * <p>However, in the nullness and lock type systems, null's type is not bottom, so they exclude
     * this test. For the Lock Checker, null's type is bottom for the @GuardedByUnknown hierarchy
     * but not for the @LockPossiblyHeld hierarchy.
     */
    @SuppressWarnings({"nullness", "lock"})
    <T> T f() {
        return null;
    }
}
