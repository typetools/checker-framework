import org.checkerframework.checker.propkey.qual.*;

// @skip-test
// @UnknownPropertyKey class Issue2163AB {
//    @PropertyKey Issue2163AB() {}
// }
//
// class ClientAB {
//    void test() {
//        @PropertyKeyBottom Issue2163AB propObj = new @PropertyKeyBottom Issue2163AB();
//        @PropertyKey Issue2163AB propObj1 = new @PropertyKey Issue2163AB();
//        @UnknownPropertyKey Issue2163AB propObj2 = new @UnknownPropertyKey Issue2163AB();
//        // Following statement gives the error (assignment.type.incompatible): (Fixed)
//        // found   : @UnknownPropertyKey Issue2163AB
//        // required: @PropertyKeyBottom Issue2163AB
//        // --------------------------------------------
//        // Instead, the error should be:
//        // found   : @PropertyKey Issue2163AB
//        // required: @PropertyKeyBottom Issue2163AB
//        @PropertyKeyBottom Issue2163AB propObj3 = new Issue2163AB();
//    }
// }

@UnknownPropertyKey class Issue2163AC {
    @PropertyKeyBottom Issue2163AC() {}
}

class ClientAC {
    void test() {
        @PropertyKeyBottom Issue2163AC propObj = new @PropertyKeyBottom Issue2163AC();
        @PropertyKey Issue2163AC propObj1 = new @PropertyKey Issue2163AC();
        @UnknownPropertyKey Issue2163AC propObj2 = new @UnknownPropertyKey Issue2163AC();
        // Following statement gives the error (assignment.type.incompatible): (Fixed)
        // found   : @UnknownPropertyKey Issue2163AC
        // required: @PropertyKeyBottom Issue2163AC
        // --------------------------------------------
        // There should be no error here
        @PropertyKeyBottom Issue2163AC propObj3 = new Issue2163AC();
    }
}

