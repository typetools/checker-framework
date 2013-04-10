package checkers.initialization.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@Documented
@SubtypeOf({})
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnkownInitialization {
    /**
     * The type-frame down to which the expression (of this type) has been
     * initialized at least.
     */
    Class<?> value() default Object.class;
}
