package org.checkerframework.common.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.Pair;

import com.sun.source.tree.MethodInvocationTree;

/**
 * Interface for reflection resolvers that handle reflective method calls such
 * as {@link Method#invoke(Object, Object...)} or
 * {@link Constructor#newInstance(Object...)}.
 * 
 * @author rjust
 * 
 */
public interface ReflectionResolver {
    /**
     * Constant for "method name" of constructors
     */
    public static final String INIT = "<init>";

    /**
     * Determines whether reflection resolution has been enabled and whether the
     * given tree represents a reflective method or constructor call and
     * therefore should be resolved.
     * 
     * @return <code>true</code> iff reflection should be resolved,
     *         <code>false</code> otherwise
     */
    public boolean shouldResolveReflection(MethodInvocationTree tree);

    /**
     * Resolve reflection and return the result of
     * <code>factory.methodFromUse</code> for the actual, resolved method or
     * constructor call. If the reflective method cannot be resolved the
     * original result (<code>origResult</code>) is returned.
     * 
     * @param factory
     *            the currently used AnnotatedTypeFactory
     * @param tree
     *            the reflective invocation tree (m.invoke or c.newInstance)
     * @param origResult
     *            the original result for the unresolved, reflective method call
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolveReflectiveCall(
            AnnotatedTypeFactory factory, MethodInvocationTree tree,
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> origResult);
}
