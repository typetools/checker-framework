package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a {@link FieldDescriptor field descriptor} for a primitive or for an array whose base
 * type is primitive or in the unnamed package.
 *
 * <p>Examples: I [[J MyClass MyClass$22 [LMyClass; [LMyClass$22
 *
 * <p>Field descriptor (JVM type format) is defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.3.2">Java Virtual
 * Machine Specification, section 4.3.2</a>.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ClassGetName.class, FieldDescriptor.class})
@QualifierForLiterals(
        stringPatterns =
                /* Do not edit; see SignatureRegexes.java */ "^([BCDFIJSZ]|\\[+[BCDFIJSZ]|\\[L(?!abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|if|goto|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|boolean|byte|char|double|float|int|long|short|true|false|null)[A-Za-z_][A-Za-z_0-9]*(\\$[A-Za-z_0-9]+)*;)$")
public @interface FieldDescriptorWithoutPackage {}
