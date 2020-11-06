package org.checkerframework.common.initializedfields.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A method postcondition annotation indicates which fields the method definitely initializes. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface InitializesFields {
    /**
     * The object whose fields this method initializes.
     *
     * @return object whose fields are initialized
     */
    public String[] value() default "this";

    /**
     * Fields that this method initializes.
     *
     * @return fields that this method initializes
     */
    public String[] fields();
}
