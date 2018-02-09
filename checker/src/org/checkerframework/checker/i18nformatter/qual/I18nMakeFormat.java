package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used internally to annotate {@link java.util.ResourceBundle#getString}
 * indicating the checker to check if the given key exist in the translation file and annotate the
 * result string with the correct format annotation according to the corresponding key's value. This
 * is done in {@link org.checkerframework.checker.i18nformatter.I18nFormatterTransfer}
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface I18nMakeFormat {}
