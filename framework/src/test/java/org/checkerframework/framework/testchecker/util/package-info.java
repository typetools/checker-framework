/**
 * A minimal checker and simple annotation used for testing the Checker Framework without the
 * semantics of any particular (meaningful) checker.
 *
 * <p>The checker and annotation in this package should only be used for testing.
 *
 * <p>The {@code @Odd} annotation is a straightforward subtype-style qualifier. It has no special
 * semantics; values that do not have an {@code @Odd} type cannot be assigned to values that do have
 * the {@code @Odd} type.
 */
package org.checkerframework.framework.testchecker.util;
