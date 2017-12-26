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
 * <p>Additionally, the precondition annotations annotated by this meta-annotation can define
 * arguments, that will be used as arguments of the qualifier. The arguments {@code sourceArguments}
 * and {@code targetArguments} specify how arguments of the precondition annotation are mapped to
 * arguments of the qualifier. The array {@code sourceArguments} contains names of arguments of the
 * precondition annotation that should be passed to the qualifier. For each such argument, the name
 * of the argument of the qualifier should be at the same index in the {@code targetArguments}
 * array. If {@code targetArguments} is shorter than {@code sourceArguments}, then the arguments
 * that do not have a corresponding entry in {@code targetArguments} are mapped to an argument of
 * the same name.
 *
 * @author Stefan Heule
 * @see RequiresQualifier
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreconditionAnnotation {
    /** The hard-coded qualifier for the precondition. */
    Class<? extends Annotation> qualifier();
    /** List of names of arguments specified in the precondition. */
    String[] sourceArguments() default {};
    /** List of names of arguments passed to the qualifier. */
    String[] targetArguments() default {};
}
