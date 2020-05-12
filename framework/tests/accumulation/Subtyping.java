// A basic test that subtyping between accumulation annotations
// works as expected.

import testaccumulation.qual.*;

class Subtyping {
    void top(@TestAccumulationTop Object o1) {
        @TestAccumulationTop Object o2 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation("foo") Object o3 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation("bar") Object o4 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulationBottom Object o6 = o1;
    }

    void foo(@TestAccumulation("foo") Object o1) {
        @TestAccumulationTop Object o2 = o1;
        @TestAccumulation("foo") Object o3 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation("bar") Object o4 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulationBottom Object o6 = o1;
    }

    void bar(@TestAccumulation("bar") Object o1) {
        @TestAccumulationTop Object o2 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation("foo") Object o3 = o1;
        @TestAccumulation("bar") Object o4 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulationBottom Object o6 = o1;
    }

    void foobar(@TestAccumulation({"foo", "bar"}) Object o1) {
        @TestAccumulationTop Object o2 = o1;
        @TestAccumulation("foo") Object o3 = o1;
        @TestAccumulation("bar") Object o4 = o1;
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulationBottom Object o6 = o1;
    }

    void barfoo(@TestAccumulation({"bar", "foo"}) Object o1) {
        @TestAccumulationTop Object o2 = o1;
        @TestAccumulation("foo") Object o3 = o1;
        @TestAccumulation("bar") Object o4 = o1;
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        // :: error: assignment.type.incompatible
        @TestAccumulationBottom Object o6 = o1;
    }

    void bot(@TestAccumulationBottom Object o1) {
        @TestAccumulationTop Object o2 = o1;
        @TestAccumulation("foo") Object o3 = o1;
        @TestAccumulation("bar") Object o4 = o1;
        @TestAccumulation({"foo", "bar"}) Object o5 = o1;
        @TestAccumulationBottom Object o6 = o1;
    }
}
