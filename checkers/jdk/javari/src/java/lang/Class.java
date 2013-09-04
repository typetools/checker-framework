package java.lang;
import checkers.javari.quals.*;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.InvocationTargetException;
import java.lang.ref.SoftReference;
import java.io.InputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.lang.annotation.Annotation;

public final @ReadOnly class Class<T> implements java.io.Serializable,
                  java.lang.reflect.GenericDeclaration,
                  java.lang.reflect.Type,
                              java.lang.reflect.AnnotatedElement {
    private static final long serialVersionUID = 0L;                                 
    private Class() { throw new RuntimeException("skeleton method"); }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public static Class<?> forName(String name, boolean initialize,
                   ClassLoader loader)
        throws ClassNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public T newInstance()
        throws InstantiationException, IllegalAccessException
    {
        throw new RuntimeException("skeleton method");
    }

    public native boolean isInstance(@ReadOnly Object obj);
    public native boolean isAssignableFrom(Class<?> cls);
    public native boolean isInterface();
    public native boolean isArray();
    public native boolean isPrimitive();

    public boolean isAnnotation() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isSynthetic() {
        throw new RuntimeException("skeleton method");
    }

    public String getName() {
        throw new RuntimeException("skeleton method");
    }

    public ClassLoader getClassLoader() {
        throw new RuntimeException("skeleton method");
    }

    public TypeVariable<Class<T>>[] getTypeParameters() {
        throw new RuntimeException("skeleton method");
    }

    public native Class<? super T> getSuperclass();

    public Type getGenericSuperclass() {
        throw new RuntimeException("skeleton method");
    }

    public Package getPackage() {
        throw new RuntimeException("skeleton method");
    }

    public native Class<?>[] getInterfaces();

    public Type[] getGenericInterfaces() {
        throw new RuntimeException("skeleton method");
    }

    public native Class<?> getComponentType();
    public native int getModifiers();
    public native Object[] getSigners();

    public Method getEnclosingMethod() {
        throw new RuntimeException("skeleton method");
    }

    public Constructor<?> getEnclosingConstructor() {
        throw new RuntimeException("skeleton method");
    }

    public native Class<?> getDeclaringClass();

    public Class<?> getEnclosingClass() {
        throw new RuntimeException("skeleton method");
    }

    public String getSimpleName() {
        throw new RuntimeException("skeleton method");
    }

    public String getCanonicalName() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isAnonymousClass() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isLocalClass() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isMemberClass() {
        throw new RuntimeException("skeleton method");
    }

    public Class<?>[] getClasses() {
        throw new RuntimeException("skeleton method");
    }

    public Field[] getFields() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Method[] getMethods() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Constructor<?>[] getConstructors() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Field getField( String name)
        throws NoSuchFieldException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Method getMethod( String name,  Class<?> ... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Constructor<T> getConstructor( Class<?> ... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Class<?>[] getDeclaredClasses() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Field[] getDeclaredFields() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Method[] getDeclaredMethods() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Field getDeclaredField(String name)
        throws NoSuchFieldException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Method getDeclaredMethod(String name, Class<?> ... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public Constructor<T> getDeclaredConstructor(Class<?> ... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public InputStream getResourceAsStream(String name) {
        throw new RuntimeException("skeleton method");
    }


    public java.net.URL getResource(String name) {
        throw new RuntimeException("skeleton method");
    }

    public java.security.ProtectionDomain getProtectionDomain() {
        throw new RuntimeException("skeleton method");
    }

    public boolean desiredAssertionStatus() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isEnum() {
        throw new RuntimeException("skeleton method");
    }

    public T[] getEnumConstants() {
        throw new RuntimeException("skeleton method");
    }

    public @PolyRead T cast(@PolyRead Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        throw new RuntimeException("skeleton method");
    }

    public <A extends Annotation> A getAnnotation( Class<A> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public boolean isAnnotationPresent(
         Class<? extends Annotation> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getAnnotations() {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getDeclaredAnnotations()  {
        throw new RuntimeException("skeleton method");
    }
}
