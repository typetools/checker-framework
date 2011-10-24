package java.lang.reflect;

import java.lang.annotation.Annotation;
import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")
public final class Constructor<T extends @Nullable Object> extends AccessibleObject implements GenericDeclaration, Member {
	public Class<T> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
	public String getName() { throw new RuntimeException("skeleton method"); }
	public int getModifiers() { throw new RuntimeException("skeleton method"); }
	public TypeVariable<Constructor<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
	public Class<?>[] getParameterTypes() { throw new RuntimeException("skeleton method"); }
	public Type[] getGenericParameterTypes() { throw new RuntimeException("skeleton method"); }
	public Class<?>[] getExceptionTypes() { throw new RuntimeException("skeleton method"); }
	public Type[] getGenericExceptionTypes() { throw new RuntimeException("skeleton method"); }
	public boolean equals(Object arg0) { throw new RuntimeException("skeleton method"); }
	public int hashCode() { throw new RuntimeException("skeleton method"); }
    public String toString() { throw new RuntimeException("skeleton method"); }
    public String toGenericString() { throw new RuntimeException("skeleton method"); }
    public @NonNull T newInstance(Object[] arg0) throws InstantiationException,IllegalAccessException,IllegalArgumentException,InvocationTargetException { throw new RuntimeException("skeleton method"); }
    public boolean isVarArgs() { throw new RuntimeException("skeleton method"); }
    public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
    public <T extends Annotation> T getAnnotation(Class<T> arg0) { throw new RuntimeException("skeleton method"); }
    public Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
    public Annotation[][] getParameterAnnotations() { throw new RuntimeException("skeleton method"); }
}
