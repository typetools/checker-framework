package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.framework.qual.TypeQualifiers;

@TypeQualifiers({ LockHeld.class, LockPossiblyHeld.class })
public class LockChecker extends BaseTypeChecker {
}