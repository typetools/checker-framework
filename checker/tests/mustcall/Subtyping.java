// simple subtyping test for the MustCall annotation

import org.checkerframework.checker.mustcall.qual.*;

class Subtyping {

    Object unannotatedObj;

    void test_act(@Owning @MustCallUnknown Object o) {
        @MustCallUnknown Object act = o;
        // :: error: assignment
        @MustCall("close") Object file = o;
        // :: error: assignment
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment
        @MustCall({}) Object notAfile = o;
        // :: error: assignment
        unannotatedObj = o;
    }

    void test_close(@Owning @MustCall("close") Object o) {
        @MustCallUnknown Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment
        @MustCall({}) Object notAfile = o;
        // :: error: assignment
        unannotatedObj = o;
    }

    void test_close_read(@Owning @MustCall({"close", "read"}) Object o) {
        @MustCallUnknown Object act = o;
        // :: error: assignment
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment
        @MustCall({}) Object notAfile = o;
        // :: error: assignment
        unannotatedObj = o;
    }

    void test_blank(@Owning @MustCall({}) Object o) {
        @MustCallUnknown Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        @MustCall({}) Object notAfile = o;
        unannotatedObj = o;
    }

    void test_unannotated(@Owning Object o) {
        @MustCallUnknown Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        @MustCall({}) Object notAfile = o;
        unannotatedObj = o;
    }
}
