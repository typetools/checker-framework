package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

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
