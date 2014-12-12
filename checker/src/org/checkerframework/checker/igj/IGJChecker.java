package org.checkerframework.checker.igj;

import org.checkerframework.checker.igj.qual.Assignable;
import org.checkerframework.checker.igj.qual.AssignsFields;
import org.checkerframework.checker.igj.qual.I;
import org.checkerframework.checker.igj.qual.Immutable;
import org.checkerframework.checker.igj.qual.Mutable;
import org.checkerframework.checker.igj.qual.ReadOnly;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;


/**
 * A type-checker plug-in for the IGJ immutability type system that finds (and
 * verifies the absence of) undesired side-effect errors.
 *
 * The IGJ language is a Java language extension that expresses immutability
 * constraints, using six annotations: {@link ReadOnly}, {@link Mutable},
 * {@link Immutable}, {@link I} -- a polymorphic qualifier, {@link Assignable},
 * and {@link AssignsFields}.  The language is specified by the FSE 2007 paper.
 *
 * @checker_framework.manual #igj-checker IGJ Checker
 *
 */
@TypeQualifiers({ ReadOnly.class, Mutable.class, Immutable.class, I.class,
    AssignsFields.class, IGJBottom.class })
public class IGJChecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
