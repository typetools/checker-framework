package org.checkerframework.framework.qual;

import java.lang.annotation.*;

/**
 * A meta-annotation indicating that the annotated annotation won't be taken
 * into consideration during the signature inference process.
 * <p>
 * See {@link org.checkerframework.common.signatureinference.SignatureInferenceScenes}}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface IgnoreInSignatureInference { }