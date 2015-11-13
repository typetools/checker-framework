package org.checkerframework.framework.qual;

import java.lang.annotation.*;

/**
 * This meta-annotation is deprecated.
 * <p>
 *
 * An annotation will no longer use this meta-annotation. To indicate that an
 * annotation is a type qualifier, it should now have the meta-annotation
 * {@code @Target}, with the value of {@link ElementType#TYPE_USE} and
 * optionally the value of {@link ElementType#TYPE_PARAMETER}. Annotations that
 * have other {@link ElementType} values will not be automatically supported by
 * a checker, but can be included in a checker's set of supported qualifiers by
 * explicitly listing it in code. See
 * {@link org.checkerframework.framework.type.AnnotatedTypeFactory#createSupportedTypeQualifiers()
 * AnnotatedTypeFactory#createSupportedTypeQualifiers()}
 *
 * @see TypeQualifiers
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeQualifier {

}
