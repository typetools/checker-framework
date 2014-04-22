package org.checkerframework.checker.lock;

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByTop;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker plug-in for the JCIP type system qualifier that finds (and
 * verifies the absence of) locking and concurrency errors.
 *
 * @see GuardedBy
 * @see Holding
 * @checker_framework_manual #lock-checker Lock Checker
 */
@TypeQualifiers( { GuardedBy.class, GuardedByBottom.class, GuardedByTop.class } )
public class LockChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

}
