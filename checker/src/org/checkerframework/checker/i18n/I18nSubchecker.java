package org.checkerframework.checker.i18n;

import org.checkerframework.checker.i18n.qual.Localized;
import org.checkerframework.checker.i18n.qual.UnknownLocalized;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker that checks that only localized {@code String}s are visible
 * to the user.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@TypeQualifiers( {Localized.class, UnknownLocalized.class} )
public class I18nSubchecker extends BaseTypeChecker {
}
