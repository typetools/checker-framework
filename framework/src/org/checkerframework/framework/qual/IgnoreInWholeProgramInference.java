package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used two ways:
 *
 * <p>1. As a meta-annotation indicating that an annotation type prevents whole-program inference.
 * For example, if the definition of {@code @Inject} is meta-annotated with
 * {@code @IgnoreInWholeProgramInference}:<br>
 * <tt>@IgnoreInWholeProgramInference</tt><br>
 * <tt>@interface Inject { }</tt><br>
 * then no type qualifier will be inferred for any field annotated by {@code @Inject}.
 *
 * <p>This is appropriate for fields that are set reflectively, so there are no calls in client code
 * that type inference can learn from. Examples of qualifiers that should be meta-annotated with
 * {@code @IgnoreInWholeProgramInference} include <a
 * href="https://docs.oracle.com/javaee/7/api/javax/inject/Inject.html">{@code @Inject}</a>, <a
 * href="https://docs.oracle.com/javaee/7/api/javax/inject/Singleton.html">{@code @Singleton}</a>,
 * and <a
 * href="https://types.cs.washington.edu/plume-lib/api/plume/Option.html">{@code @Option}</a>.
 *
 * <p>2. As a field annotation indicating that no type qualifier will be inferred for the field it
 * annotates.
 *
 * @see
 *     org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes#updateInferredFieldType
 * @checker_framework.manual #whole-program-inference-ignores-some-code Whole-program inference
 *     ignores some code
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
public @interface IgnoreInWholeProgramInference {}
