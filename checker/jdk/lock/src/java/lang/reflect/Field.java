package java.lang.reflect;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  @SideEffectFree public Class<?> getDeclaringClass(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String getName(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @Pure public int getModifiers(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEnumConstant(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSynthetic(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Class<? /*extends Object*/> getType(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Type getGenericType(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@GuardSatisfied Field this,@GuardSatisfied Object obj) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toGenericString(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object get(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public boolean getBoolean(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public byte getByte(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public char getChar(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public short getShort(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public int getInt(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public long getLong(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public float getFloat(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public double getDouble(@GuardSatisfied Field this,@GuardSatisfied Object obj) throws IllegalArgumentException, @GuardSatisfied IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public <T extends java.lang.annotation. Annotation> T getAnnotation(@GuardSatisfied Field this,Class<T> obj) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public java.lang.annotation.Annotation[] getDeclaredAnnotations(@GuardSatisfied Field this) { throw new RuntimeException("skeleton method"); }
}
