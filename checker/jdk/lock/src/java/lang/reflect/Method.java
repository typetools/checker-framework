package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.nio.ByteBuffer;
import java.util.Map;

import org.checkerframework.checker.lock.qual.*;

@SuppressWarnings("rawtypes")
public final
    class Method extends AccessibleObject implements GenericDeclaration, Member {
    Method(Class declaringClass,
           String name,
           Class[] parameterTypes,
           Class returnType,
           Class[] checkedExceptions,
           int modifiers,
           int slot,
           String signature,
           byte[] annotations,
           byte[] parameterAnnotations,
           byte[] annotationDefault)
    {
        throw new RuntimeException("skeleton method");
    }


    Method copy() {
        throw new RuntimeException("skeleton method");
    }


    public Class<?> getDeclaringClass() {
        throw new RuntimeException("skeleton method");
    }


    public String getName() {
        throw new RuntimeException("skeleton method");
    }


    public int getModifiers() {
        throw new RuntimeException("skeleton method");
    }


    public TypeVariable<Method>[] getTypeParameters() {
        throw new RuntimeException("skeleton method");
    }


    // never returns null; returns Void instead
    public Class<?> getReturnType() {
        throw new RuntimeException("skeleton method");
    }

    // never returns null; returns Void instead
    public Type getGenericReturnType() {
        throw new RuntimeException("skeleton method");
    }



    public Class<?>[] getParameterTypes() {
        throw new RuntimeException("skeleton method");
    }


    public Type[] getGenericParameterTypes() {
        throw new RuntimeException("skeleton method");
    }



    public Class<?>[] getExceptionTypes() {
        throw new RuntimeException("skeleton method");
    }


    public Type[] getGenericExceptionTypes() {
        throw new RuntimeException("skeleton method");
    }


     public boolean equals(@GuardSatisfied Method this,@GuardSatisfied Object obj) {
        throw new RuntimeException("skeleton method");
    }


     public int hashCode(@GuardSatisfied Method this) {
        throw new RuntimeException("skeleton method");
    }


     public String toString(@GuardSatisfied Method this) {
        throw new RuntimeException("skeleton method");
    }


    public String toGenericString() {
        throw new RuntimeException("skeleton method");
    }


    // The method being invoked might be one that requires non-null
    // arguments, or might be one that permits null.  We don't know which.
    // Therefore, the Nullness Checker should conservatively issue a
    // warning whenever null is passed, in order to give a guarantee that
    // no nullness-related exception will be thrown by the invoked method.
    public Object invoke(Object obj, Object ... args)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException
    {
        throw new RuntimeException("skeleton method");
    }


     public boolean isBridge(@GuardSatisfied Method this) {
        throw new RuntimeException("skeleton method");
    }


     public boolean isVarArgs(@GuardSatisfied Method this) {
        throw new RuntimeException("skeleton method");
    }


     public boolean isSynthetic(@GuardSatisfied Method this) {
        throw new RuntimeException("skeleton method");
    }


    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getDeclaredAnnotations()  {
        throw new RuntimeException("skeleton method");
    }

    public Object getDefaultValue() {
        throw new RuntimeException("skeleton method");
    }


    public Annotation[][] getParameterAnnotations() {
        throw new RuntimeException("skeleton method");
    }
}
