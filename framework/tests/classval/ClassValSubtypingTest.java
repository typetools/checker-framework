import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;

public class ClassValSubtypingTest {
  @ClassVal("a") Object a = null;

  @ClassVal({"a", "b"}) Object ab = null;

  @ClassVal("c") Object c = null;

  @ClassVal({"c", "d"}) Object cd = null;

  Object unknown = null;

  void assignToUnknown() {
    unknown = a;
    unknown = ab;
    unknown = c;
    unknown = cd;
  }

  void assignUnknown() {
    // :: error: (assignment)
    a = unknown;
    // :: error: (assignment)
    ab = unknown;
    // :: error: (assignment)
    c = unknown;
    // :: error: (assignment)
    cd = unknown;
  }

  void assignments() {
    // :: error: (assignment)
    a = ab;
    ab = a;
    // :: error: (assignment)
    a = c;
    // :: error: (assignment)
    ab = c;
    // :: error: (assignment)
    ab = cd;
  }
}

class ClassBoundSubtypingTest {
  @ClassBound("a") Object a = null;

  @ClassBound({"a", "b"}) Object ab = null;

  @ClassBound("c") Object c = null;

  @ClassBound({"c", "d"}) Object cd = null;

  Object unknown = null;

  void assignToUnknown() {
    unknown = a;
    unknown = ab;
    unknown = c;
    unknown = cd;
  }

  void assignUnknown() {
    // :: error: (assignment)
    a = unknown;
    // :: error: (assignment)
    ab = unknown;
    // :: error: (assignment)
    c = unknown;
    // :: error: (assignment)
    cd = unknown;
  }

  void assignments() {
    // :: error: (assignment)
    a = ab;
    ab = a;
    // :: error: (assignment)
    a = c;
    // :: error: (assignment)
    ab = c;
    // :: error: (assignment)
    ab = cd;
  }
}

class ClassValClassBoundSubtypingTest {
  @ClassVal("a") Object a = null;

  @ClassVal({"a", "b"}) Object ab = null;

  @ClassBound("a") Object aBound = null;

  @ClassBound({"a", "b"}) Object abBound = null;

  void assignments1() {
    // :: error: (assignment)
    a = aBound;
    // :: error: (assignment)
    ab = aBound;
    // :: error: (assignment)
    a = abBound;
    // :: error: (assignment)
    ab = abBound;
  }

  void assignments2() {
    aBound = a;
    // :: error: (assignment)
    aBound = ab;

    abBound = a;
    abBound = ab;
  }
}
