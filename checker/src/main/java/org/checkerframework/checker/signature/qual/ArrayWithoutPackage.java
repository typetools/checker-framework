package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An identifier or primitive type, followed by any number of array square brackets.
 *
 * <p>Example: Foobar[][] Example: Baz22
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({FullyQualifiedName.class, ClassGetSimpleName.class})
@QualifierForLiterals(
        stringPatterns =
                /* Do not edit; see SignatureRegexes.java */ "^((?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*|boolean|byte|char|double|float|int|long|short)(\\[\\])*$")
public @interface ArrayWithoutPackage {}
