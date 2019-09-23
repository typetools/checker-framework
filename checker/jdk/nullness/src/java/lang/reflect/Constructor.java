package java.lang.reflect;

import org.checkerframework.framework.qual.Covariant;
import java.lang.annotation.Annotation;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// The type argument to Constructor is meaningless.
// Constructor<@NonNull String> and Constructor<@Nullable String> have the same
// meaning, but are unrelated by the Java type hierarchy.
// @Covariant makes Constructor<@NonNull String> a subtype of Constructor<@Nullable String>.
@Covariant(0)
public final class Constructor<T> extends AccessibleObject implements GenericDeclaration, Member {
    public Class<T> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
    public String getName() { throw new RuntimeException("skeleton method"); }
    public int getModifiers() { throw new RuntimeException("skeleton method"); }
    public TypeVariable<Constructor<T>>[] getTypeParameters() { throw new RuntimeException("skeleton method"); }
    public Class<?>[] getParameterTypes() { throw new RuntimeException("skeleton method"); }
    public Type[] getGenericParameterTypes() { throw new RuntimeException("skeleton method"); }
    public Class<?>[] getExceptionTypes() { throw new RuntimeException("skeleton method"); }
    public Type[] getGenericExceptionTypes() { throw new RuntimeException("skeleton method"); }
    @Pure public boolean equals(@Nullable Object arg0) { throw new RuntimeException("skeleton method"); }
    @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
    @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
    public String toGenericString() { throw new RuntimeException("skeleton method"); }
    public @NonNull T newInstance(Object ... initargs) throws InstantiationException,IllegalAccessException,IllegalArgumentException,InvocationTargetException { throw new RuntimeException("skeleton method"); }
    @Pure public boolean isVarArgs() { throw new RuntimeException("skeleton method"); }
    @Pure public boolean isSynthetic() { throw new RuntimeException("skeleton method"); }
    public <T extends @Nullable Annotation> @Nullable T getAnnotation(Class<T> arg0) { throw new RuntimeException("skeleton method"); }
    public Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
    public Annotation[][] getParameterAnnotations() { throw new RuntimeException("skeleton method"); }
}
