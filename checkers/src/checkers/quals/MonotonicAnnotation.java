package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is monotonic.
 *
 * <p>
 * TODO: full documentation.
 *
 * @author Stefan Heule
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicAnnotation {
    Class<? extends Annotation> value();
}
