package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code AssertMethod} is a method annotation that indicates that a method throws an exception if
 * the value of a boolean argument is false. This can be used to annotate methods such as Junit's
 * {@code Assertions.assertTrue(...)}.
 *
 * <p>The annotation enables flow-sensitive type refinement to be more precise. For example, after
 *
 * <pre>
 *   Assertions.assertTrue(optional.isPresent());
 * </pre>
 *
 * the Optional Checker can determine that {@code optional} has a value.
 *
 * <p>The annotation is a <em>trusted</em> annotation, meaning that it is not checked whether the
 * annotated method really does throw an exception if the boolean expression is true.
 *
 * @checker_framework.manual #type-refinement Automatic type refinement (flow-sensitive type
 *     qualifier inference)
 */
// TODO: Make the following true like TerminatesExecution.
// * <p>This annotation is inherited by subtypes, just as if it were meta-annotated with
//    * {@code @InheritedAnnotation}.
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertMethod {}
