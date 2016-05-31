package org.checkerframework.framework.qual;

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
