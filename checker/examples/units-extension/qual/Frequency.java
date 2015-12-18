package qual;

import java.lang.annotation.*;

import org.checkerframework.checker.units.qual.UnitsRelations;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.framework.qual.*;


/**
 * Units of frequency, such as hertz (@{@link Hz}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownUnits.class})
@UnitsRelations(FrequencyRelations.class)
public @interface Frequency {}
