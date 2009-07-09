package checkers.tainting;

import checkers.quals.*;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

/**
 * Denotes a reference that is untainted, i.e. can be trusted.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor(trees = { STRING_LITERAL })
public @interface Untainted { }
