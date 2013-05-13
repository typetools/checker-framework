package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import checkers.quals.*;

public final class Class<T> implements java.io.Serializable, java.lang.reflect.GenericDeclaration, java.lang.reflect.Type, java.lang.reflect.AnnotatedElement {
  public @NonNull String toString() { throw new RuntimeException("skeleton method"); }
  public static @NonNull Class<?> forName(@NonNull String a1) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public static @NonNull Class<? extends @NonNull Object> forName(@NonNull String a1, boolean a2, @Nullable ClassLoader a3) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public @NonNull T newInstance() throws InstantiationException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotation() { throw new RuntimeException("skeleton method"); }
  public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public @NonNull String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable ClassLoader getClassLoader() { throw new RuntimeException("skeleton method"); }
  public @NonNull TypeVariable<Class<T>> @NonNull [] getTypeParameters() { throw new RuntimeException("skeleton method"); }
  public @Nullable Type getGenericSuperclass() { throw new RuntimeException("skeleton method"); }
  public @Nullable Package getPackage() { throw new RuntimeException("skeleton method"); }
  public @NonNull Type @NonNull [] getGenericInterfaces() { throw new RuntimeException("skeleton method"); }
  public @Nullable Method getEnclosingMethod() { throw new RuntimeException("skeleton method"); }
  public @Nullable Constructor<?> getEnclosingConstructor() { throw new RuntimeException("skeleton method"); }
  public @Nullable Class<?> getEnclosingClass() { throw new RuntimeException("skeleton method"); }
  public @NonNull String getSimpleName() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getCanonicalName() { throw new RuntimeException("skeleton method"); }
  public boolean isAnonymousClass() { throw new RuntimeException("skeleton method"); }
  public boolean isLocalClass() { throw new RuntimeException("skeleton method"); }
  public boolean isMemberClass() { throw new RuntimeException("skeleton method"); }
  public @NonNull Class<?> @NonNull [] getClasses() { throw new RuntimeException("skeleton method"); }
  public @NonNull Field @NonNull [] getFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Method @NonNull [] getMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Constructor<? extends @NonNull Object> @NonNull [] getConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Field getField(@NonNull String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Method getMethod(@NonNull String a1, Class<?>... a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Constructor<T> getConstructor(Class<?>... a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Class<?> @NonNull [] getDeclaredClasses() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Field @NonNull [] getDeclaredFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Method @NonNull [] getDeclaredMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Constructor<?> @NonNull [] getDeclaredConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Field getDeclaredField(@NonNull String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Method getDeclaredMethod(@NonNull String a1, Class<?>[] a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public @NonNull Constructor<T> getDeclaredConstructor(Class<?>[] a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.io. @Nullable InputStream getResourceAsStream(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public java.net. @Nullable URL getResource(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public java.security. @NonNull ProtectionDomain getProtectionDomain() { throw new RuntimeException("skeleton method"); }
  public boolean desiredAssertionStatus() { throw new RuntimeException("skeleton method"); }
  public boolean isEnum() { throw new RuntimeException("skeleton method"); }
  public boolean isPrimitive() { throw new RuntimeException("skeleton method"); }
  public @Nullable T @NonNull [] getEnumConstants() { throw new RuntimeException("skeleton method"); }
  public @Nullable T cast(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull <U> Class<? extends U> asSubclass(@NonNull Class<U> a1) { throw new RuntimeException("skeleton method"); }
  public <A extends Annotation> @Nullable A getAnnotation(@NonNull Class<A> a1) { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotationPresent(@NonNull Class<? extends Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull Annotation @NonNull [] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public @NonNull Annotation @NonNull [] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }

  public boolean isTypeAnnotationPresent(@NonNull Class<? extends Annotation> annotationClass) { throw new RuntimeException("skeleton method"); }
  public <T extends Annotation> T getTypeAnnotation(@NonNull Class<T> annotationClass) { throw new RuntimeException("skeleton method"); }
  public @NonNull Annotation[] getTypeAnnotations() { throw new RuntimeException("skeleton method"); }
  public @NonNull Annotation[] getDeclaredTypeAnnotations() { throw new RuntimeException("skeleton method"); }
}
