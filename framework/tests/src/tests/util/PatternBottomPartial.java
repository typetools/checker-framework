package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

import com.sun.source.tree.Tree;

@SubtypeOf({PatternAB.class, PatternBC.class, PatternAC.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface PatternBottomPartial {}
