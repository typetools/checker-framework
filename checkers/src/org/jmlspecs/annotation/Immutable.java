package org.jmlspecs.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
/** Defines the 'immutable' JML annotation (not currently standard) */

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Immutable {

}
