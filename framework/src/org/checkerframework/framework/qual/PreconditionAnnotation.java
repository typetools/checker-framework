package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a precondition annotation, i.e., a
 * type-specialized version of {@link RequiresQualifier}. The annotation that is annotated as {@link
 * PreconditionAnnotation} must have a value called {@code value} that is an array of {@code
 * String}s of the same format and with the same meaning as the value {@code expression} in {@link
 * RequiresQualifier}.
 *
 * <p>The value {@code qualifier} that is necessary for a precondition specified with {@link
 * RequiresQualifier} is hard-coded here with the value {@code qualifier}.
 *
 * @author Stefan Heule
 * @see RequiresQualifier
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreconditionAnnotation {
    /** The hard-coded qualifier for the precondition. */
    Class<? extends Annotation> qualifier();
}
