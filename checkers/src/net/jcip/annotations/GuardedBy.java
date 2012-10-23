package net.jcip.annotations;

import java.lang.annotation.*;
import checkers.quals.TypeQualifier;

@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
public @interface GuardedBy {
  String value();
}
