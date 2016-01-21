package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Format String type hierarchy.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@SubtypeOf({Format.class,InvalidFormat.class})
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@DefaultFor(value = {DefaultLocation.LOWER_BOUNDS})
public @interface FormatBottom {
}
