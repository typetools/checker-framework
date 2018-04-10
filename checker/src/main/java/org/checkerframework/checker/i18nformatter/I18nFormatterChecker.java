package org.checkerframework.checker.i18nformatter;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A type-checker plug-in for the qualifier that finds syntactically invalid i18n-formatter calls
 * (MessageFormat.format()).
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@SupportedOptions({"bundlenames", "propfiles"})
@RelevantJavaTypes(CharSequence.class)
public class I18nFormatterChecker extends BaseTypeChecker {}
