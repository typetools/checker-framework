package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ObjectStreamField implements Comparable<Object> {
  public ObjectStreamField(String a1, Class<?> a2) { throw new RuntimeException("skeleton method"); }
  public ObjectStreamField(String a1, Class<?> a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public Class<?> getType() { throw new RuntimeException("skeleton method"); }
  public char getTypeCode() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getTypeString() { throw new RuntimeException("skeleton method"); }
  public int getOffset() { throw new RuntimeException("skeleton method"); }
  public boolean isPrimitive() { throw new RuntimeException("skeleton method"); }
  public boolean isUnshared() { throw new RuntimeException("skeleton method"); }
  public int compareTo(Object a1) { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
