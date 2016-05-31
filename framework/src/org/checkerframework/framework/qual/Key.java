package org.checkerframework.framework.qual;

/**
 * Annotation used internally by the qualifier framework for mapping annotations to qualifiers.
 */
@SubtypeOf({})
public @interface Key {
    /**
     * An index into the lookup table.
     */
    int index() default -1;

    /**
     * A string representation of the qualifier this {@link Key} represents.  This lets us have
     * slightly nicer error messages.
     */
    String desc() default "";
}
