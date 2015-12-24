package org.checkerframework.checker.i18nformatter;

import javax.annotation.processing.SupportedOptions;

import org.checkerframework.common.basetype.BaseTypeChecker;

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
@SupportedOptions( {"bundlenames", "propfiles"} )
public class I18nFormatterChecker extends BaseTypeChecker {
}
