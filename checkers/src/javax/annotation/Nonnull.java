package javax.annotation;

import java.lang.annotation.*;
import checkers.quals.TypeQualifier;
import javax.annotation.meta.When;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
public @interface Nonnull {
    When when() default When.ALWAYS;
}
