package a.b.c;

import org.checkerframework.checker.nullness.qual.*;

public class Issue752 {

    Issue752 field = new Issue752();
    static Issue752 staticField = new Issue752();

    @RequiresNonNull("java.lang")
    //:: error: (flowexpr.parse.error)
    void method1() { }

    @RequiresNonNull("java.lang.String")
    void method2() { }

    @RequiresNonNull("a.b.c")
    //:: error: (flowexpr.parse.error)
    void method3() { }

    @RequiresNonNull("a.b.c.notaclass")
    //:: error: (flowexpr.parse.error)
    void method4() { }

    @RequiresNonNull("a.b.c.Issue752")
    void method5() {
    }

    @RequiresNonNull("a.b.c.Issue752.staticField")
    void method6() { }

    @RequiresNonNull("a.b.c.Issue752.staticField.field")
    void method7() { }

    @RequiresNonNull("a.b.c.Issue752.field")
    //:: error: (flowexpr.parse.error)
    void method8() { }

    @RequiresNonNull("a.b.c.Issue752.field.field")
    //:: error: (flowexpr.parse.error)
    void method9() { }
}
