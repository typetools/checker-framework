// Keep in sync with ../../jdk25/inheritDeclAnnoPersist/Super.java.  The two copies are identical:
// this file uses neither the com.sun.tools.classfile nor the java.lang.classfile API.  Each of
// jdk24/ and jdk25/ has its own copy so that it is self-contained.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Super {
  Object f;
  Object g;
  Object h;

  @EnsuresNonNull("f")
  void setf() {
    f = new Object();
  }

  void setg() {
    g = null;
  }

  @SideEffectFree
  void seth() {
    h = null;
  }
}
