package checkers.regex.quals;

import java.lang.annotation.Target;

import checkers.quals.ImplicitFor;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Regex qualifier hierarchy. This is used to make
 * the null literal a subtype of all Regex annotations.
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
@SubtypeOf({Regex.class, PartialRegex.class})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface RegexBottom {}
