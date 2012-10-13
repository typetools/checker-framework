import java.lang.annotation.*;

import checkers.quals.*;
import checkers.units.quals.*;

/**
 * Hertz (Hz).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf( { Frequency.class } )
@UnitsRelations(FrequencyRelations.class)
public @interface Hz {
    Prefix value() default Prefix.one;
}
