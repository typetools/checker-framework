import tests.jaifinference.qual.*;
public class MyObjectFieldInferenceTest {

    //:: warning: (cast.unsafe)
    private @Sibling1 MyObject annotatedField = (@Sibling1 MyObject) new MyObject();
    public MyObject refinedField = new MyObject();
    public MyObject notRefinedField = new MyObject();

    public void annotateField() {
        refinedField = annotatedField;
    }

    public @Sibling1 MyObject getSiblingRefined() {
        return refinedField;
    }

    public @JaifBottom MyObject getBottomWrong() {
        //:: error: (return.type.incompatible)
        return refinedField;
    }

    public @Sibling1 MyObject getSiblingNotRefined() {
        //:: error: (return.type.incompatible)
        return notRefinedField;
    }
}

class MyObject{}
