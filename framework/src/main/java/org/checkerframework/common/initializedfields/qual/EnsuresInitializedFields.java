package org.checkerframework.common.initializedfields.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/** A method postcondition annotation indicates which fields the method definitely initializes. */
@PostconditionAnnotation(qualifier = InitializedFields.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EnsuresInitializedFields {
    /**
     * The object(s) whose fields this method initializes.
     *
     * @return object(s) whose fields are initialized
     */
    public String[] value(); // TODO: default "this";

    /**
     * Fields that this method initializes.
     *
     * @return fields that this method initializes
     */
    @QualifierArgument("value")
    public String[] fields();
}
