package javax.annotation;

import java.lang.annotation.*;

import javax.annotation.meta.When;

import org.checkerframework.framework.qual.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
public @interface Nonnull {
    When when() default When.ALWAYS;
}
