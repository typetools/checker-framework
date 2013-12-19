package lubglb.quals;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface A { }
