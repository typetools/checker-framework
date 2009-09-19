package checkers.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation indicating that the annotated annotation is a type
 * qualifier.
 *
 * Examples of such qualifiers: {@code @ReadOnly}, {@code @NonNull}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeQualifier {

}
