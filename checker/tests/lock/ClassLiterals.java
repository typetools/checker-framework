package testpackage;

import org.checkerframework.checker.lock.qual.Holding;

public class ClassLiterals {
    @Holding("ClassLiterals.class")
    static Object method1() {
        return new Object();
    }

    // a class literal may not terminate a JavaExpression string
    @Holding("ClassLiterals")
    // :: error: (flowexpr.parse.error)
    static void method2() {}

    @Holding("ClassLiterals.method1()")
    static void method3() {}

    @Holding("testpackage.ClassLiterals.class")
    static void method4() {}

    // a class literal may not terminate a JavaExpression string
    @Holding("testpackage.ClassLiterals")
    // :: error: (flowexpr.parse.error)
    static void method5() {}

    @Holding("testpackage.ClassLiterals.method1()")
    static void method6() {}

    @Holding("java.lang.Comparable.class")
    static void method7() {}
}
