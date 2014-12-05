package org.checkerframework.framework.util;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.qual.Unqualified;

/**
 * Perform purity checking only.
 */
@TypeQualifiers(Unqualified.class)
public class PurityChecker extends BaseTypeChecker {
}
