package testpackage;

import org.checkerframework.checker.lock.qual.Holding;

class ClassLiterals {
    @Holding("ClassLiterals.class")
    static Object method1() {
        return new Object();
    }

    @Holding("ClassLiterals") // a class literal may not terminate a flow expression string
    //:: error: (flowexpr.parse.error)
    static void method2() {
    }

    @Holding("ClassLiterals.method1()")
    static void method3() {
    }

    @Holding("testpackage.ClassLiterals.class")
    static void method4() {
    }

    @Holding("testpackage.ClassLiterals") // a class literal may not terminate a flow expression string
    //:: error: (flowexpr.parse.error)
    static void method5() {
    }

    @Holding("testpackage.ClassLiterals.method1()")
    static void method6() {
    }
}
