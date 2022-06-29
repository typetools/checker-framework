package org.checkerframework.checker.testchecker.disbaruse;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker that issues a "disbar.use" error at any use of fields, methods or parameters whose type
 * is {@code @DisbarUse}.
 */
public class DisbarUseChecker extends BaseTypeChecker {}
