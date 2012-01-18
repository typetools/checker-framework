package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The type representation used by the {@link Class#getName()},
 * {@link Class#forName(String)}, and {@link Class#forName(String, boolean,
 * ClassLoader)} methods.  This format is:
 * <ul>
 *   <li>the {@link BinaryName binary name} for any non-array type</li>
 *   <li>the {@link FieldDescriptor field descriptor} for any array type</li>
 * </ul>
 * <p>
 * Examples include
 * <pre>
 *   java.lang.String
 *   [Ljava.lang.Object;
 *   int
 *   [[[I
 * </pre>
 */
@TypeQualifier
@SubtypeOf(UnannotatedString.class)
@ImplicitFor(stringPatterns="(^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?$)|^\\[+([BCDFIJSZ]|L[A-Za-z_][A-Za-z_0-9]*(/[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?;)$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ClassGetName {}
