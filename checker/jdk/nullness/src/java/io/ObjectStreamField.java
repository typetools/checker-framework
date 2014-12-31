package java.io;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public class ObjectStreamField implements Comparable<Object> {
  public ObjectStreamField(String a1, Class<?> a2) { throw new RuntimeException("skeleton method"); }
  public ObjectStreamField(String a1, Class<?> a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public Class<?> getType() { throw new RuntimeException("skeleton method"); }
  public char getTypeCode() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getTypeString() { throw new RuntimeException("skeleton method"); }
  public int getOffset() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isPrimitive() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isUnshared() { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