// @PropertyKey class Issue2163BC {
//    @PropertyKeyBottom Issue2163BC() {}
// }
//
// class ClientBC {
//    void test() {
//        @PropertyKeyBottom Issue2163BC propObj = new @PropertyKeyBottom Issue2163BC();
//        @PropertyKey Issue2163BC propObj1 = new @PropertyKey Issue2163BC();
//        // Following statement gives the error (assignment.type.incompatible): (Fixed)
//        // found   : @PropertyKey Issue2163BC
//        // required: @PropertyKeyBottom Issue2163BC
//        // --------------------------------------------
//        // There should be no error here
//        @PropertyKeyBottom Issue2163BC propObj3 = new Issue2163BC();
//    }
// }
//
// @UnknownPropertyKey class Issue2163AA {
//    @UnknownPropertyKey Issue2163AA() {}
// }
//
// class ClientAA {
//    void test() {
//        @PropertyKeyBottom Issue2163AA propObj = new @PropertyKeyBottom Issue2163AA();
//        @PropertyKey Issue2163AA propObj1 = new @PropertyKey Issue2163AA();
//        @UnknownPropertyKey Issue2163AA propObj2 = new @UnknownPropertyKey Issue2163AA();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163AA propObj3 = new Issue2163AA();
//    }
// }
//
// @PropertyKey class Issue2163BB {
//    @PropertyKey Issue2163BB() {}
// }
//
// class ClientBB {
//    void test() {
//        @PropertyKeyBottom Issue2163BB propObj = new @PropertyKeyBottom Issue2163BB();
//        @PropertyKey Issue2163BB propObj1 = new @PropertyKey Issue2163BB();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163BB propObj3 = new Issue2163BB();
//    }
// }
//
// @PropertyKeyBottom class Issue2163CC {
//    @PropertyKeyBottom Issue2163CC() {}
// }
//
// class ClientCC {
//    void test() {
//        @PropertyKeyBottom Issue2163CC propObj = new @PropertyKeyBottom Issue2163CC();
//        @PropertyKeyBottom Issue2163CC propObj3 = new Issue2163CC();
//    }
// }
//
// @UnknownPropertyKey class Issue2163A {
//    Issue2163A() {}
// }
//
// class ClientA {
//    void test() {
//        @PropertyKeyBottom Issue2163A propObj = new @PropertyKeyBottom Issue2163A();
//        @PropertyKey Issue2163A propObj1 = new @PropertyKey Issue2163A();
//        @UnknownPropertyKey Issue2163A propObj2 = new @UnknownPropertyKey Issue2163A();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163A propObj3 = new Issue2163A();
//    }
// }
//
// @PropertyKey class Issue2163B {
//    // Following statement gives error: (Issue#2186)
//    // error: [type.invalid.annotations.on.use] invalid type: annotations [@UnknownPropertyKey]
//    // conflict with declaration of type Issue2163B
//    // -----------------------------------------------------
//    // I think this shouldn't be an error (constructor return type should default to
// @PropertyKey?)
//    Issue2163B() {}
// }
//
// @PropertyKeyBottom class Issue2163C {
//    // Following statement gives error: (Issue#2186)
//    // error: [type.invalid.annotations.on.use] invalid type: annotations [@UnknownPropertyKey]
//    // conflict with declaration of type Issue2163C
//    // -----------------------------------------------------
//    // I think this shouldn't be an error (constructor return type should default to
// @PropertyKey?)
//    Issue2163C() {}
// }
//
// class Issue2163DefaultA {
//    @UnknownPropertyKey Issue2163DefaultA() {}
// }
//
// class ClientDefaultA {
//    void test() {
//        @PropertyKeyBottom Issue2163DefaultA propObj = new @PropertyKeyBottom Issue2163DefaultA();
//        @PropertyKey Issue2163DefaultA propObj1 = new @PropertyKey Issue2163DefaultA();
//        @UnknownPropertyKey Issue2163DefaultA propObj2 = new @UnknownPropertyKey
// Issue2163DefaultA();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163DefaultA propObj3 = new Issue2163DefaultA();
//    }
// }
//
// class Issue2163DefaultB {
//    @PropertyKey Issue2163DefaultB() {}
// }
//
// class ClientDefaultB {
//    void test() {
//        @PropertyKeyBottom Issue2163DefaultB propObj = new @PropertyKeyBottom Issue2163DefaultB();
//        @PropertyKey Issue2163DefaultB propObj1 = new @PropertyKey Issue2163DefaultB();
//        // The following line doesn't report any error: (Issue#2187)
//        // ----------------------------------------------
//        // I think it shouldn't be allowed because the constructor is annotated as @PropertyKey
//        @UnknownPropertyKey Issue2163DefaultB propObj2 = new @UnknownPropertyKey
// Issue2163DefaultB();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163DefaultB propObj3 = new Issue2163DefaultB();
//    }
// }
//
// class Issue2163DefaultC {
//    @PropertyKeyBottom Issue2163DefaultC() {}
// }
//
// class ClientDefaultC {
//    void test() {
//        @PropertyKeyBottom Issue2163DefaultC propObj = new @PropertyKeyBottom Issue2163DefaultC();
//        // The following line doesn't report any error: (Issue#2187)
//        // ----------------------------------------------
//        // I think it shouldn't be allowed because the constructor is annotated as
//        // @PropertyKeyBottom
//        @PropertyKey Issue2163DefaultC propObj1 = new @PropertyKey Issue2163DefaultC();
//        // The following line doesn't report any error: (Issue#2187)
//        // ----------------------------------------------
//        // I think it shouldn't be allowed because the constructor is annotated as
//        // @PropertyKeyBottom
//        @UnknownPropertyKey Issue2163DefaultC propObj2 = new @UnknownPropertyKey
// Issue2163DefaultC();
//        @PropertyKeyBottom Issue2163DefaultC propObj3 = new Issue2163DefaultC();
//    }
// }
//
// class Issue2163 {
//    Issue2163() {}
// }
//
// class Client {
//    void test() {
//        @PropertyKeyBottom Issue2163 propObj = new @PropertyKeyBottom Issue2163();
//        @PropertyKey Issue2163 propObj1 = new @PropertyKey Issue2163();
//        @UnknownPropertyKey Issue2163 propObj2 = new @UnknownPropertyKey Issue2163();
//        // :: error: (assignment.type.incompatible)
//        @PropertyKeyBottom Issue2163 propObj3 = new Issue2163();
//    }
// }
