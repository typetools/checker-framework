// The test cases GenericNull, FieldAccess, and InferTypeArgs often fail together.
// Here are some things that might be wrong if they are failing.
//  * If you have overridden AnnotatedTypeFactory.createTreeAnnotator(), the body should return a
//    list that contains the result of running the overridden implementation, as in:
//     public TreeAnnotator createTreeAnnotator() {
//         return new ListTreeAnnotator(super.createTreeAnnotator(), new MyOwnTreeAnnotator(this));
//     }
//  * If 'null' is intentionally not the bottom type in your type hierarchy, then you may suppress
//    this warning for your particular type system.
//
//  * The standard defaulting rules set the qualifier of type variable lower
//    bounds to bottom. (https://checkerframework.org/manual/#climb-to-top)  If
//    you change this default to top, then this error will go away. (Add the
//    meta-annotation @DefaultFor(LOWER_BOUNDS) to the top annotation class.)
//    This will make types like List<@Bottom Object> illegal, so carefully
//    consider if this is desirable.
//

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
