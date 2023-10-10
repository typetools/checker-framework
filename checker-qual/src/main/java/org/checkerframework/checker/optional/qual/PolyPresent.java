package org.checkerframework.checker.optional.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A polymorphic qualifier for the Optional type system.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@RelevantJavaTypes(Optional.class)
@PolymorphicQualifier(MaybePresent.class)
public @interface PolyPresent {}
