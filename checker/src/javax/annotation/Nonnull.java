package javax.annotation;

import java.lang.annotation.*;

import javax.annotation.meta.When;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
    When when() default When.ALWAYS;
}
