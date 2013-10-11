package checkers.igj;

import checkers.basetype.BaseTypeChecker;
import checkers.igj.quals.Assignable;
import checkers.igj.quals.AssignsFields;
import checkers.igj.quals.I;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.Mutable;
import checkers.igj.quals.ReadOnly;
import checkers.quals.TypeQualifiers;


/**
 * A type-checker plug-in for the IGJ immutability type system that finds (and
 * verifies the absence of) undesired side-effect errors.
 *
 * The IGJ language is a Java language extension that expresses immutability
 * constraints, using six annotations: {@link ReadOnly}, {@link Mutable},
 * {@link Immutable}, {@link I} -- a polymorphic qualifier, {@link Assignable},
 * and {@link AssignsFields}.  The language is specified by the FSE 2007 paper.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
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
