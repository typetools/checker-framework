package checkers.quals;

import java.lang.annotation.*;

import javax.annotation.processing.SupportedAnnotationTypes;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;

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
 * {@link BaseTypeChecker#getSupportedTypeQualifiers()} construct the type
 * qualifier hierarchy. The framework also uses this annotation to determine
 * which annotations may be added to an {@link AnnotatedTypeMirror} (an
 * annotation may be added if and only if it is a {@link TypeQualifier} and it
 * appears in in the list of supported annotations from {@link TypeQualifiers}).
 *
 * <p>
 *
 * This annotation differs from {@link SupportedAnnotationTypes} in that it
 * simply lists the annotations that a processor (checker) recognizes -- any
 * annotations not in the list should be ignored, but files lacking these
 * annotations should still be processed. {@link SupportedAnnotationTypes}, on
 * the other hand, instructs the compiler to skip processing of any file that
 * does not contain any supported annotations.
 *
 * Another difference is that {@link SupportedAnnotationTypes}'s argument is an
 * array of strings, whereas {@link TypeQualifiers}'s argument is an array of
 * classes. The former supports the use of "*" for specifying multiple
 * annotations (which is less important for {@link TypeQualifiers} than for
 * {@link SupportedAnnotationTypes}), while the latter permits type-checking,
 * refactoring by IDEs, etc.
 *
 * @see BaseTypeChecker#getSupportedTypeQualifiers()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { ElementType.TYPE } )
public @interface TypeQualifiers {
    /** The type qualifier annotations supported by the annotated {@code Checker}.
     * The checker may also support other, non-type-qualifier, annotations. */
    Class<? extends Annotation>[] value();
}
