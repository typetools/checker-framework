import java.lang.annotation.*;

import checkers.quals.*;
import checkers.units.quals.*;

/**
 * Hertz (Hz), a unit of frequency.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf( { Frequency.class } )
@UnitsRelations(FrequencyRelations.class)
public @interface Hz {
    Prefix value() default Prefix.one;
}
