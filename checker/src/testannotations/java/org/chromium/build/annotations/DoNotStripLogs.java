// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/DoNotStripLogs.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The annotated method or class will have -maximumremovedandroidloglevel 0 applied to it. */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface DoNotStripLogs {}
