package checkers.tainting.quals;

import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a reference that is untainted, i.e. can be trusted.
 *
 * @checker.framework.manual #tainting-checker Tainting Checker
 */
@TypeQualifier
@SubtypeOf(Tainted.class)
@ImplicitFor(trees = { STRING_LITERAL, NULL_LITERAL })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Untainted {}
