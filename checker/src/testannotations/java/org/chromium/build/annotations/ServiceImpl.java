// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/ServiceImpl.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a META-INF/services file that maps the annotated class to the given service class.
 *
 * <p>This is the same as @AutoService, but directly supported in Chrome's build system (does not
 * require an annotation processor).
 */
@Target({ElementType.TYPE})
// Must be CLASS retention in order to cause compile_java.py to not skip @ServiceImpl parsing via
// its incremental mode.
@Retention(RetentionPolicy.CLASS)
public @interface ServiceImpl {
  /** The service implemented by this class. */
  Class<?> value();
}
