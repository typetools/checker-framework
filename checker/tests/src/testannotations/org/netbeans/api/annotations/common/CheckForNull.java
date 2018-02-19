// Upstream version (update version number when a new NetBeans release appears):
// http://bits.netbeans.org/8.2/javadoc/org-netbeans-api-annotations-common/org/netbeans/api/annotations/common/CheckForNull.html

package org.netbeans.api.annotations.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface CheckForNull {}
