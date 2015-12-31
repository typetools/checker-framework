package org.checkerframework.framework.qual;

import java.lang.annotation.*;

/**
 * This meta-annotation is deprecated.
 * <p>
 *
 * An annotation will no longer use this meta-annotation. To indicate that an
 * annotation is a type qualifier, see the Checker Framework manual for details
 *
 * @see org.checkerframework.framework.type.AnnotatedTypeFactory#createSupportedTypeQualifiers()
 * @checker_framework.manual #indicating-supported-annotations Indicating supported annotations
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeQualifier {

}
