import org.checkerframework.common.value.qual.*;

class Basics {

    public void boolTest() {
        boolean a = false;
        if (true) {
            a = true;
        }
        @BoolVal({true, false}) boolean b = a;

        //:: error: (assignment.type.incompatible)
        @BoolVal({false}) boolean c = a;
    }

    public void CharacterTest() {
        Character a = 'a';
        if (true) {
            a = 'b';
        }
        @IntVal({'a', 'b'}) Character b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({'a'}) Character c = a;
    }

    public void charTest() {
        char a = 'a';
        if (true) {
            a = 'b';
        }
        @IntVal({'a', 'b'}) char b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({'a'}) char c = a;
    }

    public void DoubleTest() {
        Double a = new Double(0.0);
        if (true) {
            a = 2.0;
        }
        @DoubleVal({0, 2}) Double b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({0}) Double c = a;
    }

    public void doubleTest() {
        double a = 0.0;
        if (true) {
            a = 2.0;
        }
        @DoubleVal({0, 2}) double b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({0}) double c = a;
    }

    public void FloatTest() {
        Float a = new Float(0.0f);
        if (true) {
            a = 2.0f;
        }
        @DoubleVal({0, 2}) Float b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({0}) Float c = a;
    }

    public void floatTest() {
        float a = 0.0f;
        if (true) {
            a = 2.0f;
        }
        @DoubleVal({0, 2}) float b = a;

        //:: error: (assignment.type.incompatible)
        @DoubleVal({'a'}) float c = a;
    }

    public void intTest(@IntRange(from = 0, to = 10) int x, @IntRange(from = 20, to = 30) int y) {
        int a = 0;
        if (true) {
            // @IntVal / @IntVal
            a = 2;
        }

        int z = x;
        if (true) {
            // @IntRange / @IntRange
            z = y;
        }

        int i = y;
        if (true) {
            // @IntRange / @IntVal
            i = 15;
        }

        @IntVal({0, 2}) int b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({0}) int c = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) int d = z;

        @IntRange(from = 0, to = 20)
        int e = a;

        @IntRange(from = 1, to = 100)
        //:: error: (assignment.type.incompatible)
        int f = a;

        @IntRange(from = 1, to = 40)
        //:: error: (assignment.type.incompatible)
        int g = z;

        @IntRange(from = 15, to = 30)
        int j = i;

        @IntRange(from = 20, to = 40)
        //:: error: (assignment.type.incompatible)
        int k = i;
    }

    public void IntegerTest(
            @IntRange(from = 0, to = 10) Integer x, @IntRange(from = 20, to = 30) Integer y) {
        Integer a = new Integer(0);
        if (true) {
            a = 2;
        }

        Integer z = x;
        if (true) {
            z = y;
        }

        @IntVal({0, 2}) Integer b = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({0}) Integer c = a;

        //:: error: (assignment.type.incompatible)
        @IntVal({0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) Integer d = z;

        @IntRange(from = 0, to = 20)
        Integer e = a;

        @IntRange(from = 1, to = 100)
        //:: error: (assignment.type.incompatible)
        Integer f = a;

        @IntRange(from = 1, to = 40)
        //:: error: (assignment.type.incompatible)
        Integer g = z;
    }

    public void stringTest() {
        String a = "test1";
        if (true) {
            a = "test2";
        }
        @StringVal({"test1", "test2"}) String b = a;

        //:: error: (assignment.type.incompatible)
        @StringVal({"test1"}) String c = a;
    }

    public void stringCastTest() {
        Object a = "test1";
        @StringVal({"test1"}) String b = (String) a;
        @StringVal({"test1"}) String c = (java.lang.String) b;
    }

    void tooManyValuesInt() {
        //:: warning: (too.many.values.given)
        @IntVal({1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 100}) int a = 8;

        @UnknownVal int b = a; // This should always succeed

        @UnknownVal int c = 20;

        a = c; // This should succeed if a is treated as @IntRange(from=1, to=100)

        //:: warning: (too.many.values.given)
        @IntVal({1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
        //:: error: (assignment.type.incompatible)
        int d = a; // d is @IntRange(from=1, to=12), a is @IntVal({20});
    }

    void fromGreaterThanTo() {
        //:: error: (from.greater.than.to)
        @IntRange(from = 2, to = 0)
        int a;

        a = 3; //should be succeed if a is treated as @UnknownVal
    }

    void tooManyValuesDouble() {
        //:: warning: (too.many.values.given)
        @DoubleVal({1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0}) double a = 8.0;

        @UnknownVal double b = a; // This should always succeed

        @UnknownVal double c = 0;

        a = c; // This should succeed if a is treated as @UnknownVal

        //:: warning: (too.many.values.given)
        @DoubleVal({1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0}) double d = 8.0;

        d = 2.0 * d; // This should succeed since d is @UnknownVal
    }

    void tooManyValuesString() {
        //:: warning: (too.many.values.given)
        @StringVal({"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"}) String a = "h";

        @UnknownVal String b = a; // This should always succeed

        @UnknownVal String c = "";

        a = c; // This should succeed if a is treated as @UnknownVal

        //:: warning: (too.many.values.given)
        @StringVal({"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"}) String d = "h";

        d = "b" + d; // This should succeed since d is @UnknownVal
    }
}
