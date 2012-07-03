package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a precondition
 * annotation, i.e., a type-specialized version of {@link RequiresAnnotation}.
 * The annotation that is annotated as {@link PreconditionAnnotation} must have
 * a value called {@code value} that is an array of {@code String}s of the same
 * format and with the same meaning as the value {@code expression} in
 * {@link RequiresAnnotation}.
 * 
 * <p>
 * The value {@code annotation} that is necessary for a precondition specified
 * with {@link RequiresAnnotation} is hard-coded here with the value
 * {@code annotation}.
 * 
 * @author Stefan Heule
 * @see RequiresAnnotation
 * 
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PreconditionAnnotation {
    /** The hard-coded annotation for the precondition. */
    Class<? extends Annotation> annotation();
}
