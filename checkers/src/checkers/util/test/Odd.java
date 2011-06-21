package checkers.util.test;

import java.lang.annotation.Inherited;

import com.sun.source.tree.Tree;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

@TypeQualifier
@Inherited
@ImplicitFor ( trees = { Tree.Kind.NULL_LITERAL } )
@SubtypeOf({Unqualified.class})
public @interface Odd { /**/ }
