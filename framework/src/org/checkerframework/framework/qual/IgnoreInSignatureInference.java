package org.checkerframework.framework.qual;

import java.lang.annotation.*;

/**
 * A meta-annotation indicating that an annotation type prevents signature
 * inference. For example, if the definition of &#064;Option is meta-annotated
 * with &#064;IgnoreInSignatureInference:<br>
 *   <tt>@IgnoreInSignatureInference</tt><br>
 *   <tt>@interface Option { }</tt><br>
 * then no type qualifier will be inferred for any field annotated by &#064;Option.
 * <p>
 * See {@link org.checkerframework.common.signatureinference.SignatureInferenceScenes#updateInferredFieldType}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface IgnoreInSignatureInference { }