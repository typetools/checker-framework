package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated qualifier is the default qualifier in the qualifier hierarchy: it
 * applies if the programmer writes no explicit qualifier and no other default has been specified
 * for the location.
 *
 * <p>Other defaults can be specified for a checker via the {@link DefaultFor} meta-annotation,
 * which takes precedence over {@code DefaultQualifierInHierarchy}, or via {@code
 * GenericAnnotatedTypeFactory.addCheckedCodeDefaults()}. Also, the CLIMB-to-top rule applies unless
 * explicitly overruled.
 *
 * <p>The {@link DefaultQualifier} annotation, which targets Java code elements, takes precedence
 * over {@code DefaultQualifierInHierarchy}.
 *
 * <p>Each type qualifier hierarchy may have at most one qualifier marked as {@code
 * DefaultQualifierInHierarchy}.
 *
 * @checker_framework.manual #effective-qualifier The effective qualifier on a type (defaults and
 *     inference)
 * @checker_framework.manual #defaults Default qualifiers for unannotated types
 * @see org.checkerframework.framework.qual.DefaultQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultQualifierInHierarchy {}
