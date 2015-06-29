package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.processing.SupportedAnnotationTypes;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.qualframework.base.Checker;

/**
 * An annotation that lists the type qualifiers supported by the annotated
 * {@code Checker}.
 *
 * <p>
 * Example:
 *
 * <pre>
 * &#064;TypeQualifiers( { Nullable.class, NonNull.class } )
 * public class NullnessChecker extends BaseTypeChecker { ... }
 * </pre>
 *
 * The checker reflectively queries this annotation, and subsequently the
 * meta-annotations on the annotations in the list, to form the result of
 * {@link AnnotatedTypeFactory#createSupportedTypeQualifiers()} and
 * {@link AnnotatedTypeFactory#getSupportedTypeQualifiers()}.
 * The framework also uses this annotation to determine
 * which annotations may be added to an {@link AnnotatedTypeMirror}: an
 * annotation may be added if and only if it is a {@link TypeQualifier} and it
 * appears in in the list of supported annotations from {@link TypeQualifiers}.
 * <p>
 *
 * Each type-checker should either be annotated with
 * {@code @TypeQualifiers} or should override
 * {@link AnnotatedTypeFactory#createSupportedTypeQualifiers()
 * createSupportedTypeQualifiers} (which takes precedence over
 * {@code @TypeQualifiers}).
 * <p>
 *
 * This annotation differs from the JDK's {@link SupportedAnnotationTypes}
 * in the following ways.
 * <ul>
 * <li>
 * <tt>{@link TypeQualifiers}</tt>
 * simply lists the annotations that a processor (checker) recognizes -- any
 * annotations not in the list should be ignored, but files lacking these
 * annotations should still be processed. {@link SupportedAnnotationTypes}
 * instructs the compiler to skip processing of any file that
 * does not contain any supported annotations.
 * </li>
 * <li>
 * {@link SupportedAnnotationTypes}'s argument is an array of strings, which
 * supports the use of "*" for specifying multiple annotations.
 * {@link TypeQualifiers}'s argument is an array of
 * classes, which is less error-prone.
 * </li>
 * </ul>
 *
 * @see AnnotatedTypeFactory#createSupportedTypeQualifiers()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE } )
public @interface TypeQualifiers {
    /** The type qualifier annotations supported by the annotated {@link Checker}.
     * The checker may also support other, non-type-qualifier, annotations. */
    Class<? extends Annotation>[] value();
}
