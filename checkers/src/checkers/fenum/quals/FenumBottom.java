package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * The Any modifier expresses no static ownership information, the referenced
 * object can have any owner.
 * 
 * @author wmdietl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( {} )
@TypeQualifier
@SubtypeOf( { Fenum.class, FenumUnqualified.class } )
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface FenumBottom {}
