package org.checkerframework.common.returnsreceiver;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/** Entry point for the Returns Receiver Checker. */
@StubFiles({"DescribeImages.astub", "GenerateDataKey.astub"})
public class ReturnsReceiverChecker extends BaseTypeChecker {}
