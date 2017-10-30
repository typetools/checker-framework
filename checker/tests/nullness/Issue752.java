package a.b.c;

import org.checkerframework.checker.nullness.qual.*;

public class Issue752 {

    Issue752 field = new Issue752();
    static Issue752 staticField = new Issue752();

    Issue752 method() {
        return field;
    }

    static Issue752 staticMethod() {
        return staticField;
    }

    // A package name without a class name is not a valid flow expression string.
    @RequiresNonNull("java.lang")
    // :: error: (flowexpr.parse.error)
    void method1() {}

    @RequiresNonNull("java.lang.String.class")
    void method2() {}

    // A package name without a class name is not a valid flow expression string.
    @RequiresNonNull("a.b.c")
    // :: error: (flowexpr.parse.error)
    void method3() {}

    // notaclass does not exist.
    @RequiresNonNull("a.b.c.notaclass")
    // :: error: (flowexpr.parse.error)
    void method4() {}

    @RequiresNonNull("a.b.c.Issue752.class")
    void method5() {}

    @RequiresNonNull("a.b.c.Issue752.staticField")
    void method6() {}

    @RequiresNonNull("a.b.c.Issue752.staticField.field")
    void method7() {}

    // field is an instance field, and Issue752 is a class.
    @RequiresNonNull("a.b.c.Issue752.field")
    // :: error: (flowexpr.parse.error)
    void method8() {}

    // field is an instance field, and Issue752 is a class.
    @RequiresNonNull("a.b.c.Issue752.field.field")
    // :: error: (flowexpr.parse.error)
    void method9() {}

    @RequiresNonNull("a.b.c.Issue752.staticMethod()")
    void method10() {}

    @RequiresNonNull("a.b.c.Issue752.staticMethod().field")
    void method11() {}

    // method() is an instance method, and Issue752 is a class.
    @RequiresNonNull("a.b.c.Issue752.method()")
    // :: error: (flowexpr.parse.error)
    void method12() {}

    // method() is an instance method, and Issue752 is a class.
    @RequiresNonNull("a.b.c.Issue752.method().field")
    // :: error: (flowexpr.parse.error)
    void method13() {}
}
