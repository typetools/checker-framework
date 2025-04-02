// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/Nullable.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Based on jspecify's @Nullable and checked by NullAway.
 *
 * <p>Not directly using jspecify's annotations so that Cronet does not need the extra dep.
 *
 * <p>See: https://github.com/uber/NullAway/wiki/Supported-Annotations
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.CLASS)
public @interface Nullable {}
