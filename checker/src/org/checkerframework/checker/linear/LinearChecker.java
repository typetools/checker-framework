package org.checkerframework.checker.linear;

import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Normal;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker plug-in for the Linear type system.  A {@code @Linear}
 * reference may be used only one time.  After that, it is "used up" and
 * of type {@code @Unusable}, and any further use is a compile-time error.
 *
 * @checker_framework.manual #linear-checker Linear Checker

 */
@TypeQualifiers({Normal.class, Linear.class, Unusable.class})
public class LinearChecker extends BaseTypeChecker { }
