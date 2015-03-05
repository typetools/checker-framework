package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifiers({ LockHeld.class, LockPossiblyHeld.class })
public class LockChecker extends BaseTypeChecker {
}
