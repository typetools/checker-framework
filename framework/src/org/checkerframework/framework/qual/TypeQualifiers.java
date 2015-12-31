package org.checkerframework.framework.qual;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.qualframework.base.Checker;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This meta-annotation is deprecated.
 * <p>
 *
 * This meta-annotation was used to indicate a list of supported qualifiers of a
 * checker.
 *
 * Instead, each checker should either place its qualifiers within a
 * <tt>qual</tt> subdirectory that's located directly in the same directory as
 * the {@code Checker}, or override
 * {@link org.checkerframework.framework.type.AnnotatedTypeFactory#createSupportedTypeQualifiers()}
 * to explicitly list the qualifiers it supports.
 *
 * @see org.checkerframework.framework.type.AnnotatedTypeFactory#createSupportedTypeQualifiers()
 * @checker_framework.manual #indicating-supported-annotations Indicating supported annotations
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface TypeQualifiers {
    /**
     * The type qualifier annotations supported by the annotated {@link Checker}.
     * The checker may also support other, non-type-qualifier, annotations.
     */
    Class<? extends Annotation>[] value();
}
