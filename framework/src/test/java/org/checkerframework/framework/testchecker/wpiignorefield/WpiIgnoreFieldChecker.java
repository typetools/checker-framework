package org.checkerframework.framework.testchecker.wpiignorefield;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose only purpose is to run the assertions in {@link WpiIgnoreFieldVisitor}. It
 * performs no type-checking of its own.
 */
public class WpiIgnoreFieldChecker extends BaseTypeChecker {}
