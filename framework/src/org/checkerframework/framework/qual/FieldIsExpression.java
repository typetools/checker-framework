package org.checkerframework.framework.qual;

import java.lang.annotation.*;

/**
 * A meta-annotation indicating that the annotated annotation is a type
 * qualifier with a field named 'value' that is an array of Strings
 * containing Java expressions.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface FieldIsExpression { }