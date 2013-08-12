package checkers.nullness.quals;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;

import checkers.quals.*;

/**
 * TODO: document that this is the bottom type for the KeyFor system.
 *
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@InvisibleQualifier
@SubtypeOf(KeyFor.class)
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
public @interface KeyForBottom {}
