package java.lang.reflect;


import org.checkerframework.checker.lock.qual.*;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
   public Class<?> getDeclaringClass(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public String getName(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public int getModifiers(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public boolean isEnumConstant(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public boolean isSynthetic(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public Class<? /*extends Object*/> getType(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public Type getGenericType(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied Field this,@GuardSatisfied Object obj) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public String toGenericString(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
   public Object get(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public boolean getBoolean(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public byte getByte(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public char getChar(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public short getShort(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public int getInt(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public long getLong(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public float getFloat(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public double getDouble(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public <T extends java.lang.annotation. Annotation> T getAnnotation(@GuardSatisfied Field this,Class<T> obj) { throw new RuntimeException("skeleton method"); }
   public java.lang.annotation.Annotation[] getDeclaredAnnotations(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
}
