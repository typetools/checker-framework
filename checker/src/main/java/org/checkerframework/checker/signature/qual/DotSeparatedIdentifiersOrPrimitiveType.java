package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.FullyQualifiedName fully qualified name} and a valid
 * {@linkplain org.checkerframework.checker.signature.qual.BinaryNameOrPrimitiveType binary name or
 * primitive type}. It represents a primitive type or a non-array, non-inner class, where the latter
 * is represented by dot-separated identifiers.
 *
 * <p>Examples: int, MyClass, java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({FullyQualifiedName.class, BinaryNameOrPrimitiveType.class})
@QualifierForLiterals(
        stringPatterns =
                /* Do not edit; see SignatureRegexes.java */ "^((?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*(\\.(?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*)*|boolean|byte|char|double|float|int|long|short)$")
public @interface DotSeparatedIdentifiersOrPrimitiveType {}
