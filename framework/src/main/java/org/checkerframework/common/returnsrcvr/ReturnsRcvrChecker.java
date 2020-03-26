package org.checkerframework.common.returnsrcvr;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/** Entry point for Returns Receiver Checker */
@SupportedOptions({ReturnsRcvrChecker.DISABLED_FRAMEWORK_SUPPORTS})
public class ReturnsRcvrChecker extends BaseTypeChecker {
    public static final String DISABLED_FRAMEWORK_SUPPORTS = "disableFrameworkSupports";
    public static final String LOMBOK_SUPPORT = "LOMBOK";
    public static final String AUTOVALUE_SUPPORT = "AUTOVALUE";
}
