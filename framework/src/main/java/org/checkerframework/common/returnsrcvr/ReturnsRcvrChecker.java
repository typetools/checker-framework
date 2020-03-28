package org.checkerframework.common.returnsrcvr;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/** Entry point for Returns Receiver Checker */
@SupportedOptions({ReturnsRcvrChecker.DISABLED_FRAMEWORK_SUPPORTS})
public class ReturnsRcvrChecker extends BaseTypeChecker {
    /** String representation for DISABLED_FRAMEWORK_SUPPORTS */
    public static final String DISABLED_FRAMEWORK_SUPPORTS = "disableFrameworkSupports";
    /** String representation for LOMBOK_SUPPORT */
    public static final String LOMBOK_SUPPORT = "LOMBOK";
    /** String representation for AUTOVALUE_SUPPORT */
    public static final String AUTOVALUE_SUPPORT = "AUTOVALUE";
}
