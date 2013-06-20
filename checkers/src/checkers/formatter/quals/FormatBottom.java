package checkers.formatter.quals;

import java.lang.annotation.Target;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Formatter type hierarchy. 
 * This is not yet used.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 * 
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf({Format.class,InvalidFormat.class})
@Target({}) // empty target prevents programmers from writing this in a program
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
public @interface FormatBottom {}
