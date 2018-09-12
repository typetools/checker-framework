package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a SourceChecker subclass to specify which Java types are processed by the
 * checker. The checker's type qualifiers may only appear on the given types and their subtypes
 * &mdash; in source code, internally in the compiler, or in class files.
 *
 * <p>If a checker is not annotated with this annotation, then the checker's qualifiers may appear
 * on any type.
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
     * Classes that are processed by the checker.
     *
     * <p>{@code Object[].class} implies that the checker processes all array types. No distinction
     * among array types is currently made, and no other array class should be supplied to
     * {@code @RelevantJavaTypes}.
     *
     * <p>A boxed type, such as {@code Integer.class}, implies that the checker processes both the
     * boxed type {@code Integer} and the unboxed primitive type {@code int}.
     */
    Class<?>[] value();
}
