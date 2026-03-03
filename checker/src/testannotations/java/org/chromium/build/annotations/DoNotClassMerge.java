// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/DoNotClassMerge.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated class should never be horizontally or vertically merged.
 *
 * <p>The annotated classes are guaranteed not to be horizontally or vertically merged by Proguard.
 * Other optimizations may still apply.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface DoNotClassMerge {}
