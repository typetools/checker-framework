package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a field descriptor (JVM type format) as defined in the <a
 * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2">Java Virtual Machine Specification, section 4.3.2</a>.
 * <p>
 * For example, in
 * <pre>
 *  package checkers.signature;
 *  public class SignatureChecker {
 *    private class Inner {}
 *  }
 * </pre>
 * the field descriptors for the two types are
 * Lcheckers/signature/SignatureChecker; and
 * Lcheckers/signature/SignatureChecker$Inner;.
 */
@TypeQualifier
@SubtypeOf(UnannotatedString.class)
@ImplicitFor(stringPatterns="^\\[*([BCDFIJSZ]|L[A-Za-z_][A-Za-z_0-9]*(/[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?;)$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FieldDescriptor {}
