package org.checkerframework.checker.i18nformatter;

import javax.annotation.processing.SupportedOptions;

import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatBottom;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nUnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker plug-in for the qualifier that finds syntactically invalid
 * i18n-formatter calls (MessageFormat.format()).
 *
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 *
 */
@TypeQualifiers({ I18nUnknownFormat.class, I18nFormat.class,
      I18nFormatBottom.class, I18nInvalidFormat.class,
      I18nFormatFor.class })
@SupportedOptions( {"bundlenames", "propfiles"} )
public class I18nFormatterChecker extends BaseTypeChecker {
}
