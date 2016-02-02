package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;

/**
 * The bottom qualifier for fenums, its relationships are setup via the
 * FenumAnnotatedTypeFactory.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@Documented
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
@Target({ElementType.TYPE_USE})
@SubtypeOf({}) //subtype relationships are set up by passing this class as a bottom
               //to the multigraph hierarchy constructor
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
             typeNames = {java.lang.Void.class})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface FenumBottom {}
