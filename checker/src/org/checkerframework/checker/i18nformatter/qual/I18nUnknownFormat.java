package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The top qualifier.
 *
 * A type annotation indicating that the run-time value might or might not
 * be a valid i18n format string.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({})
public @interface I18nUnknownFormat {
}
