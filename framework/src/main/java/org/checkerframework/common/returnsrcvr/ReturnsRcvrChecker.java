package org.checkerframework.common.returnsrcvr;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * Empty Checker is the entry point for pluggable type-checking.
 *
 * <p>This one does nothing. The Checker Framework manual tells you how to make it do something:
 * https://checkerframework.org/manual/#creating-a-checker
 */
@SupportedOptions({ReturnsRcvrChecker.DISABLED_FRAMEWORK_SUPPORTS})
public class ReturnsRcvrChecker extends BaseTypeChecker {
    public static final String DISABLED_FRAMEWORK_SUPPORTS = "disableFrameworkSupports";
    public static final String LOMBOK_SUPPORT = "LOMBOK";
    public static final String AUTOVALUE_SUPPORT = "AUTOVALUE";
}
