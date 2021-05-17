import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.InternedDistinct;

public class Distinct {

  class Foo {}

  Foo f1;
  Foo f2;
  @Interned Foo i1;
  @Interned Foo i2;
  @InternedDistinct Foo d1;
  @InternedDistinct Foo d2;

  public void testEquals() {
    // :: error: not.interned
    if (f1 == f2) {}
    // :: error: not.interned
    if (f1 == i2) {}
    if (f1 == d2) {}
    // :: error: not.interned
    if (i1 == f2) {}
    if (i1 == i2) {}
    if (i1 == d2) {}
    if (d1 == f2) {}
    if (d1 == i2) {}
    if (d1 == d2) {}
  }

  public void testAssignment1() {
    f1 = f2;
  }

  public void testAssignment2() {
    f1 = i2;
  }

  public void testAssignment3() {
    f1 = d2;
  }

  public void testAssignment4() {
    // :: error: assignment
    i1 = f2;
  }

  public void testAssignment5() {
    i1 = i2;
  }

  public void testAssignment6() {
    i1 = d2;
  }

  public void testAssignment7() {
    // :: error: assignment
    d1 = f2;
  }

  public void testAssignment8() {
    // :: error: assignment
    d1 = i2;
  }

  public void testAssignment9() {
    d1 = d2;
  }
}
