// General test cases for compound assignments
// Also test case for Issue 624
// https://github.com/typetools/checker-framework/issues/624

import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class CompoundAssignment {

  @StringVal("hello") String field;

  public void refinements() {
    field = "hello";
    // :: error: (compound.assignment.type.incompatible)
    field += method();
    // :: error: (assignment.type.incompatible)
    // :: error: (compound.assignment.type.incompatible)
    @StringVal("hellohellohello") String test = field += method();
  }

  @StringVal("hello") String method() {
    // :: error: (assignment.type.incompatible)
    field = "goodbye";
    return "hello";
  }

  void value() {
    @StringVal("hello") String s = "hello";
    // :: error: (compound.assignment.type.incompatible)
    s += "hello";

    @IntVal(1) int i = 1;
    // :: error: (compound.assignment.type.incompatible)
    i += 1;

    @IntVal(2) int j = 2;
    // :: error: (compound.assignment.type.incompatible)
    j += 2;

    // :: error: (assignment.type.incompatible)
    @IntVal(4) int four = j;
  }

  void value2() {
    @StringVal("hello") String s = "hello";
    // :: error: (assignment.type.incompatible)
    s = s + "hello";

    @IntVal(1) int i = 1;
    // :: error: (assignment.type.incompatible)
    i = i + 1;
  }

  void noErrorCompoundAssignments() {
    @IntVal(0) int zero = 0;
    zero *= 12;

    @StringVal("null") String s = "null";
    s += "";
  }

  void errorCompundAssignments() {
    @StringVal("hello") String s = "hello";
    // :: error: (compound.assignment.type.incompatible)
    s += "";
  }
}
