package org.checkerframework.framework.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation intended solely for representing an unqualified type in the qualifier hierarchy for
 * the Purity Checker
 *
 * <p>Note that because of the missing RetentionPolicy, the qualifier will not be stored in
 * bytecode.
 */
// TODO: set it to store in source rather than having missing RetentionPolicy.
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PurityUnqualified {}
