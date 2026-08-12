// Keep in sync with ../../jdk25/inheritDeclAnnoPersist/AbstractClass.java.  The two copies are
// identical: this file uses neither the com.sun.tools.classfile nor the java.lang.classfile API.
// Each of jdk24/ and jdk25/ has its own copy so that it is self-contained.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractClass {
  @Nullable Object f;

  @EnsuresNonNull("f")
  public abstract void setf();

  public abstract void setg();
}
