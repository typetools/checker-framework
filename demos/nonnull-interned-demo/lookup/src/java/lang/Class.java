package java.lang;

import checkers.quals.*;

public final class Class<T> implements java.io.Serializable, java.lang.reflect.GenericDeclaration, java.lang.reflect.Type, java.lang.reflect.AnnotatedElement {
  public @NonNull java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public static @NonNull java.lang.Class<?> forName(@NonNull java.lang.String a1) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public static @NonNull java.lang.Class<? extends @NonNull Object> forName(@NonNull java.lang.String a1, boolean a2, @Nullable java.lang.ClassLoader a3) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public @NonNull T newInstance() throws java.lang.InstantiationException, java.lang.IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotation() { throw new RuntimeException("skeleton method"); }
  public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.ClassLoader getClassLoader() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.TypeVariable<java.lang.Class<T>> @NonNull [] getTypeParameters() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Type getGenericSuperclass() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Package getPackage() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Type @NonNull [] getGenericInterfaces() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Method getEnclosingMethod() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Constructor<?> getEnclosingConstructor() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Class<?> getEnclosingClass() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String getSimpleName() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getCanonicalName() { throw new RuntimeException("skeleton method"); }
  public boolean isAnonymousClass() { throw new RuntimeException("skeleton method"); }
  public boolean isLocalClass() { throw new RuntimeException("skeleton method"); }
  public boolean isMemberClass() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.Class<?> @NonNull [] getClasses() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Field @NonNull [] getFields() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Method @NonNull [] getMethods() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Constructor<? extends @NonNull Object> @NonNull [] getConstructors() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Field getField(@NonNull java.lang.String a1) throws java.lang.NoSuchFieldException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Method getMethod(@NonNull java.lang.String a1, java.lang.Class<?>... a2) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Constructor<T> getConstructor(java.lang.Class<?>... a1) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.Class<?> @NonNull [] getDeclaredClasses() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Field @NonNull [] getDeclaredFields() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Method @NonNull [] getDeclaredMethods() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Constructor<?> @NonNull [] getDeclaredConstructors() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Field getDeclaredField(@NonNull java.lang.String a1) throws java.lang.NoSuchFieldException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Method getDeclaredMethod(@NonNull java.lang.String a1, java.lang.Class<?>[] a2) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.reflect.Constructor<T> getDeclaredConstructor(java.lang.Class<?>[] a1) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.InputStream getResourceAsStream(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.net.URL getResource(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.security.ProtectionDomain getProtectionDomain() { throw new RuntimeException("skeleton method"); }
  public boolean desiredAssertionStatus() { throw new RuntimeException("skeleton method"); }
  public boolean isEnum() { throw new RuntimeException("skeleton method"); }
  public boolean isPrimitive() { throw new RuntimeException("skeleton method"); }
  public @Nullable T @NonNull [] getEnumConstants() { throw new RuntimeException("skeleton method"); }
  public @Nullable T cast(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull <U> java.lang.Class<? extends U> asSubclass(@NonNull java.lang.Class<U> a1) { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(@NonNull java.lang.Class<A> a1) { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotationPresent(@NonNull java.lang.Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.annotation.Annotation @NonNull [] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.annotation.Annotation @NonNull [] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
