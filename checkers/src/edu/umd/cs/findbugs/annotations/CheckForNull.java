package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.*;
import checkers.quals.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
public @interface CheckForNull {
}
