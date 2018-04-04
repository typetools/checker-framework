import testlib.wholeprograminference.qual.*;

public class TypeVariablesTest<T1 extends @Parent Object, T2 extends @Parent Object> {

    // This method's parameter type should not be updated by the whole-program inference.
    // Even though there is only one call to foo with argument of type @WholeProgramInferenceBottom,
    // the method has in its signature that the parameter is a subtype of @Parent,
    // therefore no annotation should be added.
    public static <A extends @Parent Object, B extends @Parent Object> TypeVariablesTest<A, B> foo(
            A a, B b) {
        return null;
    }

    public static <A extends @Parent Object, B extends A> void typeVarWithTypeVarUB(A a, B b) {}

    void test1() {
        // :: warning: (cast.unsafe)
        @Parent String s = (@Parent String) "";
        foo(getSibling1(), getSibling2());
        typeVarWithTypeVarUB(getSibling1(), getSibling2());
    }

    static @Sibling1 int getSibling1() {
        return 0;
    }

    static @Sibling2 int getSibling2() {
        return 0;
    }
}
