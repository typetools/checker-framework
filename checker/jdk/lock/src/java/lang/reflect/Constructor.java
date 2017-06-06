package java.lang.reflect;

import java.lang.annotation.Annotation;

import org.checkerframework.checker.lock.qual.*;

public final class Constructor<T> extends AccessibleObject implements GenericDeclaration, Member {
    public Class<T> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
    public String getName() { throw new RuntimeException("skeleton method"); }
    public int getModifiers() { throw new RuntimeException("skeleton method"); }
    public TypeVariable<Constructor<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
    public Class<?>[] getParameterTypes() { throw new RuntimeException("skeleton method"); }
    public Type[] getGenericParameterTypes() { throw new RuntimeException("skeleton method"); }
    public Class<?>[] getExceptionTypes() { throw new RuntimeException("skeleton method"); }
    public Type[] getGenericExceptionTypes() { throw new RuntimeException("skeleton method"); }
     public boolean equals(@GuardSatisfied Constructor<T> this,@GuardSatisfied Object arg0) { throw new RuntimeException("skeleton method"); }
     public int hashCode(@GuardSatisfied Constructor<T> this) { throw new RuntimeException("skeleton method"); }
     public String toString(@GuardSatisfied Constructor<T> this) { throw new RuntimeException("skeleton method"); }
    public String toGenericString() { throw new RuntimeException("skeleton method"); }
    public T newInstance(Object ... initargs) throws InstantiationException,IllegalAccessException,IllegalArgumentException,InvocationTargetException { throw new RuntimeException("skeleton method"); }
     public boolean isVarArgs(@GuardSatisfied Constructor<T> this) { throw new RuntimeException("skeleton method"); }
     public boolean isSynthetic(@GuardSatisfied Constructor<T> this) { throw new RuntimeException("skeleton method"); }
    public <T extends Annotation> T getAnnotation(Class<T> arg0) { throw new RuntimeException("skeleton method"); }
    public Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
    public Annotation[][] getParameterAnnotations() { throw new RuntimeException("skeleton method"); }
}
