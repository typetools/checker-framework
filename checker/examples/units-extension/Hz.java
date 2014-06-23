import java.lang.annotation.*;

import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.UnitsRelations;
import org.checkerframework.framework.qual.*;

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
