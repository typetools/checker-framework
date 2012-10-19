package java.lang.reflect;

import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;

public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  public Class<?> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public int getModifiers() { throw new RuntimeException("skeleton method"); }
  public boolean isEnumConstant() { throw new RuntimeException("skeleton method"); }
  public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public Class<? /*extends @NonNull Object*/> getType() { throw new RuntimeException("skeleton method"); }
  public Type getGenericType() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public String toGenericString() { throw new RuntimeException("skeleton method"); }
  public @Nullable Object get(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean getBoolean(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public byte getByte(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public char getChar(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public short getShort(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public int getInt(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public long getLong(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public float getFloat(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public double getDouble(@Nullable Object a1) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void set(@Nullable Object a1, @Nullable Object a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(@Nullable Object a1, boolean a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(@Nullable Object a1, byte a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(@Nullable Object a1, char a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(@Nullable Object a1, short a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(@Nullable Object a1, int a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(@Nullable Object a1, long a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(@Nullable Object a1, float a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(@Nullable Object a1, double a2) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public <T extends @Nullable java.lang.annotation.Annotation> T getAnnotation(Class<@NonNull T> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
