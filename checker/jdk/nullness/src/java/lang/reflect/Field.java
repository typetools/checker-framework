package java.lang.reflect;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  @SideEffectFree public Class<?> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String getName() { throw new RuntimeException("skeleton method"); }
  @Pure public int getModifiers() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEnumConstant() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Class<?> getType() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Type getGenericType() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object obj) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toGenericString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public @Nullable Object get(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public boolean getBoolean(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public byte getByte(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public char getChar(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public short getShort(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public int getInt(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public long getLong(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public float getFloat(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public double getDouble(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(@UnknownInitialization @Raw @Nullable Object obj, @Nullable Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(@UnknownInitialization @Raw @Nullable Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(@UnknownInitialization @Raw @Nullable Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(@UnknownInitialization @Raw @Nullable Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(@UnknownInitialization @Raw @Nullable Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(@UnknownInitialization @Raw @Nullable Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(@UnknownInitialization @Raw @Nullable Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(@UnknownInitialization @Raw @Nullable Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(@UnknownInitialization @Raw @Nullable Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public <T extends java.lang.annotation. @Nullable Annotation> @Nullable T getAnnotation(Class<@NonNull T> obj) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
