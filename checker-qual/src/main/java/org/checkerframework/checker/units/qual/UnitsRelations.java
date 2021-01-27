package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specify the class that knows how to handle the meta-annotated unit when put in relation (plus,
 * multiply, ...) with another unit. That class is a subtype of interface {@link
 * org.checkerframework.checker.units.UnitsRelations}.
 *
 * @see org.checkerframework.checker.units.UnitsRelations
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitsRelations {
    /**
     * Returns the subclass of {@link org.checkerframework.checker.units.UnitsRelations} to use.
     *
     * @return the subclass of {@link org.checkerframework.checker.units.UnitsRelations} to use
     */
    // The more precise type is Class<? extends org.checkerframework.checker.units.UnitsRelations>,
    // but org.checkerframework.checker.units.UnitsRelations is not in checker-qual.jar, nor can
    // it be since it uses AnnotatedTypeMirrors.  So this declaration uses a less precise type, and
    // UnitsAnnotatedTypeFactory checks that the argument implements
    // org.checkerframework.checker.units.UnitsRelations.
    Class<?> value();
}
