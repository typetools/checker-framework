package org.checkerframework.checker.index.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated expression evaluates to an integer greater than or equal to -1. ("GTE" stands for
 * ``Greater Than or Equal to''.)
 *
 * <p>As an example use case, consider the definition of the read() method in java.io.InputStream:
 *
 * <pre>
 *
 *      Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.
 *      If no byte is available because the end of the stream has been reached, the value -1 is returned.
 *      This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
 *      A subclass must provide an implementation of this method.
 *
 *      Returns: the next byte of data, or -1 if the end of the stream is reached.
 *      Throws: IOException - if an I/O error occurs.
 *
 *     {@code public abstract @GTENegativeOne int read() throws IOException;}
 * </pre>
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({LowerBoundUnknown.class})
public @interface GTENegativeOne {}
