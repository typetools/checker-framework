package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a postcondition
 * annotation, i.e., a type-specialized version of {@link EnsuresAnnotation}.
 * The annotation that is annotated as {@link PostconditionAnnotation} must have
 * a value called {@code value} that is an array of {@code String}s of the same
 * format and with the same meaning as the value {@code expression} in
 * {@link EnsuresAnnotation}.
 * 
 * <p>
 * The value {@code annotation} that is necessary for a postcondition specified
 * with {@link EnsuresAnnotation} is hard-coded here with the value
 * {@code annotation}.
 * 
 * @author Stefan Heule
 * @see EnsuresAnnotation
 * 
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PostconditionAnnotation {
    /** The hard-coded annotation for the postcondition. */
    Class<? extends Annotation> annotation();
}
