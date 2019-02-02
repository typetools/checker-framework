package org.checkerframework.checker.toy.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/** Toy bottom qualifier. */
@SubtypeOf(ToyTop.class)
@ImplicitFor(literals = {LiteralKind.STRING, LiteralKind.NULL})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@DefaultQualifierInHierarchy
public @interface ToyBottom {}
