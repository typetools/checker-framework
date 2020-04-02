package org.checkerframework.common.returnsreceiver;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/** Entry point for the Returns Receiver Checker. */
@SupportedOptions({ReturnsReceiverChecker.DISABLE_FRAMEWORK_SUPPORT})
public class ReturnsReceiverChecker extends BaseTypeChecker {
    /** String representation for DISABLE_FRAMEWORK_SUPPORTS. */
    public static final String DISABLE_FRAMEWORK_SUPPORT = "disableFrameworks";
    /** String representation for LOMBOK_SUPPORT. */
    public static final String LOMBOK_SUPPORT = "LOMBOK";
    /** String representation for AUTOVALUE_SUPPORT. */
    public static final String AUTOVALUE_SUPPORT = "AUTOVALUE";
}
