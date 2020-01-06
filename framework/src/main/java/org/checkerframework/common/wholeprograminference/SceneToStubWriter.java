package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AElement;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
import scenelib.annotations.el.AnnotationDef;
import scenelib.annotations.el.DefCollector;
import scenelib.annotations.el.DefException;
import scenelib.annotations.el.InnerTypeLocation;
import scenelib.annotations.field.AnnotationAFT;
import scenelib.annotations.field.AnnotationFieldType;
import scenelib.annotations.field.ArrayAFT;
import scenelib.annotations.field.BasicAFT;
import scenelib.annotations.field.ClassTokenAFT;
import scenelib.annotations.util.Strings;

/**
 * SceneToStubWriter provides two static methods named {@code write} that write a {@link AScene} in
 * stub file format, to a {@link Writer} {@link #write(AScene, Map, Map, Map, Writer)}, or to a file
 * {@link #write(AScene, Map, Map, Map, Writer)}. This class is the equivalent of {@code
 * IndexFileWriter} from the Annotation File Utilities, but outputs the results in the stub file
 * format instead of jaif format.
 *
 * <p>You can use this writer instead of {@code IndexFileWriter} by passing the {@code
 * -Ainfer=stubs} command-line argument.
 */
public final class SceneToStubWriter {

    /** How far to indent when writing members of a stub file. */
    private static final String INDENT = "    ";

    /**
     * A map from the {@code description} field of an ATypeElement to the corresponding unqualified
     * Java types, since {@code AScene}s don't carry that information. See the comment on the {@code
     * basetypes} field of {@link WholeProgramInferenceScenesHelper} for more information.
     */
    private Map<String, TypeMirror> basetypes;

    /**
     * A map from fully-qualified class names to the TypeElement that represents them. Computed by
     * {@link WholeProgramInferenceScenes}. Used to output the names of generic parameters to
     * classes.
     */
    private Map<@FullyQualifiedName String, TypeElement> types;

    /**
     * Map from the fully-qualified name of each enum to its list of enum constants.
     *
     * <p>The stub parser can't parse an enum that's labeled "class", but {@link AClass} doesn't
     * specify if a class is an enum. So track which classes are enums. In addition, enum constants
     * need to be present in the stub file.
     */
    private Map<@FullyQualifiedName String, List<VariableElement>> enumConstants;

    /**
     * Create a new SceneToStubWriter.
     *
     * @param basetypes a map from the description of {@code ATypeElement}s to the {@code
     *     TypeMirror}s that represent their base Java types
     * @param types a map from fully-qualified names to the {@code TypeElement}s representing their
     *     declarations
     * @param enumConstants a map from fully-qualified enum names to the enum constants defined in
     *     that name
     */
    private SceneToStubWriter(
            Map<String, TypeMirror> basetypes,
            Map<@FullyQualifiedName String, TypeElement> types,
            Map<@FullyQualifiedName String, List<VariableElement>> enumConstants) {
        this.basetypes = basetypes;
        this.types = types;
        this.enumConstants = enumConstants;
    }

    /**
     * Writes the annotations in {@code scene} to {@code out} in stub file format.
     *
     * @param scene the scene to write out
     * @param basetypes a map from the description of {@code ATypeElement}s to the {@code
     *     TypeMirror}s that represent their base Java types
     * @param types a map from fully-qualified names to the {@code ATypeElement}s representing their
     *     declarations
     * @param enumConstants a map from fully-qualified enum names to the enum constants defined in
     *     that name
     * @param out the Writer to output the result to
     */
    public static void write(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<@FullyQualifiedName String, TypeElement> types,
            Map<@FullyQualifiedName String, List<VariableElement>> enumConstants,
            Writer out) {
        SceneToStubWriter writer = new SceneToStubWriter(basetypes, types, enumConstants);
        writer.writeImpl(scene, new PrintWriter(out));
    }

