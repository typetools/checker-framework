package org.checkerframework.common.delegation;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.delegation.qual.Delegate;

/**
 * This class enforces checks for the {@link Delegate} annotation.
 *
 * <p>It is not a checker for a type system. It enforces the following syntactic checks:
 *
 * <ul>
 *   <li>A class may have up to exactly one field marked with the {@link Delegate} annotation.
 *   <li>An overridden method's implementation must be exactly a call to the delegate field.
 *   <li>A class overrides <i>all</i> methods declared in its superclass.
 * </ul>
 */
public class DelegationChecker extends BaseTypeChecker {}
