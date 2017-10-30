import testlib.util.*;

/*
 * See Issue 137:
 * https://github.com/typetools/checker-framework/issues/137
 */
class GenericTest7 {
    interface A {}

    interface B<T> {}

    interface C<U> {}

    public <I extends B<A> & C<A>> void one(I i) {
        B<A> i1 = i;
        C<A> i2 = i;
    }

    public <I extends B<A> & C<A>> void oneA(I i) {
        // :: error: (assignment.type.incompatible)
        @Odd B<A> i1 = i;
        // :: error: (assignment.type.incompatible)
        @Odd C<A> i2 = i;
    }

    public <I extends @Odd B<A> & @Odd C<A>> void oneB(I i) {
        @Odd B<A> i1 = i;
        @Odd C<A> i2 = i;
    }

    public <I extends B<? extends A> & C<? extends A>> void two(I i) {
        B<? extends A> i1 = i;
        C<? extends A> i2 = i;
    }

    public <I extends B<? extends A> & C<? extends A>> void twoA(I i) {
        // :: error: (assignment.type.incompatible)
        @Odd B<? extends A> i1 = i;
        // :: error: (assignment.type.incompatible)
        @Odd C<? extends A> i2 = i;
    }

    public <I extends @Odd B<? extends A> & @Odd C<? extends A>> void twoB(I i) {
        @Odd B<? extends A> i1 = i;
        @Odd C<? extends A> i2 = i;
    }

    public <I extends B<? extends @Odd A> & C<? extends @Odd A>> void twoC(I i) {
        B<? extends A> i1 = i;
        C<? extends A> i2 = i;
        B<? extends @Odd A> i3 = i;
        C<? extends @Odd A> i4 = i;
    }
}
