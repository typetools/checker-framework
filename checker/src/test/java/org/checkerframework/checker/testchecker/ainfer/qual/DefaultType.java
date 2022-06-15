package org.checkerframework.checker.testchecker.ainfer.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * DefaultType is used to test the relaxInference option. Toy type system for testing field
 * inference. This annotation cannot be used in source code.
 *
 * @see Sibling1
 * @see Sibling2
 * @see Parent
 * @see Top
 */
@SubtypeOf({Top.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
public @interface DefaultType {}
