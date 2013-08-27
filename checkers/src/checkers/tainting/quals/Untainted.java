package checkers.tainting.quals;

import java.lang.annotation.*;

import checkers.quals.*;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

/**
 * Denotes a reference that is untainted, i.e. can be trusted.
 *
 * @checker.framework.manual #tainting-checker Tainting Checker
 */
@TypeQualifier
@SubtypeOf(Tainted.class)
@ImplicitFor(trees = { STRING_LITERAL })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Untainted {}
