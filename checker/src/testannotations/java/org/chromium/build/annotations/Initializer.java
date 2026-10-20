// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/Initializer.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Causes a method to treated as a constructor for the sake of null checking.
 *
 * <p>See: https://github.com/uber/NullAway/wiki/Supported-Annotations
 *
 * <p>Not directly using NullAway's annotations so that Cronet does not need the extra dep.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Initializer {}
