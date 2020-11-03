package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a SourceChecker subclass to specify which Java types are processed by the
 * checker. In source code, the checker's type qualifiers may only appear on the given types and
 * their subtypes. If a checker is not annotated with this annotation, then the checker's qualifiers
 * may appear on any type.
 *
 * <p>This restriction is coarse-grained in that it applies to all type annotations for a given
 * checker. To have different restrictions for different Java types, override {@code
 * org.checkerframework.common.basetype.BaseTypeVisitor#visitAnnotatedType(List, Tree)}.
 *
 * <p>This is orthogonal to Java's {@code @Target} annotation; each enforces a different type of
 * restriction on what can be written in source code.
 *
 * @checker_framework.manual #creating-relevant-java-types Relevant Java types
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RelevantJavaTypes {
    /**
     * Classes where a type annotation supported by this checker may be written.
     *
     * <p>{@code Object[].class} means that the checker processes all array types. No distinction
     * among array types is currently made, and no other array class should be supplied to
     * {@code @RelevantJavaTypes}.
     *
     * <p>If a checker processes both primitive and boxed types, both must be specified separately,
     * for example as {@code int.class} and {@code Integer.class}.
     *
     * @return classes where a type annotation supported by this checker may be written
     */
    Class<?>[] value();
}
