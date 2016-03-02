package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Internationalization Format String type hierarchy.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 */
@SubtypeOf({ I18nFormat.class, I18nInvalidFormat.class, I18nFormatFor.class })
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@DefaultFor(value = {DefaultLocation.LOWER_BOUNDS})
public @interface I18nFormatBottom {
}
