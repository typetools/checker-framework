package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByInaccessible;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * @checker_framework.manual #lock-checker Lock Checker
 */
// The Lock Checker uses two distinct type qualifier hierarchies:
// the @LockPossiblyHeld and the @GuardedByInaccessible hierarchies.
@TypeQualifiers({ LockPossiblyHeld.class, LockHeld.class,
                  GuardedByInaccessible.class, GuardedBy.class,
                  javax.annotation.concurrent.GuardedBy.class,
                  net.jcip.annotations.GuardedBy.class,
                  GuardSatisfied.class, GuardedByBottom.class })
public class LockChecker extends BaseTypeChecker {
}
