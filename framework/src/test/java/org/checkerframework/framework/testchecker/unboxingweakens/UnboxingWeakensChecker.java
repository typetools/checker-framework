package org.checkerframework.framework.testchecker.unboxingweakens;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose unboxing conversion weakens a qualifier: unboxing a {@code @UWStrong} value
 * yields a {@code @UWTop} primitive. It tests that dataflow does not restore a qualifier that an
 * unboxing conversion removes.
 */
public class UnboxingWeakensChecker extends BaseTypeChecker {}
