package java.lang.reflect;

import dataflow.quals.Pure;
import dataflow.quals.SideEffectFree;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.Raw;
import checkers.initialization.quals.UnknownInitialization;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  public Class<?> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public int getModifiers() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEnumConstant() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public Class<? /*extends @NonNull Object*/> getType() { throw new RuntimeException("skeleton method"); }
  public Type getGenericType() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object obj) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  public String toGenericString() { throw new RuntimeException("skeleton method"); }
  public @Nullable Object get(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean getBoolean(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public byte getByte(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public char getChar(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public short getShort(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public int getInt(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public long getLong(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public float getFloat(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public double getDouble(@UnknownInitialization @Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(@UnknownInitialization @Raw @Nullable Object obj, @Nullable Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(@UnknownInitialization @Raw @Nullable Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(@UnknownInitialization @Raw @Nullable Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(@UnknownInitialization @Raw @Nullable Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(@UnknownInitialization @Raw @Nullable Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(@UnknownInitialization @Raw @Nullable Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(@UnknownInitialization @Raw @Nullable Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(@UnknownInitialization @Raw @Nullable Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(@UnknownInitialization @Raw @Nullable Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public <T extends java.lang.annotation. @Nullable Annotation> @Nullable T getAnnotation(Class<@NonNull T> obj) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
