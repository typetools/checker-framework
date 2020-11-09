package org.checkerframework.common.initializedfields;

import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The Initialized Fields Checker.
 *
 * @checker_framework.manual #initialized-fields-checker Initialized Fields Checker
 */
@SupportedOptions({"checkInitializedFields"})
public class InitializedFieldsChecker extends AccumulationChecker {}
