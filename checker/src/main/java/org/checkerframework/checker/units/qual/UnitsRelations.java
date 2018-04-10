package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specify the class that knows how to handle the meta-annotated unit when put in relation (plus,
 * multiply, ...) with another unit.
 *
 * @see org.checkerframework.checker.units.UnitsRelations
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitsRelations {
    /** @return the UnitsRelations subclass to use */
    // The more precise type is Class<? extends org.checkerframework.checker.units.UnitsRelations>,
    // but org.checkerframework.checker.units.UnitsRelations is not in checker-qual.jar, nor can
    // it be since it uses AnnotatedTypeMirrors.  So use a less precise type and check that it
    // is a subclass in UnitsAnnotatedTypeFactory.
    Class<?> value();
}
