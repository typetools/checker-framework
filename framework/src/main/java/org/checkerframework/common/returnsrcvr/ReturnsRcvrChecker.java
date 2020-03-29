package org.checkerframework.common.returnsrcvr;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/** Entry point for Returns Receiver Checker */
@SupportedOptions({ReturnsRcvrChecker.DISABLE_FRAMEWORK_SUPPORT})
public class ReturnsRcvrChecker extends BaseTypeChecker {
    /** String representation for DISABLE_FRAMEWORK_SUPPORTS. */
    public static final String DISABLE_FRAMEWORK_SUPPORT = "disableFrameworkSupport";
    /** String representation for LOMBOK_SUPPORT */
    public static final String LOMBOK_SUPPORT = "LOMBOK";
    /** String representation for AUTOVALUE_SUPPORT */
    public static final String AUTOVALUE_SUPPORT = "AUTOVALUE";
}
