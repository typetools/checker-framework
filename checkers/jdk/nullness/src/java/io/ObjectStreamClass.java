package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ObjectStreamClass implements Serializable {
  private static final long serialVersionUID = 0;
  protected ObjectStreamClass() {}
  public final static java.io.ObjectStreamField[] NO_FIELDS = {};
  public static @Nullable java.io.ObjectStreamClass lookup(java.lang.Class<?> a1) { throw new RuntimeException("skeleton method"); }
  public static java.io.ObjectStreamClass lookupAny(java.lang.Class<?> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public long getSerialVersionUID() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Class<?> forClass() { throw new RuntimeException("skeleton method"); }
  public java.io.ObjectStreamField[] getFields() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.ObjectStreamField getField(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
