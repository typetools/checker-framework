package javax.annotation;

import java.lang.annotation.*;

import javax.annotation.meta.When;

import org.checkerframework.framework.qual.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
public @interface Nonnull {
    When when() default When.ALWAYS;
}
