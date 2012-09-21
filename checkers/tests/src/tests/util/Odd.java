package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

@TypeQualifier
@Inherited
@ImplicitFor ( trees = { Tree.Kind.NULL_LITERAL } )
@SubtypeOf(MonotonicOdd.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Odd {}
