package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.i18nformatter.I18nFormatUtil;

/**
 * This annotation is used internally to annotate {@link I18nFormatUtil#isFormat}
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 * @author Siwakorn Srisakaokul
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface I18nValidFormat {}
