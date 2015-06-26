package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import java.lang.annotation.*;

/**
 * Indicates that the annotated qualifier is the default qualifier on types
 * from unchecked bytecode.  For example code imported from a jar
 * file which does not have a stub file describing it.  This allows you to set
 * safe defaults for unchecked code.
 *
 * Note, this annotations is analogous to DefaultQualifierInHierarchy but for
 * unchecked bytecode.
 * @see DefaultForInUncheckedBytecode
 * <p>
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * {@code DefaultQualifierInTyped}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface DefaultQualifierInUncheckedBytecode {}
