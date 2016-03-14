import tests.wholeprograminference.qual.*;
public class TypeVariablesTest<T1 extends /*@Parent*/ Object, T2 extends /*@Parent*/ Object> {

    // This method's parameter type should not be updated by the whole-program inference.
    // Even though there is only one call to foo with argument of type @WholeProgramInferenceBottom,
    // the method has in its signature that the parameter is a subtype of @Parent,
    // therefore no annotation should be added.
    public static <A extends /*@Parent*/ Object, B extends /*@Parent*/ Object> TypeVariablesTest<A, B> foo(A a, B b) {
        return null;
    }

    void test() {
        //:: warning: (cast.unsafe)
        @Parent String s = (@Parent String) "";
        foo(s, s);
    }

}