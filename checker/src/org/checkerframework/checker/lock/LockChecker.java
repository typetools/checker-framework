package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByTop;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.framework.qual.TypeQualifiers;

// The Lock Checker uses two distinct type qualifier hierarchies:
// the @LockPossiblyHeld and the @GuardedByTop hierarchies.
@TypeQualifiers({ LockPossiblyHeld.class, LockHeld.class,
                  GuardedByTop.class, GuardedBy.class, GuardedByBottom.class })
public class LockChecker extends BaseTypeChecker {
}