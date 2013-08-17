package net.jcip.annotations;

import java.lang.annotation.*;
import checkers.quals.TypeQualifier;

@Documented
// The JCIP annotation can be used on a field (in which case it is a type
// qualifier corresponding to the Lock Checker's @GuardedBy) or on a method
// (in which case it is a declaration annotation corresponding to the Lock
// Checker's @Holding).
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
public @interface GuardedBy {
  String value();
}
