package org.checkerframework.framework.testchecker.customshouldbeannotated;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose applier overrides {@code
 * QualifierDefaults.DefaultApplierElement.shouldBeAnnotated}, so that no default for {@code
 * TypeUseLocation.FIELD} is applied.
 */
public class CustomShouldBeAnnotatedChecker extends BaseTypeChecker {}
