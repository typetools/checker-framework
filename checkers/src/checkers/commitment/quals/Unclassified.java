package checkers.commitment.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
@SubtypeOf({})
public @interface Unclassified {
}
