package org.checkerframework.checker.signature;

/** This class defines stringPattern regexes for the Signature Checker. */
public class SignatureRegexes {

    /** Do not instantiate this class. */
    private SignatureRegexes() {
        throw new Error("Do not instantiate");
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Functions on regular expressions
    ///

    /**
     * Create a capturing group.
     *
     * @param arg a regular expression
     * @return the argument wrapped in a capturing group
     */
    private static final String GROUPED(String arg) {
        return "(" + arg + ")";
    }

    /**
     * Create a regex matching zero or more of the given argument (Kleene star).
     *
     * @param arg a regular expression
     * @return the argument, repeated zero or more times
     */
    private static final String ANY(String arg) {
        return GROUPED(arg) + "*";
    }

    /**
     * Create a regex that must match the entire string.
     *
     * @param arg a regular expression
     * @return the argument, made to match the entire string
     */
    private static final String ANCHORED(String arg) {
        return "^" + arg + "$";
    }

    /**
     * An ungrouped alternation.
     *
     * @param args regular expressions
     * @return a regex that matches any one of the arguments
     */
    private static final String ALTERNATE(String... args) {
        return String.join("|", args);
    }

    /**
     * A grouped alternation.
     *
     * @param args regular expressions
     * @return a regex that matches any one of the arguments, wrapped in a capturing group
     */
    private static final String GROUPED_ALTERNATE(String... args) {
        return GROUPED(ALTERNATE(args));
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Building blocks for regular expressions
    ///

    /** An unanchored regex that matches keywords, except primitive types. */
    private static final String KEYWORD_NON_PRIMITIVE_TYPE =
            String.join(
                    "|",
                    "abstract",
                    "assert",
                    // "boolean",
                    "break",
                    // "byte",
                    "case",
                    "catch",
                    // "char",
                    "class",
                    "const",
                    "continue",
                    "default",
                    "do",
                    // "double",
                    "else",
                    "enum",
                    "extends",
                    "final",
                    "finally",
                    // "float",
                    "for",
                    "if",
                    "goto",
                    "implements",
                    "import",
                    "instanceof",
                    // "int",
                    "interface",
                    // "long",
                    "native",
                    "new",
                    "package",
                    "private",
                    "protected",
                    "public",
                    "return",
                    // "short",
                    "static",
                    "strictfp",
                    "super",
                    "switch",
                    "synchronized",
                    "this",
                    "throw",
                    "throws",
                    "transient",
                    "try",
                    "void",
                    "volatile",
                    "while");

    /** An unanchored regex that matches primitive types. */
    private static final String PRIMITIVE_TYPE =
            String.join("|", "boolean", "byte", "char", "double", "float", "int", "long", "short");

    /** A regex that matches field descriptors for primitive types. */
    private static final String FD_PRIMITIVE = "[BCDFIJSZ]";

    /** An unanchored regex that matches keywords. */
    private static final String KEYWORD = KEYWORD_NON_PRIMITIVE_TYPE + "|" + PRIMITIVE_TYPE;

    /**
     * A regex that matches identifier tokens that are not identifiers (keywords, boolean literals,
     * and the null literal).
     */
    private static final String KEYWORD_OR_LITERAL =
            String.join("|", KEYWORD, "true", "false", "null");

    /** A regex that matches Java identifier tokens, as defined by the Java grammar. */
    private static final String IDENTIFIER_TOKEN = "[A-Za-z_][A-Za-z_0-9]*";

    /** A grouped regex that matches identifiers. */
    private static final String IDENTIFIER = "(?!" + KEYWORD_OR_LITERAL + ")" + IDENTIFIER_TOKEN;

    /** An anchored regex that matches Identifier strings. */
    public static final String IDENTIFIER_OR_PRIMITIVE_TYPE = ALTERNATE(IDENTIFIER, PRIMITIVE_TYPE);

    /** An unanchored regex that matches DotSeparatedIdentifiers strings. */
    private static final String DOT_SEPARATED_IDENTIFIERS = IDENTIFIER + ANY("\\." + IDENTIFIER);

    /** An unanchored regex that matches slash-separated identifiers. */
    private static final String SLASH_SEPARATED_IDENTIFIERS = IDENTIFIER + ANY("/" + IDENTIFIER);

    /** A regex that matches the nested-class part of a class name, for one nested class. */
    private static final String NESTED_ONE = "\\$[A-Za-z_0-9]+";

    /** A regex that matches the nested-class part of a class name. */
    private static final String NESTED = ANY(NESTED_ONE);

    /** An unanchored regex that matches BinaryName strings. */
    private static final String BINARY_NAME = DOT_SEPARATED_IDENTIFIERS + NESTED;

    /** A regex that matches the nested-class part of a class name. */
    private static final String ARRAY = "(\\[\\])*";

    /** A regex that matches InternalForm strings. */
    public static final String INTERNAL_FORM = SLASH_SEPARATED_IDENTIFIERS + NESTED;

    /** A regex that matches ClassGetName, for non-primitive, non-array types. */
    private static final String CLASS_GET_NAME_NONPRIMITIVE_NONARRAY =
            IDENTIFIER + "(\\." + IDENTIFIER + "|" + NESTED_ONE + ")*";

    ///////////////////////////////////////////////////////////////////////////
    // Regexes for literal Strings, one per annotation definitions.

    /** A regex that matches ArrayWithoutPackage strings. */
    public static final String ArrayWithoutPackage =
            ANCHORED(GROUPED(IDENTIFIER_OR_PRIMITIVE_TYPE) + ARRAY);

    /** A regex that matches BinaryName strings. */
    public static final String BinaryName = ANCHORED(BINARY_NAME);

    /** A regex that matches BinaryNameWithoutPackage strings. */
    public static final String BinaryNameWithoutPackage = ANCHORED(IDENTIFIER + NESTED);

    /** A regex that matches BinaryNameOrPrimitiveType strings. */
    public static final String BinaryNameOrPrimitiveType =
            ANCHORED(GROUPED_ALTERNATE(BINARY_NAME, PRIMITIVE_TYPE));

    /** A regex that matches ClassGetName strings. */
    public static final String ClassGetName =
            ANCHORED(
                    GROUPED_ALTERNATE(
                            // non-array
                            PRIMITIVE_TYPE,
                            CLASS_GET_NAME_NONPRIMITIVE_NONARRAY,
                            // array
                            ("\\[+"
                                    + GROUPED_ALTERNATE(
                                            FD_PRIMITIVE,
                                            "L" + CLASS_GET_NAME_NONPRIMITIVE_NONARRAY + ";"))));

    /** A regex that matches ClassGetSimpleName strings. */
    public static final String ClassGetSimpleName =
            ANCHORED(
                    GROUPED_ALTERNATE(
                                    "", // empty string is a ClassGetSimpleName
                                    IDENTIFIER_OR_PRIMITIVE_TYPE)
                            + ARRAY);

    /** A regex that matches DotSeparatedIdentifiers strings. */
    public static final String DotSeparatedIdentifiers = ANCHORED(DOT_SEPARATED_IDENTIFIERS);

    /** A regex that matches DotSeparatedIdentifiersOrPrimitiveType strings. */
    public static final String DotSeparatedIdentifiersOrPrimitiveType =
            ANCHORED(GROUPED_ALTERNATE(DOT_SEPARATED_IDENTIFIERS, PRIMITIVE_TYPE));

    /** A regex that matches FieldDescriptor strings. */
    public static final String FieldDescriptor =
            ANCHORED("\\[*(" + FD_PRIMITIVE + "|L" + INTERNAL_FORM + ";)");

    /** A regex that matches FieldDescriptorWithoutPackage strings. */
    public static final String FieldDescriptorWithoutPackage =
            ANCHORED(
                    "("
                            + FD_PRIMITIVE
                            + "|\\[+"
                            + FD_PRIMITIVE
                            + "|\\[L"
                            + IDENTIFIER
                            + NESTED
                            + ";)");

    /** A regex that matches FieldDescriptorForPrimitive strings. */
    public static final String FieldDescriptorForPrimitive = ANCHORED("^[BCDFIJSZ]$");

    /** A regex that matches FqBinaryName strings. */
    public static final String FqBinaryName =
            ANCHORED("(" + PRIMITIVE_TYPE + "|" + BINARY_NAME + ")" + ARRAY);

    /** A regex that matches FullyQualifiedName strings. */
    public static final String FullyQualifiedName =
            ANCHORED("(" + PRIMITIVE_TYPE + "|" + DOT_SEPARATED_IDENTIFIERS + ")" + ARRAY);

    /** A regex that matches Identifier strings. */
    public static final String Identifier = ANCHORED(IDENTIFIER);

    /** A regex that matches IdentifierOrPrimitiveType strings. */
    public static final String IdentifierOrPrimitiveType = ANCHORED(IDENTIFIER_OR_PRIMITIVE_TYPE);

    /** A regex that matches InternalForm strings. */
    public static final String InternalForm = ANCHORED(INTERNAL_FORM);

    /** A regex that matches PrimitiveType strings. */
    public static final String PrimitiveType = ANCHORED(PRIMITIVE_TYPE);
}
