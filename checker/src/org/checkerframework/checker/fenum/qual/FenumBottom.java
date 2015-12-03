package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The bottom qualifier for fenums, its relationships are setup via the
 * FenumAnnotatedTypeFactory.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@TypeQualifier
@Documented
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({}) //subtype relationships are set up by passing this class as a bottom
               //to the multigraph hierarchy constructor
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
             typeNames = {java.lang.Void.class})
public @interface FenumBottom {}
