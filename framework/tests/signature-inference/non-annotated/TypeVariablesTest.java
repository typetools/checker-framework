import tests.signatureinference.qual.*;
public class TypeVariablesTest<T1 extends Object> {

    // This method's parameter type should not be updated by the signature inference.
    public <A extends /*@Parent*/ Object> @SignatureInferenceBottom TypeVariablesTest<A> foo(A a) {
        return null;
    }

    void test() {
        //:: warning: (cast.unsafe) 
        @SignatureInferenceBottom String s = (@SignatureInferenceBottom String) "";
        foo(s);
    }

}