package org.checkerframework.qualframework.qual;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Annotation used internally by the qualifier framework for mapping annotations to qualifiers.
 */
@SubtypeOf({})
public @interface QualifierKey {
    /**
     * An index into the lookup table.
     */
    int index() default -1;

    /**
     * A string representation of the qualifier this {@link QualifierKey} represents.  This lets us have
     * slightly nicer error messages.
     */
    String desc() default "";
}
