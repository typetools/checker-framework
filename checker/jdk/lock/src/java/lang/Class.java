package java.lang;

import org.checkerframework.checker.lock.qual.*;




public final class Class<T extends Object> extends Object implements java.io.Serializable, java.lang.reflect.GenericDeclaration, java.lang.reflect.Type, java.lang.reflect.AnnotatedElement {
  private static final long serialVersionUID = 0;
  protected Class() {}
  @Override
 public String toString(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
  public static Class<?> forName(String a1) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public static Class<?> forName(String a1, boolean a2, ClassLoader a3) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public T newInstance() throws InstantiationException, IllegalAccessException { throw new RuntimeException("skeleton method"); }
   public boolean isAnnotation(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
   public native boolean isInstance(@GuardSatisfied Class<T> this,Object a1);
   public boolean isSynthetic(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public ClassLoader getClassLoader() { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.reflect.TypeVariable<Class<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. Type getGenericSuperclass() { throw new RuntimeException("skeleton method"); }
   public Package getPackage(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Type[] getGenericInterfaces() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. Method getEnclosingMethod() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect. Constructor<?> getEnclosingConstructor() { throw new RuntimeException("skeleton method"); }
  public Class<?> getEnclosingClass() { throw new RuntimeException("skeleton method"); }
  public String getSimpleName() { throw new RuntimeException("skeleton method"); }
   public native Class<? super T> getSuperclass(@GuardSatisfied Class<T> this);
   public native Class<?>[] getInterfaces(@GuardSatisfied Class<T> this);
  public String getCanonicalName() { throw new RuntimeException("skeleton method"); }
   public boolean isAnonymousClass(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
   public boolean isLocalClass(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
   public boolean isMemberClass(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
  public Class<?>[] getClasses() { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getField(String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getMethod(String a1, Class<?> ... a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getConstructor(Class<?>... a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public Class<?>[] getDeclaredClasses() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field[] getDeclaredFields() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method[] getDeclaredMethods() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<?>[] getDeclaredConstructors() throws SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Field getDeclaredField(String a1) throws NoSuchFieldException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Method getDeclaredMethod(String a1, Class<?>... a2) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.lang.reflect.Constructor<T> getDeclaredConstructor(Class<?>... a1) throws NoSuchMethodException, SecurityException { throw new RuntimeException("skeleton method"); }
  public java.io. InputStream getResourceAsStream(String a1) { throw new RuntimeException("skeleton method"); }
  public java.net. URL getResource(String a1) { throw new RuntimeException("skeleton method"); }
  public java.security.ProtectionDomain getProtectionDomain() { throw new RuntimeException("skeleton method"); }
  public boolean desiredAssertionStatus() { throw new RuntimeException("skeleton method"); }
   public boolean isEnum(@GuardSatisfied Class<T> this) { throw new RuntimeException("skeleton method"); }
  public T [] getEnumConstants() { throw new RuntimeException("skeleton method"); }
  java.util.Map<String, T> enumConstantDirectory() { throw new RuntimeException("skeleton method"); }
  public T cast(Object a1) { throw new RuntimeException("skeleton method"); }
  public <U> Class<? extends U> asSubclass(Class<U> a1) { throw new RuntimeException("skeleton method"); }
  @Override
public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> a1) { throw new RuntimeException("skeleton method"); }
  @Override
 public boolean isAnnotationPresent(@GuardSatisfied Class<T> this,@GuardSatisfied Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  @Override
public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
   public native Class<?> getComponentType(@GuardSatisfied Class<T> this);
  public native Object [] getSigners();
  public native Class<?> getDeclaringClass();
   public native boolean isPrimitive(@GuardSatisfied Class<T> this);

   public native boolean isArray(@GuardSatisfied Class<T> this);
   public native boolean isAssignableFrom(@GuardSatisfied Class<T> this,Class<?> cls);
   public native boolean isInterface(@GuardSatisfied Class<T> this);
   public native int getModifiers(@GuardSatisfied Class<T> this);

  // public boolean isTypeAnnotationPresent(@GuardSatisfied Class<T> this,@GuardSatisfied Class<T><? extends java.lang.annotation.Annotation> annotationClass) { throw new RuntimeException("skeleton method"); }
  //public <M extends java.lang.annotation.Annotation> M getTypeAnnotation(Class<M> annotationClass) { throw new RuntimeException("skeleton method"); }
  //public java.lang.annotation.Annotation[] getTypeAnnotations() { throw new RuntimeException("skeleton method"); }
  //public java.lang.annotation.Annotation[] getDeclaredTypeAnnotations() { throw new RuntimeException("skeleton method"); }
}
