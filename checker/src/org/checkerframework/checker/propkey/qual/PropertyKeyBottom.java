package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom qualifier for the PropertyKeyChecker and associated checkers.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
//Note: Most PropertyKey related type systems also make this annotation implicit for NULL literals
//by overloading the AnnotatedTypeFactory.createTreeAnnotator method (see CompilerMessagesAnnotatedTypeFactory),
//one exception to this is the I18nChecker
@ImplicitFor(typeNames = {java.lang.Void.class})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface PropertyKeyBottom {}
