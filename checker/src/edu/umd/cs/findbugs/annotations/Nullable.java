package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.TypeQualifier;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@TypeQualifier
public @interface Nullable {
}
