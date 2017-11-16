// Upstream version:
// https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NotNull.html

package javax.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.PARAMETER,
    ElementType.TYPE_USE
})
public @interface NotNull {
    // The following annotation attributes are allowed in source code,
    // but are ignored by the Nullness Checker.

    Class<?>[] groups() default {};

    String message() default "{javax.validation.constraints.NotNull.message}";
    // To not depend on the Payload class, let us use a more flexible bound.
    Class<?>[] payload() default {};
}
