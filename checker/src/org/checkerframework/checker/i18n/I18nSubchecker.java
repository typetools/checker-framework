package org.checkerframework.checker.i18n;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A type-checker that checks that only localized {@code String}s are visible to the user.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@RelevantJavaTypes(CharSequence.class)
public class I18nSubchecker extends BaseTypeChecker {}
