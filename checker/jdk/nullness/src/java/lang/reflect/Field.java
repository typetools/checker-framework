package java.lang.reflect;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// In general, the field value `get` methods should take a top-qualified `obj` parameter
// and have a top-qualified return type; the field value `set` methods should take a
// top-qualified `obj` parameter and a bottom-qualified `value` parameter.
//
// nullness: the `obj` parameter in `get`/`set` methods is @NonNull, because instance fields
// require a receiver. Static field accesses need to suppress the errors.
//
// initialization: using fully-initialized types should make the typical use case easier.
public final class Field extends AccessibleObject implements Member {
  protected Field() {}
  @SideEffectFree public Class<?> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String getName() { throw new RuntimeException("skeleton method"); }
  @Pure public int getModifiers() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEnumConstant() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Class<?> getType() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Type getGenericType() { throw new RuntimeException("skeleton method"); }
  @Pure
  public boolean equals(@Nullable Object obj) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toGenericString() { throw new RuntimeException("skeleton method"); }
  // The `obj` formal parameters can be null if the field is static, or must be non-null if the
  // field is an instance field. We don't know which. To prevent a possible NullPointerException,
  // the Nullness Checker conservatively issues a warning whenever null is passed.
  @SideEffectFree public @Nullable Object get(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  // It is OK to set a value in a possibly not fully-initialized `obj` parameter
  public void set(@UnknownInitialization Object obj, Object value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setBoolean(@UnknownInitialization Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setByte(@UnknownInitialization Object obj, byte value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setChar(@UnknownInitialization Object obj, char value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setShort(@UnknownInitialization Object obj, short value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setInt(@UnknownInitialization Object obj, int value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setLong(@UnknownInitialization Object obj, long value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setFloat(@UnknownInitialization Object obj, float value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public void setDouble(@UnknownInitialization Object obj, double value) throws IllegalArgumentException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public <T extends java.lang.annotation.Annotation> @Nullable T getAnnotation(Class<@NonNull T> obj) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
