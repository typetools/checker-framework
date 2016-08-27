package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specify the class that knows how to handle the meta-annotated unit
 * when put in relation (plus, multiply, ...) with another unit.
 *
 * @see org.checkerframework.checker.units.UnitsRelations
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitsRelations {
    /**
     * @return the UnitsRelations subclass to use
     */
    Class<? extends org.checkerframework.checker.units.UnitsRelations> value();
}
