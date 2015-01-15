package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The bottom qualifier for the PropertyKeyChecker and associated checkers.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@TypeQualifier
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
//Note: Most PropertyKey related type systems also make this annotation implicit for NULL literals
//by overloading the AnnotatedTypeFactory.createTreeAnnotator method (see CompilerMessagesAnnotatedTypeFactory),
//one exception to this is the I18nChecker
@ImplicitFor(typeNames = {java.lang.Void.class})
public @interface PropertyKeyBottom {}
