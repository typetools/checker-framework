package qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.UnitsMultiple;
import org.checkerframework.checker.units.qual.UnitsRelations;
import org.checkerframework.framework.qual.SubtypeOf;

/** Kilohertz (kHz), a unit of frequency, and an alias of @Hz(Prefix.kilo). */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Frequency.class)
@UnitsRelations(FrequencyRelations.class)
@UnitsMultiple(quantity = Hz.class, prefix = Prefix.kilo) // alias of @Hz(Prefix.kilo)
public @interface kHz {} // No prefix defined in the annotation itself
