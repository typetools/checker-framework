// General test cases for compound assignments
// Also test case for Issue 624
// https://github.com/typetools/checker-framework/issues/624

import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class CompoundAssignment {

  @StringVal("hello") String field;

  public void refinements() {
    field = "hello";
    // :: error: (compound.assignment)
    field += method();
    // :: error: (assignment)
    // :: error: (compound.assignment)
    @StringVal("hellohellohello") String test = field += method();
  }

  @StringVal("hello") String method() {
    // :: error: (assignment)
    field = "goodbye";
    return "hello";
  }

  void value() {
    @StringVal("hello") String s = "hello";
    // :: error: (compound.assignment)
    s += "hello";

    @IntVal(1) int i = 1;
    // :: error: (compound.assignment)
    i += 1;

    @IntVal(2) int j = 2;
    // :: error: (compound.assignment)
    j += 2;

    // :: error: (assignment)
    @IntVal(4) int four = j;
  }

  void value2() {
    @StringVal("hello") String s = "hello";
    // :: error: (assignment)
    s = s + "hello";

    @IntVal(1) int i = 1;
    // :: error: (assignment)
    i = i + 1;
  }

  @IntRange(from = 5, to = 10) int afield;

  void afield() {
    if (afield == 5) {
      afield += 5;
    }
    // :: error: (compound.assignment)
    afield += 2;
  }

  void aparam(@IntRange(from = 5, to = 10) int aparam) {
    if (aparam == 5) {
      aparam += 5;
    }
    // :: error: (compound.assignment)
    aparam += 2;
  }

  void alocal() {
    @IntRange(from = 5, to = 10) int alocal;
    if (this.hashCode() > 100) {
      alocal = 5;
    } else {
      alocal = 10;
    }

    if (alocal == 5) {
      alocal += 5;
    }
    // :: error: (compound.assignment)
    alocal += 2;
  }

  void noErrorCompoundAssignments() {
    @IntVal(0) int zero = 0;
    zero *= 12;

    @StringVal("null") String s = "null";
    s += "";
  }

  void errorCompundAssignments() {
    @StringVal("hello") String s = "hello";
    // :: error: (compound.assignment)
    s += "";
  }
}
