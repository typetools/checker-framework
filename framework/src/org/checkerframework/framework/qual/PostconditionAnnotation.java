package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a postcondition annotation, i.e., a
 * type-specialized version of {@link EnsuresQualifier}. The annotation that is annotated as {@link
 * PostconditionAnnotation} must have a value called {@code value} that is an array of {@code
 * String}s of the same format and with the same meaning as the value {@code expression} in {@link
 * EnsuresQualifier}.
 *
 * <p>The value {@code qualifier} that is necessary for a postcondition specified with {@link
 * EnsuresQualifier} is hard-coded here with the value {@code qualifier}.
 *
 * <p>Additionally, the postcondition annotations annotated by this meta-annotation can define
 * arguments, that will be used as arguments of the qualifier. The arguments {@code sourceArguments}
 * and {@code targetArguments} specify how arguments of the postcondition annotation are mapped to
 * arguments of the qualifier. The array {@code sourceArguments} contains names of arguments of the
 * postcondition annotation that should be passed to the qualifier. For each such argument, the name
 * of the argument of the qualifier should be at the same index in the {@code targetArguments}
 * array. If {@code targetArguments} is shorter than {@code sourceArguments}, then the arguments
 * that do not have a corresponding entry in {@code targetArguments} are mapped to an argument of
 * the same name.
 *
 * @author Stefan Heule
 * @see EnsuresQualifier
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostconditionAnnotation {
    /** The hard-coded qualifier for the postcondition. */
    Class<? extends Annotation> qualifier();
    /** List of names of arguments specified in the postcondition. */
    String[] sourceArguments() default {};
    /** List of names of arguments passed to the qualifier. */
    String[] targetArguments() default {};
}
