package checkers.nullness.quals;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;

import checkers.quals.*;

/**
 * TODO: the bottom type for the KeyFor system.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@InvisibleQualifier
@SubtypeOf(KeyFor.class)
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface KeyForBottom {}