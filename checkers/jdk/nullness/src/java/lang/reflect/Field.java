package java.lang.reflect;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.Raw;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  public Class<?> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public int getModifiers() { throw new RuntimeException("skeleton method"); }
  public boolean isEnumConstant() { throw new RuntimeException("skeleton method"); }
  public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public Class<? /*extends @NonNull Object*/> getType() { throw new RuntimeException("skeleton method"); }
  public Type getGenericType() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object obj) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public String toGenericString() { throw new RuntimeException("skeleton method"); }
  public @Nullable Object get(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean getBoolean(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public byte getByte(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public char getChar(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public short getShort(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public int getInt(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public long getLong(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public float getFloat(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public double getDouble(@Raw @Nullable Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(@Raw @Nullable Object obj, @Nullable Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(@Raw @Nullable Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(@Raw @Nullable Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(@Raw @Nullable Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(@Raw @Nullable Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(@Raw @Nullable Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(@Raw @Nullable Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(@Raw @Nullable Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(@Raw @Nullable Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public <T extends java.lang.annotation. @Nullable Annotation> @Nullable T getAnnotation(Class<@NonNull T> obj) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
