package org.checkerframework.checker.guieffect.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Annotation to override the UI effect on a class, and make a field or method
 * safe for non-UI code to use.
 *
 * @checker_framework.manual #guieffect-checker GUI Effect Checker
 */
@TypeQualifier
@SubtypeOf({UI.class})
@DefaultQualifierInHierarchy
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface AlwaysSafe {}
