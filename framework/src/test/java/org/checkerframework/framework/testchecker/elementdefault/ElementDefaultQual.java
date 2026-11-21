package org.checkerframework.framework.testchecker.elementdefault;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.testchecker.util.SuperQual;

/**
 * A subtype of SuperQual and a sibling of SubQual. Its name sorts before {@code SubQual}'s name, so
 * a test can tell whether a default was chosen by the annotations' names or by the defaults'
 * origins.
 */
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ElementDefaultQual {}
