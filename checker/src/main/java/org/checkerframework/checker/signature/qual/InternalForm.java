package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The syntax for binary names that appears in a class file, as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.2">JVM
 * Specification, section 4.2</a>. A {@linkplain BinaryName binary name} is conceptually the name
 * for the class or interface in a compiled binary, but the actual representation of that name in
 * its class file is slightly different.
 *
 * <p>Internal form is the same as the binary name, but with periods ({@code .}) replaced by forward
 * slashes ({@code /}).
 *
 * <p>Programmers more often use the binary name, leaving the internal form as a JVM implementation
 * detail.
 *
 * @see BinaryName
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SignatureUnknown.class)
@QualifierForLiterals(
        stringPatterns =
                /* Do not edit; see SignatureRegexes.java */ "^(?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*(/(?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_0-9]+)*$")
public @interface InternalForm {}
