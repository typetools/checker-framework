package lubglb.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultForInUntyped;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

@TypeQualifier
@SubtypeOf({D.class, E.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@DefaultForInUntyped({DefaultLocation.RETURNS})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface F { }
