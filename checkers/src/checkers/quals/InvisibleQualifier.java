package checkers.quals;

import java.lang.annotation.*;

/**
 * A meta-annotation indicating that the annotated annotation is a type
 * qualifier that should not be visible in output.
 *
 * Examples of such qualifiers: {@code @Unqualified}, {@code @Primitive}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InvisibleQualifier { }