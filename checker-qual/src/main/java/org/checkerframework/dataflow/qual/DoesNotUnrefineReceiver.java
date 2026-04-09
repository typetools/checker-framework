package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At a call to a side-effecting method, the framework ordinarily discards all refined type
 * information, since the side-effecting method might invalidate that information. This annotation
 * indicates that, at a call to the annotated method, the receiver's type should not be unrefinde.
 * That is, a call to the method does not affect the type qualifier (in the given hierarchy).
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface DoesNotUnrefineReceiver {}
