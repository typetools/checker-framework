package checkers.propkey.quals;

import java.lang.annotation.*;

import checkers.quals.*;

import com.sun.source.tree.Tree;

/**
 * The bottom of the type hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( {} )
@TypeQualifier
@SubtypeOf( PropertyKey.class )
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface Bottom {}
