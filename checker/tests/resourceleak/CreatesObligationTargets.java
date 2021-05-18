// A test that errors are correctly issued when re-assignments don't match the
// create obligation annotation on a method.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationTargets {
  @Owning InputStream is1;

  @CreatesObligation
  // :: error: incompatible.creates.obligation
  static void resetObj1(CreatesObligationTargets r) throws Exception {
    if (r.is1 == null) {
      r.is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation("#2")
  // :: error: incompatible.creates.obligation
  static void resetObj2(CreatesObligationTargets r, CreatesObligationTargets other)
      throws Exception {
    if (r.is1 == null) {
      r.is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation("#1")
  static void resetObj3(CreatesObligationTargets r, CreatesObligationTargets other)
      throws Exception {
    if (r.is1 == null) {
      r.is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation
  void resetObj4(CreatesObligationTargets this, CreatesObligationTargets other) throws Exception {
    if (is1 == null) {
      is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation
  // :: error: incompatible.creates.obligation
  void resetObj5(CreatesObligationTargets this, CreatesObligationTargets other) throws Exception {
    if (other.is1 == null) {
      other.is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation("#2")
  // :: error: incompatible.creates.obligation
  void resetObj6(CreatesObligationTargets this, CreatesObligationTargets other) throws Exception {
    if (other.is1 == null) {
      other.is1 = new FileInputStream("foo.txt");
    }
  }

  @CreatesObligation("#1")
  void resetObj7(CreatesObligationTargets this, CreatesObligationTargets other) throws Exception {
    if (other.is1 == null) {
      other.is1 = new FileInputStream("foo.txt");
    }
  }

  @EnsuresCalledMethods(value = "this.is1", methods = "close")
  void a() throws Exception {
    is1.close();
  }
}
