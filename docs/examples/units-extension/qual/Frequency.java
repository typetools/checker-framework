package qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.units.qual.UnitsRelations;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Units of frequency, such as hertz (@{@link Hz}).
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownUnits.class})
@UnitsRelations(FrequencyRelations.class)
public @interface Frequency {}
