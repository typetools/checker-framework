// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/UsedReflectively.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells Error Prone that annotated types should not be considered unused.
 *
 * <p>Package-private since other users should use com.google.errorprone.annotations.Keep, which is
 * not used here in order to avoid a dependency from //build.
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.CLASS)
@interface UsedReflectively {}
