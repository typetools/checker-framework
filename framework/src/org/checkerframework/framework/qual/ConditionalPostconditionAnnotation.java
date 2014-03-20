package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a conditional
 * postcondition annotation, i.e., a type-specialized version of
 * {@link EnsuresQualifierIf}. The annotation that is annotated as
 * {@link ConditionalPostconditionAnnotation} must have a value called
 * {@code expression} that is an array of {@code String}s of the same format and
 * with the same meaning as the value {@code expression} in
 * {@link EnsuresQualifierIf}, as well as a value {@code result} with the same
 * meaning as the value {@code result} in {@link EnsuresQualifierIf}.
 *
 * <p>
 * The value {@code qualifier} that is necessary for a conditional
 * postcondition specified with {@link EnsuresQualifier} is hard-coded here
 * with the value {@code qualifier}.
 *
 * @author Stefan Heule
 * @see EnsuresQualifier
 *
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalPostconditionAnnotation {
    /** The hard-coded qualifier for the postcondition. */
    Class<? extends Annotation> qualifier();
}
