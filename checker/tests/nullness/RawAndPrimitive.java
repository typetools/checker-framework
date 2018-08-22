class RawAndPrimitive<T> {
    public T foo(T startValue) {
        return startValue;
    }

    // this tests that DefaultTypeHierarchy.visitPrimitive_Wildcard works
    public static void bar(float f) {
        // the lower bound of the resultant wildcard (which replaces the raw type argument) will be
        // lower than the default annotation on float

        // :: warning: [unchecked] unchecked call to foo(T) as a member of the raw type
        // RawAndPrimitive
        new RawAndPrimitive().foo(f);
    }
}
