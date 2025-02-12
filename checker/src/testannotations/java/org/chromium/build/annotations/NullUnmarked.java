// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/NullUnmarked.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ensures the annotated class or method is not checked by NullAway. */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
public @interface NullUnmarked {}
