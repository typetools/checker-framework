package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.nio.ByteBuffer;
import java.util.Map;

import checkers.nullness.quals.*;

@SuppressWarnings("rawtypes")
public final
    class Method extends AccessibleObject implements GenericDeclaration,
						     Member {
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


    public @Nullable Type getGenericReturnType() {
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


    public boolean equals(@Nullable Object obj) {
	throw new RuntimeException("skeleton method");
    }


    public int hashCode() {
	throw new RuntimeException("skeleton method");
    }


    public String toString() {
	throw new RuntimeException("skeleton method");
    }


    public String toGenericString() {
	throw new RuntimeException("skeleton method");
    }


    public Object invoke(@Nullable Object obj, @Nullable Object @Nullable ... args)
	throws IllegalAccessException, IllegalArgumentException,
           InvocationTargetException
    {
        throw new RuntimeException("skeleton method");
    }


    public boolean isBridge() {
        throw new RuntimeException("skeleton method");
    }


    public boolean isVarArgs() {
        throw new RuntimeException("skeleton method");
    }


    public boolean isSynthetic() {
        throw new RuntimeException("skeleton method");
    }


    public <T extends Annotation> @Nullable T getAnnotation(Class<T> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getDeclaredAnnotations()  {
        throw new RuntimeException("skeleton method");
    }

    public @Nullable Object getDefaultValue() {
        throw new RuntimeException("skeleton method");
    }


    public Annotation[][] getParameterAnnotations() {
        throw new RuntimeException("skeleton method");
    }
}
