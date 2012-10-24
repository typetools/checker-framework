package org.jmlspecs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import checkers.quals.TypeQualifier;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({TYPE_USE, TYPE_PARAMETER})
@TypeQualifier
public @interface Nullable {}
