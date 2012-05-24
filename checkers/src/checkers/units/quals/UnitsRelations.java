package checkers.units.quals;

import java.lang.annotation.*;

/**
 * Specify the class that knows how to handle the meta-annotated unit
 * when put in relation (plus, multiply, ...) with another unit.
 * 
 * @see checkers.units.UnitsRelations
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitsRelations {
    /**
     * @return The UnitsRelations subclass to use.
     */
    Class<? extends checkers.units.UnitsRelations> value();
}
