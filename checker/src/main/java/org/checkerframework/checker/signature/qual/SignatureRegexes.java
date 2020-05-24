package org.checkerframework.checker.signature.qual;

import java.lang.reflect.Field;

/** This class exists to define regexes that can be referenced in qualifier definitions. */
public class SignatureRegexes {

    /** Do not instantiate this class. */
    private SignatureRegexes() {}

    /** A regex that matches keywords, but without any regex grouping (or anchoring). */
    private static final String KEYWORD =
            String.join(
                    "|",
                    "abstract",
                    "assert",
                    "boolean",
                    "break",
                    "byte",
                    "case",
                    "catch",
                    "char",
                    "class",
                    "const",
                    "continue",
                    "default",
                    "do",
                    "double",
                    "else",
                    "enum",
                    "extends",
                    "final",
                    "finally",
                    "float",
                    "for",
                    "if",
                    "goto",
                    "implements",
                    "import",
                    "instanceof",
                    "int",
                    "interface",
                    "long",
                    "native",
                    "new",
                    "package",
                    "private",
                    "protected",
                    "public",
                    "return",
                    "short",
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

    /**
     * A regex that matches identifier tokens that are not identifiers (keywords, boolean literals,
     * and the null literal).
     */
    @SuppressWarnings("UnusedVariable") // temporary
    private static final String KEYWORD_OR_LITERAL =
            String.join("|", KEYWORD, "true", "false", "null");

    /** A regex that matches Java identifier tokens, as defined by the Java grammar. */
    private static final String IDENTIFIER_TOKEN = "[A-Za-z_][A-Za-z_0-9]*";

    /** A grouped regex that matches identifiers. */
    private static final String IDENTIFIER =
            // "(?!" + KEYWORD_OR_LITERAL + ")" +
            IDENTIFIER_TOKEN;

    // Strings to use in annotation definitions.  They are defined here because the expression in
    // the annotation definition cannot do computation.

    /** An anchored regex that matches Identifier strings. */
    public static final String Identifier_ANCHORED = "^" + IDENTIFIER + "$";

    /** An anchored regex that matches FieldDescriptor strings. */
    public static final String FieldDescriptor_ANCHORED =
            "^\\[*([BCDFIJSZ]|L" + IDENTIFIER + "(/" + IDENTIFIER + ")*(\\$[A-Za-z_0-9]+)*;)$";

    /**
     * An anchored regex that matches FieldDescriptorForPrimitiveOrArrayInUnnamedPackage strings.
     */
    public static final String FieldDescriptorForPrimitiveOrArrayInUnnamedPackage_ANCHORED =
            "^([BCDFIJSZ]|\\[+[BCDFIJSZ]|\\[L" + IDENTIFIER + "(\\$[A-Za-z_0-9]+)*;)$";

    /** An anchored regex that matches FqBinaryName strings. */
    public static final String FqBinaryName_ANCHORED =
            "^" + IDENTIFIER + "(\\." + IDENTIFIER + ")*(\\$[A-Za-z_0-9]+)*(\\[\\])*$";

    /** An anchored regex that matches BinaryNameInUnnamedPackage strings. */
    public static final String BinaryNameInUnnamedPackage_ANCHORED =
            "^" + IDENTIFIER + "(\\$[A-Za-z_0-9]+)*$";

    /** An anchored regex that matches InternalForm strings. */
    public static final String InternalForm_ANCHORED =
            "^" + IDENTIFIER + "(/" + IDENTIFIER + ")*(\\$[A-Za-z_0-9]+)*$";

    /** An anchored regex that matches FullyQualifiedName strings. */
    public static final String FullyQualifiedName_ANCHORED =
            "^" + IDENTIFIER + "(\\." + IDENTIFIER + ")*(\\[\\])*$";

    /** An anchored regex that matches ClassGetSimpleName strings. */
    public static final String ClassGetSimpleName_ANCHORED = "^(|" + IDENTIFIER + ")(\\[\\])*$";

    /** An anchored regex that matches IdentifierOrArray strings. */
    public static final String IdentifierOrArray_ANCHORED = "^" + IDENTIFIER + "(\\[\\])*$";

    /** An anchored regex that matches ClassGetName strings. */
    public static final String ClassGetName_ANCHORED =
            "(^"
                    + IDENTIFIER
                    + "(\\."
                    + IDENTIFIER
                    + "|\\$[A-Za-z_0-9]+)*$)|^\\[+([BCDFIJSZ]|L"
                    + IDENTIFIER
                    + "(\\."
                    + IDENTIFIER
                    + "|\\$[A-Za-z_0-9]+)*;)$";

    /** An anchored regex that matches BinaryName strings. */
    public static final String BinaryName_ANCHORED =
            "^" + IDENTIFIER + "(\\." + IDENTIFIER + ")*(\\$[A-Za-z_0-9]+)*$";

    /** An anchored regex that matches DotSeparatedIdentifiers strings. */
    public static final String DotSeparatedIdentifiers_ANCHORED =
            "^" + IDENTIFIER + "(\\." + IDENTIFIER + ")*$";

    /** The annotations for which main should output a stringPatterns value. */
    private static final String[] annotationNames =
            new String[] {
                "BinaryNameInUnnamedPackage",
                "BinaryName",
                "ClassGetName",
                "ClassGetSimpleName",
                "DotSeparatedIdentifiers",
                "FieldDescriptor",
                "FieldDescriptorForPrimitiveOrArrayInUnnamedPackage",
                "FqBinaryName",
                "FullyQualifiedName",
                "IdentifierOrArray",
                "Identifier",
                "InternalForm",
            };

    // Run like this:
    //   (cd $CHECKERFRAMEWORK && ./gradlew assemble) && \
    //   java -cp $CHECKERFRAMEWORK/checker/dist/checker.jar \
    //     org.checkerframework.checker.signature.qual.SignatureRegexes
    // Then, execute the commands it outputs.
    /**
     * Produce text for annotation definitions.
     *
     * @param args ignored
     * @throws IllegalAccessException if reflection fails
     * @throws NoSuchFieldException if reflection fails
     */
    public static void main(String[] args) throws IllegalAccessException, NoSuchFieldException {
        Class<SignatureRegexes> clazz = SignatureRegexes.class;
        for (String annotationName : annotationNames) {
            String fieldName = annotationName + "_ANCHORED";
            Field f = clazz.getDeclaredField(fieldName);
            String regex = (String) f.get(null);
            System.out.printf(
                    "sed -i 's:^ */\\* Do not edit; see SignatureRegexes.java.*:                /* Do not edit; see SignatureRegexes.java */ \"%s\"):' %s.java%n",
                    regex.replace("\\", "\\\\\\\\"), annotationName);
        }
    }
}
