package org.checkerframework.framework.testchecker.promotion;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker that gives type {@code @Promoted} to the result of each unboxing conversion and each
 * widening primitive conversion, and reports each operand of a binary operation or compound
 * assignment that has type {@code @Promoted}. It tests which conversions {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#binaryTreeArgTypes} applies to the
 * operands of a binary operation.
 */
public class PromotionChecker extends BaseTypeChecker {}
