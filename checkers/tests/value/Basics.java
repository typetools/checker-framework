import checkers.value.quals.*;

class Basics {

    public void boolTest() {
        boolean a = false;
        if (true) {
            a = true;
        }
        @BoolVal({ true, false }) boolean b = a;

        //:: error: (assignment.type.incompatible)
        @BoolVal({ false }) boolean c = a;
    }

    public void CharacterTest() {
        Character a = 'a';
        if (true) {
            a = 'b';
        }
        @CharVal({ 'a', 'b' }) Character b = a;

        //:: error: (assignment.type.incompatible)
        @CharVal({ 'a' }) Character c = a;
    }

    public void charTest() {
        char a = 'a';
        if (true) {
            a = 'b';
        }
        @CharVal({ 'a', 'b' }) char b = a;

        //:: error: (assignment.type.incompatible)
        @CharVal({ 'a' }) char c = a;
    }

    public void DoubleTest() {
        Double a = new Double(0.0);
        if (true) {
            a = 2.0;
        }
        @DoubleVal({ 0, 2 }) Double b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({ 0 }) Double c = a;

    }

    public void doubleTest() {
        double a = 0.0;
        if (true) {
            a = 2.0;
        }
        @DoubleVal({ 0, 2 }) double b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({ 0 }) double c = a;

    }

    public void FloatTest() {
        Float a = new Float(0.0f);
        if (true) {
            a = 2.0f;
        }
        @FloatVal({ 0, 2 }) Float b = a;

        //:: error: (assignment.type.incompatible)
        @FloatVal({ 0 }) Float c = a;

    }

    public void floatTest() {
        float a = 0.0f;
        if (true) {
            a = 2.0f;
        }
        @FloatVal({ 0, 2 }) float b = a;

        //:: error: (assignment.type.incompatible)
        @FloatVal({ 'a' }) float c = a;

    }

    public void intTest() {
        int a = 0;
        if (true) {
            a = 2;
        }
        @IntVal({ 0, 2 }) int b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({ 0 }) int c = a;
    }

    public void IntegerTest() {
        Integer a = new Integer(0);
        if (true) {
            a = 2;
        }
        @IntVal({ 0, 2 }) Integer b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({ 0 }) Integer c = a;
    }

    public void stringTest() {
        String a = "test1";
        if (true) {
            a = "test2";
        }
        @StringVal({ "test1", "test2" }) String b = a;

        //:: error: (assignment.type.incompatible)
        @StringVal({ "test1" }) String c = a;
    }
}
