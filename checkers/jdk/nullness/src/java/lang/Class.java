package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Class<T> extends Object implements java.io.Serializable, java.lang.reflect.GenericDeclaration, java.lang.reflect.Type, java.lang.reflect.AnnotatedElement {
  private static final long serialVersionUID = 0;
  protected Class() {}
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public static java.lang.Class<?> forName(java.lang.String a1) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Class<?> forName(java.lang.String a1, boolean a2, @Nullable java.lang.ClassLoader a3) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public T newInstance() throws java.lang.InstantiationException, java.lang.IllegalAccessException { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotation() { throw new RuntimeException("skeleton method"); }
  public native boolean isInstance(@Nullable java.lang.Object a1);
  public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.ClassLoader getClassLoader() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.TypeVariable<java.lang.Class<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Type getGenericSuperclass() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Package getPackage() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Type[] getGenericInterfaces() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Method getEnclosingMethod() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.reflect.Constructor<?> getEnclosingConstructor() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Class<?> getEnclosingClass() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getSimpleName() { throw new RuntimeException("skeleton method"); }
  public native @Pure @Nullable Class<? super T> getSuperclass();
  public native Class<?>[] getInterfaces();
  public @Nullable java.lang.String getCanonicalName() { throw new RuntimeException("skeleton method"); }
  public boolean isAnonymousClass() { throw new RuntimeException("skeleton method"); }
  public boolean isLocalClass() { throw new RuntimeException("skeleton method"); }
  public boolean isMemberClass() { throw new RuntimeException("skeleton method"); }
  public java.lang.Class<?>[] getClasses() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getFields() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getMethods() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getConstructors() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getField(java.lang.String a1) throws java.lang.NoSuchFieldException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getMethod(java.lang.String a1, java.lang.Class<?> @Nullable ... a2) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getConstructor(java.lang.Class<?>... a1) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.Class<?>[] getDeclaredClasses() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getDeclaredFields() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getDeclaredMethods() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getDeclaredConstructors() throws java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getDeclaredField(java.lang.String a1) throws java.lang.NoSuchFieldException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getDeclaredMethod(java.lang.String a1, java.lang.Class<?>... a2) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getDeclaredConstructor(java.lang.Class<?>... a1) throws java.lang.NoSuchMethodException, java.lang.SecurityException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.InputStream getResourceAsStream(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.net.URL getResource(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.security.ProtectionDomain getProtectionDomain() { throw new RuntimeException("skeleton method"); }
  public boolean desiredAssertionStatus() { throw new RuntimeException("skeleton method"); }
  public boolean isEnum() { throw new RuntimeException("skeleton method"); }
  public T @Nullable [] getEnumConstants() { throw new RuntimeException("skeleton method"); }
  java.util.Map<String, T> enumConstantDirectory() { throw new RuntimeException("skeleton method"); }
  public @PolyNull T cast(@PolyNull java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public <U> java.lang.Class<? extends U> asSubclass(java.lang.Class<U> a1) { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(java.lang.Class<A> a1) { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotationPresent(java.lang.Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
  public native @Nullable java.lang.Class<?> getComponentType();
  public native java.lang.Object @Nullable [] getSigners();
  public native @Nullable java.lang.Class<?> getDeclaringClass();
  public native boolean isPrimitive();
  public native boolean isArray();

  public native boolean isAssignableFrom(Class<?> cls);
  public native boolean isInterface();
  public native int getModifiers();

}
