import tests.signatureinference.qual.*;
public class TypeVariablesTest<T1 extends Object> {

    // This method's parameter type should not be updated by the signature inference.
    // Even though there is only one call to foo with argument of type @SignatureInferenceBottom,
    // the method has in its signature that the parameter is a subtype of @Parent,
    // therefore no annotation should be added.
    public <A extends /*@Parent*/ Object> @SignatureInferenceBottom TypeVariablesTest<A> foo(A a) {
        return null;
    }

    void test() {
        //:: warning: (cast.unsafe) 
        @SignatureInferenceBottom String s = (@SignatureInferenceBottom String) "";
        foo(s);
    }

}