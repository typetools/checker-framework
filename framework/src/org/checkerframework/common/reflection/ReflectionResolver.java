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
 * @checker_framework.manual #reflection-resolution Reflection resolution
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
     * Determines whether the given tree represents a reflective method or
     * constructor call.
     *
     * @return {@code true} iff tree is a reflective method invocation,
     *         {@code false} otherwise
     */
    public boolean isReflectiveMethodInvocation(MethodInvocationTree tree);

    /**
     * Resolve reflection and return the result of
     * {@code factory.methodFromUse} for the actual, resolved method or
     * constructor call. If the reflective method cannot be resolved the
     * original result ({@code origResult}) is returned.
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
