package checkers.lock;

import checkers.basetype.BaseTypeChecker;
import checkers.lock.quals.GuardedBy;
import checkers.lock.quals.GuardedByTop;
import checkers.lock.quals.Holding;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker plug-in for the JCIP type system qualifier that finds (and
 * verifies the absence of) locking and concurrency errors.
 *
 * @see GuardedBy
 * @see Holding
 * @checker.framework.manual #lock-checker Lock Checker
 */
@TypeQualifiers( { GuardedBy.class, Unqualified.class, GuardedByTop.class } )
public class LockChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

}
