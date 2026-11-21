package org.checkerframework.framework.testchecker.customapplier;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose applier overrides {@code QualifierDefaults.DefaultApplierElement.addAnnotation},
 * so that applying a default does not mean "add the qualifier if the hierarchy is empty".
 */
public class CustomApplierChecker extends BaseTypeChecker {}
