package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a SourceChecker subclass to specify types of interest to the checker. If a
 * checker is not annotated with this annotation, then the checker is interested in all types.
 *
 * <p>{@code Object[].class} implies that the checker is interested in all array types; all other
 * array classes are ignored. A boxed type, such as {@code Integer.class}, implies that the checker
 * is interested in both the boxed type {@code Integer}, and the unboxed primitive type {@code int}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RelevantJavaTypes {
    /**
     * Classes that are relevant to the checker. {@code Object[].class} implies that the checker is
     * interested in all array types; all other array classes are ignored. A boxed type, such as
     * {@code Integer.class}, implies that the checker is interested in both the boxed type {@code
     * Integer}, and the unboxed primitive type {@code int}.
     */
    Class<?>[] value();
}
