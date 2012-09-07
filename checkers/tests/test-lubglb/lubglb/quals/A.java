package lubglb.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface A { }