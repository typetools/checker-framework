package org.checkerframework.checker.determinism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@Documented
@PolymorphicQualifier(NonDet.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
//@DefaultFor({TypeUseLocation.PARAMETER, TypeUseLocation.RETURN, TypeUseLocation.RECEIVER})
public @interface PolyDet {
    String value() default "";
}
