package java.lang;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public final class Class<T> extends Object implements java.io.Serializable, java.lang.reflect.GenericDeclaration, java.lang.reflect.Type, java.lang.reflect.AnnotatedElement {
  private static final long serialVersionUID = 0;
  protected Class() {}
  @Override
@SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  public static Class<?> forName(String a1) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public static Class<?> forName(String a1, boolean a2, @Nullable ClassLoader a3) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public @NonNull T newInstance() throws InstantiationException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isAnnotation() { throw new RuntimeException("skeleton method"); }
  @Pure public native boolean isInstance(@Nullable Object a1);
  @Pure public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable ClassLoader getClassLoader() { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.reflect.TypeVariable<Class<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. @Nullable Type getGenericSuperclass() { throw new RuntimeException("skeleton method"); }
  @Pure public @Nullable Package getPackage() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Type[] getGenericInterfaces() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. @Nullable Method getEnclosingMethod() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. @Nullable Constructor<?> getEnclosingConstructor() { throw new RuntimeException("skeleton method"); }
  public @Nullable Class<?> getEnclosingClass() { throw new RuntimeException("skeleton method"); }
  public String getSimpleName() { throw new RuntimeException("skeleton method"); }
  @Pure public native @Nullable Class<? super T> getSuperclass();
  @Pure public native Class<?>[] getInterfaces();
  public @Nullable String getCanonicalName() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isAnonymousClass() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isLocalClass() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isMemberClass() { throw new RuntimeException("skeleton method"); }
  public Class<?>[] getClasses() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getField(String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getMethod(String a1, Class<?> @Nullable ... a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getConstructor(Class<?>... a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public Class<?>[] getDeclaredClasses() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getDeclaredFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getDeclaredMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getDeclaredConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getDeclaredField(String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getDeclaredMethod(String a1, Class<?>... a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getDeclaredConstructor(Class<?>... a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.io. @Nullable InputStream getResourceAsStream(String a1) { throw new RuntimeException("skeleton method"); }
  public java.net. @Nullable URL getResource(String a1) { throw new RuntimeException("skeleton method"); }
  public java.security.ProtectionDomain getProtectionDomain() { throw new RuntimeException("skeleton method"); }
  public boolean desiredAssertionStatus() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEnum() { throw new RuntimeException("skeleton method"); }
  public T @Nullable [] getEnumConstants() { throw new RuntimeException("skeleton method"); }
  java.util.Map<String, T> enumConstantDirectory() { throw new RuntimeException("skeleton method"); }
  public @PolyNull T cast(@PolyNull Object a1) { throw new RuntimeException("skeleton method"); }
  public <U> Class<? extends U> asSubclass(Class<U> a1) { throw new RuntimeException("skeleton method"); }
  @Override
public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(Class<A> a1) { throw new RuntimeException("skeleton method"); }
  @Override
@Pure public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
  @Pure public native @Nullable Class<?> getComponentType();
  public native Object @Nullable [] getSigners();
  public native @Nullable Class<?> getDeclaringClass();
  @Pure public native boolean isPrimitive();
  @EnsuresNonNullIf(expression="getComponentType()", result=true)
  @Pure public native boolean isArray();
  @Pure public native boolean isAssignableFrom(Class<?> cls);
  @Pure public native boolean isInterface();
  @Pure public native int getModifiers();

  @Pure public boolean isTypeAnnotationPresent(Class<? extends java.lang.annotation.Annotation> annotationClass) { throw new RuntimeException("skeleton method"); }
  public <M extends java.lang.annotation.Annotation> M getTypeAnnotation(Class<M> annotationClass) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getTypeAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredTypeAnnotations() { throw new RuntimeException("skeleton method"); }
}
