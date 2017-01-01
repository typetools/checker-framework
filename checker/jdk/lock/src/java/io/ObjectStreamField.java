package java.io;

import org.checkerframework.checker.lock.qual.*;


public class ObjectStreamField implements Comparable<Object> {
  public ObjectStreamField(String a1, Class<?> a2) { throw new RuntimeException("skeleton method"); }
  public ObjectStreamField(String a1, Class<?> a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public Class<?> getType() { throw new RuntimeException("skeleton method"); }
  public char getTypeCode() { throw new RuntimeException("skeleton method"); }
  public String getTypeString() { throw new RuntimeException("skeleton method"); }
  public int getOffset() { throw new RuntimeException("skeleton method"); }
   public boolean isPrimitive(@GuardSatisfied ObjectStreamField this) { throw new RuntimeException("skeleton method"); }
   public boolean isUnshared(@GuardSatisfied ObjectStreamField this) { throw new RuntimeException("skeleton method"); }
   public int compareTo(@GuardSatisfied ObjectStreamField this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied ObjectStreamField this) { throw new RuntimeException("skeleton method"); }
}
