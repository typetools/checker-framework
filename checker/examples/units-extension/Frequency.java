import java.lang.annotation.*;

import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.framework.qual.*;


/**
 * Units of frequency, such as hertz (@{@link Hz}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf( { UnknownUnits.class } )
public @interface Frequency {}