    /**
     * Writes the annotations in {@code scene} to the file {@code filename} in stub file format.
     *
     * @param scene the scene to write out
     * @param basetypes a map from the description of {@code ATypeElement}s to the {@code
     *     TypeMirror}s that represent their base Java types
     * @param types a map from fully-qualified names to the {@code ATypeElement}s representing their
     *     declarations
     * @param enumNamesToEnumConstant a map from fully-qualified enum names to the enum constants
     *     defined in that name
     * @param filename the path of the file to write to
     * @throws IOException if the file doesn't exist
     * @see #write(AScene, Map, Map, Map, Writer)
     */
    public static void write(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<@FullyQualifiedName String, TypeElement> types,
            Map<@FullyQualifiedName String, List<VariableElement>> enumNamesToEnumConstant,
            String filename)
            throws IOException {
        write(scene, basetypes, types, enumNamesToEnumConstant, new FileWriter(filename));
    }

    /**
     * The part of a binary name that specifies the package.
     *
     * @param className the binary name of a class
     * @return the part of the name referring to the package
     */
    private static String packagePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? "" : className.substring(0, lastdot);
    }

    /**
     * The part of a binary name that specifies the basename of the class. This method replaces the
     * {@code $}s in the names of inner classes with {@code .}s, so that they can be printed
     * correctly in stub files.
     *
     * @param className a binary name
     * @return the part of the name representing the class's name without its package
     */
    private static String basenamePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        String result = (lastdot == -1) ? className : className.substring(lastdot + 1);
        return result.replace('$', '.');
    }

    /**
     * Converts a binary name of a Java class (i.e. using the $ syntax for Outer$Inner) to the
     * fully-qualified name (i.e. using a dot to separate inner classes, instead).
     *
     * @param binaryName the binary name of a Java class
     * @return the fully-qualified name of that Java class
     */
    @SuppressWarnings("signature") // TODO: replace with a call to an appropriate JDK method
    private static @FullyQualifiedName String convertBinaryToFullyQualified(
            @BinaryName String binaryName) {
        return binaryName.replace('$', '.');
    }

    /**
     * Formats a literal; used when printing the arguments of annotations. Similar to {@code
     * IndexFileWriter#printValue}, but does not print directly. Instead, returns the result to be
     * printed.
     *
     * @param aft the annotation whose values are being formatted, for context
     * @param o the value or values to format
     * @return the String representation of the value
     */
    private String formatAnnotationValue(AnnotationFieldType aft, Object o) {
        if (aft instanceof AnnotationAFT) {
            return formatAnnotation((Annotation) o);
        } else if (aft instanceof ArrayAFT) {
            StringBuilder result = new StringBuilder();
            ArrayAFT aaft = (ArrayAFT) aft;
            result.append('{');
            if (!(o instanceof List)) {
                result.append(formatAnnotationValue(aaft.elementType, o));
            } else {
                List<?> l = (List<?>) o;
                // watch out--could be an empty array of unknown type
                // (see AnnotationBuilder#addEmptyArrayField)
                if (aaft.elementType == null) {
                    if (l.size() != 0) {
                        throw new AssertionError();
                    }
                } else {
                    StringJoiner sj = new StringJoiner(",");
                    for (Object o2 : l) {
                        sj.add(formatAnnotationValue(aaft.elementType, o2));
                    }
                    result.append(sj.toString());
                }
            }
            result.append("}");
            return result.toString();
        } else if (aft instanceof ClassTokenAFT) {
            return aft.format(o);
        } else if (aft instanceof BasicAFT && o instanceof String) {
            return Strings.escape((String) o);
        } else if (aft instanceof BasicAFT && o instanceof Long) {
            return o.toString() + "L";
        } else {
            return o.toString();
        }
    }

    /**
     * Returns the String representation of an annotation in Java source format.
     *
     * @param a the annotation to print
     * @return the formatted annotation
     */
    private String formatAnnotation(Annotation a) {
        StringBuilder result = new StringBuilder();
        result.append("@" + a.def().name.substring(a.def().name.lastIndexOf('.') + 1));
        if (!a.fieldValues.isEmpty()) {
            result.append('(');
            StringJoiner sj = new StringJoiner(",");
            for (Map.Entry<String, Object> f : a.fieldValues.entrySet()) {
                sj.add(
                        f.getKey()
                                + "="
                                + formatAnnotationValue(
                                        a.def().fieldTypes.get(f.getKey()), f.getValue()));
            }
            result.append(sj.toString());
            result.append(')');
        }
        return result.toString();
    }

    /**
     * Returns all annotations in {@code annos}, separated by spaces, in a form suitable to be
     * printed as Java source code.
     *
     * <p>This method also adds a trailing space.
     *
     * <p>Internal JDK annotations, such as jdk.Profile+Annotation, are ignored. These annotations
     * are identified using the convention that their names contain "+".
     *
     * @param annos the annotations to format
     * @see #formatAnnotation(Annotation)
     */
    private String formatAnnotations(Collection<? extends Annotation> annos) {
        StringJoiner sj = new StringJoiner(" ");
        for (Annotation tla : annos) {
            if (!tla.def.name.contains("+")) {
                sj.add(formatAnnotation(tla));
            }
        }
        return sj.toString() + " ";
    }

    /**
     * Formats the annotations on the component type of an array, if there are any.
     *
     * @param e the array type to format
     * @returns the array type formatted to be written to Java source code
     */
    private String formatArrayComponentTypeAnnotation(ATypeElement e) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<InnerTypeLocation, ATypeElement> ite : e.innerTypes.entrySet()) {
            InnerTypeLocation loc = ite.getKey();
            AElement it = ite.getValue();
            if (loc.location.contains(TypePathEntry.ARRAY)) {
                result.append(formatAnnotations(it.tlAnnotationsHere));
            }
        }
        return result.toString();
    }

    /**
     * Formats an AField so that it can be printed in a stub. An AField represents a variable
     * declaration. In practice, {@code aField} should represent either a field declaration or a
     * formal parameter of a method, because stub files should not contain local variable
     * declarations. It is the responsibility of each caller of this method to ensure that is true.
     *
     * @param aField the field to format
     * @param fieldName the name to use for the declaration in the stub file. This doesn't matter
     *     for parameters, but must be correct for fields.
     * @param className the name of the enclosing class. This is only used for printing the type of
     *     an explicit receiver parameter (i.e. a parameter named "this").
     * @return a String suitable to print in a stub file
     */
    private String formatAField(AField aField, String fieldName, String className) {
        StringBuilder result = new StringBuilder();
        String basetype;
        if ("this".equals(fieldName)) {
            basetype = className;
        } else if (basetypes.containsKey(aField.type.description.toString())) {
            basetype = basetypes.get(aField.type.description.toString()).toString();
        } else {
            throw new BugInCF(
                    "SceneToStubWriter: could not find the base type for this variable declaration: "
                            + fieldName);
        }

        if (basetype.contains("[")) {
            String component = basetype.substring(0, basetype.lastIndexOf('['));
            result.append(formatArrayComponentTypeAnnotation(aField.type));
            result.append(component);
            result.append(" ");
            basetype = "[]";
        }
        result.append(formatAnnotations(aField.type.tlAnnotationsHere));
        result.append(basetype);
        result.append(' ');
        result.append(fieldName);
        return result.toString();
    }

    /**
     * Writes out an import statement for each annotation used in an {@link AScene}.
     *
     * <p>{@code DefCollector} is a facility in the Annotation File Utilities for determining which
     * annotations are used in a given AScene. Here, we use that construct to write out the proper
     * import statements into a stub file.
     */
    private static class ImportDefCollector extends DefCollector {

        private final PrintWriter printWriter;

        /**
         * Constructs a new ImportDefCollector, which will run on the given AScene when its {@code
         * visit} method is called.
         *
         * @param scene the scene whose imported annotations should be printed
         * @throws DefException if the DefCollector does not succeed
         */
        ImportDefCollector(AScene scene, PrintWriter printWriter) throws DefException {
            super(scene);
            this.printWriter = printWriter;
        }

        /**
         * Write an import statement for a given AnnotationDef. This is only called once per
         * annotation used in the scene.
         *
         * @param d the annotation definition to print an import for
         */
        @Override
        protected void visitAnnotationDef(AnnotationDef d) {
            if (!d.name.contains("+")) {
                printWriter.println("import " + d.name + ";");
            }
        }
    }

    /**
     * For a given class, this prints the hierarchy of outer classes, and returns the number of
     * curly braces to close with. The classes are printed with appropriate opening curly braces, in
     * standard Java style. This routine does not attempt to indent them correctly.
     *
     * <p>When an inner class is present in an AScene, its name is something like "Outer.Inner".
     * Writing a stub file with that name would be useless to the stub parser, which expects inner
     * classes to be properly nested.
     *
     * @param basename the simple name of the class (without the package), fully-qualified form
     *     (that is, without any {@code $}s)
     * @param classname the fully-qualified name of the class in question, so that it can be printed
     *     as an enum if it is one
     * @param aClass the AClass representing the classname
     * @param printWriter the writer where the class definition should be printed
     * @return the number of outer classes within which this class is nested
     */
    private int printClassDefinitions(
            String basename,
            @FullyQualifiedName String classname,
            AClass aClass,
            PrintWriter printWriter) {

        String nameToPrint = basename;
        String remainingInnerClassNames = "";
        if (basename.contains(".")) {
            nameToPrint = basename.substring(0, basename.indexOf('.'));
            remainingInnerClassNames = basename.substring(basename.indexOf('.') + 1);
        }

        // Enums cannot be nested within non-classes,
        // so only print as an enum if the leaf has been reached.
        if (enumConstants.containsKey(classname) && "".equals(remainingInnerClassNames)) {
            printWriter.print("enum ");
        } else {
            printWriter.print("class ");
        }
        formatAnnotations(aClass.tlAnnotationsHere);
        printWriter.print(nameToPrint);
        printTypeParameters(classname, printWriter);
        printWriter.println(" {");
        if ("".equals(remainingInnerClassNames)) {
            return 0;
        } else {
            return 1
                    + printClassDefinitions(
                            remainingInnerClassNames, classname, aClass, printWriter);
        }
    }

    /**
     * Prints all the fields of a given class (the AClass).
     *
     * @param aClass the class whose fields should be printed
     * @param fullyQualifiedClassname the fully-qualified name of the class
     * @param printWriter the writer on which to print the fields
     */
    private void printFields(
            AClass aClass,
            @FullyQualifiedName String fullyQualifiedClassname,
            PrintWriter printWriter) {
        for (Map.Entry<String, AField> fieldEntry : aClass.fields.entrySet()) {
            String fieldName = fieldEntry.getKey();
            AField aField = fieldEntry.getValue();
            printWriter.println();
            printWriter.print(INDENT);
            printWriter.print(formatAField(aField, fieldName, fullyQualifiedClassname));
            printWriter.println(";");
        }
    }

    /**
     * Prints a method signature in stub file format (i.e. without a method body).
     *
     * @param aMethod the method to print
     * @param basename the simple name of the containing class. Used only to determine if the method
     *     being printed is the constructor of an inner class.
     * @param fullyQualifiedClassname the fully-qualified name of the containing class
     * @param printWriter where to print the method signature
     */
    private void printMethodSignature(
            AMethod aMethod,
            String basename,
            @FullyQualifiedName String fullyQualifiedClassname,
            PrintWriter printWriter) {
        printWriter.println();
        printWriter.print(INDENT);
        printWriter.print(formatAnnotations(aMethod.returnType.tlAnnotationsHere));
        String methodName = aMethod.methodName.substring(0, aMethod.methodName.indexOf("("));
        // Use Java syntax for constructors.
        if ("<init>".equals(methodName)) {
            // Constructor names cannot contain dots, if this is an inner class.
            methodName =
                    basename.contains(".")
                            ? basename.substring(basename.lastIndexOf('.') + 1)
                            : basename;
        } else {
            // This isn't a constructor, so add a return type if one is available.
            // Note that the stub file format doesn't require this to be correct,
            // so it would be acceptable to print "java.lang.Object" for every
            // method. A better type is printed if one is available to improve
            // the readability of the resulting stub file.
            String descriptionString = aMethod.returnType.description.toString();
            if (basetypes.containsKey(descriptionString)) {
                printWriter.print(basetypes.get(descriptionString));
            } else {
                printWriter.print("java.lang.Object");
            }
            printWriter.print(" ");
        }
        printWriter.print(methodName);
        printWriter.print("(");

        StringJoiner parameters = new StringJoiner(", ");

        if (!aMethod.receiver.type.tlAnnotationsHere.isEmpty()
                || !aMethod.receiver.type.innerTypes.isEmpty()) {
            // Only output the receiver if it has an annotation.
            parameters.add(formatAField(aMethod.receiver, "this", fullyQualifiedClassname));
        }
        for (Integer index : aMethod.parameters.keySet()) {
            AField param = aMethod.parameters.get(index);
            // AMethod doesn't actually track real parameter names.
            // Fortunately, the stub file parser also doesn't, so
            // this code can safely ignore that problem and use a generic
            // name instead.
            //
            // TODO: use the actual parse tree of the method
            // to figure out the real parameter names, and then thread them
            // through to here.
            parameters.add(formatAField(param, "param" + index, fullyQualifiedClassname));
        }
        printWriter.print(parameters.toString());
        printWriter.println(");");
    }

    /**
     * The implementation of {@link #write(AScene, Map, Map, Map, Writer)} and {@link #write(AScene,
     * Map, Map, Map, String)}. Prints imports, classes, method signatures, and fields in stub file
     * format, all with appropriate annotations.
     *
     * @param scene the scene to write
     * @param printWriter where to write the scene to
     */
    private void writeImpl(AScene scene, PrintWriter printWriter) {
        // Write out all imports
        ImportDefCollector importDefCollector;
        try {
            importDefCollector = new ImportDefCollector(scene, printWriter);
        } catch (DefException e) {
            throw new BugInCF(e.getMessage(), e);
        }
        importDefCollector.visit();
        printWriter.println();

        // For each class
        for (Map.Entry<String, AClass> classEntry : scene.classes.entrySet()) {
            @SuppressWarnings("signature") // TODO: annotate AScene library
            @BinaryName String classname = classEntry.getKey();
            AClass aClass = classEntry.getValue();
            String pkg = packagePart(classname);
            String basename = basenamePart(classname);

            // Do not attempt to print stubs for anonymous inner classes, because the stub parser
            // cannot read them. (An anonymous inner class has a basename like Outer.1, so this
            // check ensures that the binary name's final segment after its last . is not only
            // composed of digits.)
            if (basename.contains(".")) {
                String innermostClassname = basename.substring(basename.lastIndexOf('.') + 1);
                if (innermostClassname.matches("\\d+")) {
                    continue;
                }
            }

            if (!"".equals(pkg)) {
                printWriter.println("package " + pkg + ";");
            }
            if ("package-info".equals(basename) || "module-info".equals(basename)) {
                continue;
            }

            // At this point, we no longer care about the distinction between packages
            // and inner classes, so we should replace the $ in the definition of any
            // inner classes with a ., so that they are printed correctly in stub files.
            String fullyQualifiedClassname = convertBinaryToFullyQualified(classname);

            int curlyCount =
                    1
                            + printClassDefinitions(
                                    basename, fullyQualifiedClassname, aClass, printWriter);

            // print fields or enum constants
            if (!enumConstants.containsKey(fullyQualifiedClassname)) {
                printFields(aClass, fullyQualifiedClassname, printWriter);
            } else {
                // for enums, instead of printing fields print the enum constants
                List<VariableElement> enumConstants =
                        this.enumConstants.get(fullyQualifiedClassname);

                StringJoiner sj = new StringJoiner(", ");
                for (VariableElement enumConstant : enumConstants) {
                    sj.add(enumConstant.getSimpleName());
                }
                if (sj.length() != 0) {
                    printWriter.print(sj.toString());
                    printWriter.println(";");
                }
            }

            // print method signatures
            for (Map.Entry<String, AMethod> methodEntry : aClass.methods.entrySet()) {
                AMethod aMethod = methodEntry.getValue();
                printMethodSignature(aMethod, basename, fullyQualifiedClassname, printWriter);
            }
            for (int i = 0; i < curlyCount; i++) {
                printWriter.println("}");
            }
        }
        printWriter.flush();
    }

    /**
     * Prints the type parameters of the given class, enclosed in {@code <...>}.
     *
     * @param classname a fully-qualified class name
     * @param printWriter where to print the type parameters
     */
    private void printTypeParameters(String classname, PrintWriter printWriter) {
        TypeElement type = types.get(classname);
        if (type == null) {
            return;
        }
        List<? extends TypeParameterElement> typeParameters = type.getTypeParameters();
        if (typeParameters == null || typeParameters.isEmpty()) {
            return;
        }
        printWriter.print("<");
        StringJoiner sj = new StringJoiner(", ");
        for (TypeParameterElement t : typeParameters) {
            sj.add(t.getSimpleName().toString());
        }
        printWriter.print(sj.toString());
        printWriter.print(">");
    }
}
