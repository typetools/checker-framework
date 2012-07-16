package checkers.quals;

import java.lang.annotation.*;

import checkers.util.QualifierPolymorphism;
import checkers.nullness.quals.PolyNull;
import checkers.interning.quals.PolyInterned;

/**
 * A polymorphic qualifier that varies over all type hierarchies.
 *
 * TODO: documentation and examples.
 *
 * @see PolyNull
 * @see PolyInterned
 * @see QualifierPolymorphism
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@PolymorphicQualifier
public @interface PolyAll {
    // TODO: support multiple variables using an id.
    //int value() default 0;
}
