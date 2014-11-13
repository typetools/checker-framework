package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.framework.qual.EnsuresQualifierIf;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.experimental.regex_qual.qual.*;
*/

// This class should be kept in sync with org.checkerframework.checker.regex.RegexUtil.

/**
 * This is a copy of RegexUtil, but with different Annotations.
 * A better solution might be to allow multiple @EnsuresQualifierIf
 *
 * @see org.checkerframework.checker.regex.RegexUtil
 */
@SuppressWarnings("purity")
public class RegexUtil extends org.checkerframework.checker.regex.RegexUtil {

    protected RegexUtil() {
        super();
    }

    /**
     * {@inheritDoc}
     */
  /*@Pure*/
    @EnsuresQualifierIf(result=true, expression="#1",
            qualifier=org.checkerframework.checker.experimental.regex_qual.qual.Regex.class)
    public static boolean isRegex(String s) {
        return org.checkerframework.checker.regex.RegexUtil.isRegex(s, 0);
    }

    /**
     {@inheritDoc}
     */
  /*>>>
  @SuppressWarnings("regex")    // RegexUtil
  */
  /*@Pure*/
    @EnsuresQualifierIf(result=true, expression="#1",
            qualifier=org.checkerframework.checker.experimental.regex_qual.qual.Regex.class)
    public static boolean isRegex(final char c) {
        return org.checkerframework.checker.regex.RegexUtil.isRegex(c);
    }

    /**
     * {@inheritDoc}
     */
  /*@SideEffectFree*/
    public static /*@org.checkerframework.checker.experimental.regex_qual.qual.Regex*/ String asRegex(String s) {
        return org.checkerframework.checker.regex.RegexUtil.asRegex(s);
    }

    /**
     * {@inheritDoc}
     */
  /*>>>
  @SuppressWarnings("regex")    // RegexUtil
  */
  /*@SideEffectFree*/
    public static /*@org.checkerframework.checker.experimental.regex_qual.qual.Regex*/ String asRegex(String s, int groups) {
        return org.checkerframework.checker.regex.RegexUtil.asRegex(s, groups);
    }
}
