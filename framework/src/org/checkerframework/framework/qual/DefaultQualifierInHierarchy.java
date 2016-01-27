package org.checkerframework.framework.qual;

import org.checkerframework.framework.util.defaults.QualifierDefaults;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import java.lang.annotation.*;

/**
 * Indicates that the annotated qualifier is the default qualifier in the
 * qualifier hierarchy:  it applies if the programmer writes no explicit
 * qualifier and no other default has been specified for the location.
 * <p>
 *
 * Other defaults can be specified for a checker via the {@link DefaultFor} meta-annotation, which
 * takes precedence over {@code DefaultQualifierInHierarchy}, or via
 * {@link org.checkerframework.framework.type.GenericAnnotatedTypeFactory#addCheckedCodeDefaults(QualifierDefaults)}.
 * <p>
 *
 * The {@link DefaultQualifier} annotation, which targets Java code elements,
 * takes precedence over {@code DefaultQualifierInHierarchy}.
 * <p>
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * {@code DefaultQualifierInHierarchy}.
 *
 * @checker_framework.manual #effective-qualifier The effective qualifier on a type (defaults and inference)
 * @see org.checkerframework.framework.qual.DefaultQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface DefaultQualifierInHierarchy {}
