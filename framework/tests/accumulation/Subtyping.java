// A basic test that subtyping between accumulation annotations works as expected. Note that GJF
// botches the formatting in this file, so the comments for error messages are in weird places.

import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class Subtyping {
  void top(@TestAccumulation() Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo")
    // :: error: assignment
    Object o3 = o1;
    @TestAccumulation("bar")
    // :: error: assignment
    Object o4 = o1;
    @TestAccumulation({"foo", "bar"})
    // :: error: assignment
    Object o5 = o1;
    // :: error: assignment
    @TestAccumulationBottom Object o6 = o1;
  }

  void foo(@TestAccumulation("foo") Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo") Object o3 = o1;
    @TestAccumulation("bar")
    // :: error: assignment
    Object o4 = o1;
    @TestAccumulation({"foo", "bar"})
    // :: error: assignment
    Object o5 = o1;
    // :: error: assignment
    @TestAccumulationBottom Object o6 = o1;
  }

  void bar(@TestAccumulation("bar") Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo")
    // :: error: assignment
    Object o3 = o1;
    @TestAccumulation("bar") Object o4 = o1;
    @TestAccumulation({"foo", "bar"})
    // :: error: assignment
    Object o5 = o1;
    // :: error: assignment
    @TestAccumulationBottom Object o6 = o1;
  }

  void foobar(@TestAccumulation({"foo", "bar"}) Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo") Object o3 = o1;
    @TestAccumulation("bar") Object o4 = o1;
    @TestAccumulation({"foo", "bar"}) Object o5 = o1;
    // :: error: assignment
    @TestAccumulationBottom Object o6 = o1;
  }

  void barfoo(@TestAccumulation({"bar", "foo"}) Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo") Object o3 = o1;
    @TestAccumulation("bar") Object o4 = o1;
    @TestAccumulation({"foo", "bar"}) Object o5 = o1;
    // :: error: assignment
    @TestAccumulationBottom Object o6 = o1;
  }

  void bot(@TestAccumulationBottom Object o1) {
    @TestAccumulation() Object o2 = o1;
    @TestAccumulation("foo") Object o3 = o1;
    @TestAccumulation("bar") Object o4 = o1;
    @TestAccumulation({"foo", "bar"}) Object o5 = o1;
    @TestAccumulationBottom Object o6 = o1;
  }
}
