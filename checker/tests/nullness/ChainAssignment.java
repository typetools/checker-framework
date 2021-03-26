import org.checkerframework.checker.nullness.qual.Nullable;

public class ChainAssignment {
  @Nullable Object a;
  @Nullable Object b;
  Object x = new Object();
  Object y = new Object();

  void m1() {
    a = b = new Object();
  }

  void m2() {
    this.a = this.b = new Object();
  }

  void m3() {
    a = this.b = new Object();
  }

  void m4() {
    this.a = b = new Object();
  }

  void n1() {
    // :: error: (assignment.type.incompatible)
    x = y = null;
  }

  void n2() {
    // :: error: (assignment.type.incompatible)
    this.x = this.y = null;
  }

  void n3() {
    // :: error: (assignment.type.incompatible)
    x = this.y = null;
  }

  void n4() {
    // :: error: (assignment.type.incompatible)
    this.x = y = null;
  }
}
