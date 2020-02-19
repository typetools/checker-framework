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
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.wholeprograminference.scenelib.AClassWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.AFieldWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.AMethodWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AElement;
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
 * stub file format, to a {@link Writer} {@link #write(ASceneWrapper, Writer)}, or to a file {@link
 * #write(ASceneWrapper, Writer)}. This class is the equivalent of {@code IndexFileWriter} from the
 * Annotation File Utilities, but outputs the results in the stub file format instead of jaif
 * format.
 *
 * <p>You can use this writer instead of {@code IndexFileWriter} by passing the {@code
 * -Ainfer=stubs} command-line argument.
 */
public final class SceneToStubWriter {

    /** A pattern matching one or more digits */
    private static final Pattern digitPattern = Pattern.compile("\\d+");

    /** How far to indent when writing members of a stub file. */
    private static final String INDENT = "    ";

    /**
     * Writes the annotations in {@code scene} to {@code out} in stub file format.
     *
     * @param scene the scene to write out
     * @param out the Writer to output the result to
     */
    public static void write(ASceneWrapper scene, Writer out) {
        writeImpl(scene, new PrintWriter(out));
    }

    /**
     * Writes the annotations in {@code scene} to the file {@code filename} in stub file format.
     *
     * @param scene the scene to write out {@code TypeMirror}s that represent their base Java types
     * @param filename the path of the file to write to
     * @throws IOException if the file doesn't exist
     * @see #write(ASceneWrapper, Writer)
     */
    public static void write(ASceneWrapper scene, String filename) throws IOException {
        write(scene, new FileWriter(filename));
    }

    /**
     * Returns the part of a binary name that specifies the package.
     *
     * @param className the binary name of a class
     * @return the part of the name referring to the package
     */
    private static String packagePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? "" : className.substring(0, lastdot);
    }

    /**
     * Returns the part of a binary name that specifies the basename of the class.
     *
     * @param className a binary name
     * @return the part of the name representing the class's name without its package
     */
    private static String basenamePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        String result = (lastdot == -1) ? className : className.substring(lastdot + 1);
        return result;
    }

    /**
     * Formats a literal argument of an annotation. Similar to {@code IndexFileWriter#printValue} in
     * the AnnotationFileUtilities (which the jaif printer uses), but does not print directly.
     * Instead, returns the result to be printed.
     *
     * @param aft the annotation whose values are being formatted, for context
     * @param o the value or values to format
     * @return the String representation of the value
     */
    private static String formatAnnotationValue(AnnotationFieldType aft, Object o) {
        if (aft instanceof AnnotationAFT) {
            return formatAnnotation((Annotation) o);
        } else if (aft instanceof ArrayAFT) {
            StringJoiner sj = new StringJoiner(",", "{", "}");
            ArrayAFT aaft = (ArrayAFT) aft;
            if (!(o instanceof List)) {
                sj.add(formatAnnotationValue(aaft.elementType, o));
            } else {
                List<?> l = (List<?>) o;
                // watch out--could be an empty array of unknown type
                // (see AnnotationBuilder#addEmptyArrayField)
                if (aaft.elementType == null) {
                    if (l.size() != 0) {
                        throw new AssertionError();
                    }
                } else {

                    for (Object o2 : l) {
                        sj.add(formatAnnotationValue(aaft.elementType, o2));
                    }
                }
            }
            return sj.toString();
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
    private static String formatAnnotation(Annotation a) {
        String annoName = a.def().name.substring(a.def().name.lastIndexOf('.') + 1);
        if (a.fieldValues.isEmpty()) {
            return "@" + annoName;
        }
        StringJoiner sj = new StringJoiner(",", "@" + annoName + "(", ")");
        for (Map.Entry<String, Object> f : a.fieldValues.entrySet()) {
            AnnotationFieldType aft = a.def().fieldTypes.get(f.getKey());
            sj.add(f.getKey() + "=" + formatAnnotationValue(aft, f.getValue()));
        }
        return sj.toString();
    }

    /**
     * Returns all annotations in {@code annos} in a form suitable to be printed as Java source
     * code.
     *
     * <p>Each annotation is followed by a space, to separate it from following Java code.
     *
     * @param annos the annotations to format
     * @return all annotations in {@code annos}, separated by spaces, in a form suitable to be
     *     printed as Java source code
     */
    private static String formatAnnotations(Collection<? extends Annotation> annos) {
        StringBuilder sb = new StringBuilder();
        for (Annotation tla : annos) {
            if (!isInternalJDKAnnotation(tla.def.name)) {
                sb.append(formatAnnotation(tla));
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Formats the annotations on the component type of an array, if there are any.
     *
     * @param e the array type
     * @return the component type formatted to be written to Java source code
     */
    private static String formatArrayComponentTypeAnnotation(ATypeElement e) {
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
     * <p>It is also the responsibility of the caller to place the output of this method in context;
     * because it is shared between field declarations and formal parameters, it does not add any
     * trailing semicolons/commas/other syntax.
     *
     * @param aField the field to format
     * @param fieldName the name to use for the declaration in the stub file. This doesn't matter
     *     for parameters, but must be correct for fields.
     * @param className the simple name of the enclosing class. This is only used for printing the
     *     type of an explicit receiver parameter (i.e., a parameter named "this").
     * @return a String suitable to print in a stub file
     */
    private static String formatAField(AFieldWrapper aField, String fieldName, String className) {
        StringBuilder result = new StringBuilder();
        String basetype;
        if ("this".equals(fieldName)) {
            basetype = className;
        } else {
            basetype = aField.getType();
        }

        if (basetype.contains("[")) {
            String component = basetype.substring(0, basetype.lastIndexOf('['));
            result.append(formatArrayComponentTypeAnnotation(aField.getTheField().type));
            result.append(component);
            result.append(" ");
            basetype = "[]";
        }
        result.append(formatAnnotations(aField.getTheField().type.tlAnnotationsHere));
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
    private static class ImportDefWriter extends DefCollector {

        /** The writer onto which to write the import statements. */
        private final PrintWriter printWriter;

        /**
         * Constructs a new ImportDefWriter, which will run on the given AScene when its {@code
         * visit} method is called.
         *
         * @param scene the scene whose imported annotations should be printed
         * @param printWriter the writer onto which to write the import statements
         * @throws DefException if the DefCollector does not succeed
         */
        ImportDefWriter(ASceneWrapper scene, PrintWriter printWriter) throws DefException {
            super(scene.getAScene());
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
            if (!isInternalJDKAnnotation(d.name)) {
                printWriter.println("import " + d.name + ";");
            }
        }
    }

    /**
     * Do not print internal JDK annotations, which are the only annotations that include a '+'
     *
     * @param annotationName the name of the annotation
     * @return true iff this is an internal JDK annotation that should not be printed
     */
    private static boolean isInternalJDKAnnotation(String annotationName) {
        return annotationName.contains("+");
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
     * @param basename the binary name of the class with the package part stripped
     * @param aClass the AClass for {@code classname}
     * @param printWriter the writer where the class definition should be printed
     * @return the number of outer classes within which this class is nested
     */
    private static int printClassDefinitions(
            String basename, AClassWrapper aClass, PrintWriter printWriter) {

        String nameToPrint = basename;
        String remainingInnerClassNames = "";
        if (basename.contains("$")) {
            nameToPrint = basename.substring(0, basename.indexOf('$'));
            remainingInnerClassNames = basename.substring(basename.indexOf('$') + 1);
        }

        // For any outer class, print "class".  For a leaf class, print "enum" or "class".
        if ("".equals(remainingInnerClassNames) && aClass.isEnum()) {
            printWriter.print("enum ");
        } else {
            printWriter.print("class ");
        }
        formatAnnotations(aClass.getAnnotations());
        printWriter.print(nameToPrint);
        printTypeParameters(aClass, printWriter);
        printWriter.println(" {");
        printWriter.println();
        if ("".equals(remainingInnerClassNames)) {
            return 0;
        } else {
            return 1 + printClassDefinitions(remainingInnerClassNames, aClass, printWriter);
        }
    }

    /**
     * Prints all the fields of a given class
     *
     * @param aClass the class whose fields should be printed
     * @param classname the simple name of the class, used to print the type of "this"
     * @param printWriter the writer on which to print the fields
     */
    private static void printFields(
            AClassWrapper aClass, String classname, PrintWriter printWriter) {
        for (Map.Entry<String, AFieldWrapper> fieldEntry : aClass.getFields().entrySet()) {
            String fieldName = fieldEntry.getKey();
            AFieldWrapper aField = fieldEntry.getValue();
            printWriter.print(INDENT);
            printWriter.print(formatAField(aField, fieldName, classname));
            printWriter.println(";");
            printWriter.println();
        }
    }

    /**
     * Prints a method signature in stub file format (i.e., without a method body).
     *
     * @param aMethodWrapper the method to print
     * @param basename the simple name of the containing class. Used only to determine if the method
     *     being printed is the constructor of an inner class.
     * @param printWriter where to print the method signature
     */
    private static void printMethodSignature(
            AMethodWrapper aMethodWrapper, String basename, PrintWriter printWriter) {

        AMethod aMethod = aMethodWrapper.getAMethod();

        printWriter.print(INDENT);
        printWriter.print(formatAnnotations(aMethod.returnType.tlAnnotationsHere));
        // Needed because AMethod stores the name with the parameters, to differentiate
        // between different methods in the same class with the same name.
        String methodName = aMethod.methodName.substring(0, aMethod.methodName.indexOf("("));
        // Use Java syntax for constructors.
        if ("<init>".equals(methodName)) {
            // Constructor names cannot contain dots, if this is an inner class.
            methodName =
                    basename.contains(".")
                            ? basename.substring(basename.lastIndexOf('.') + 1)
                            : basename;
        } else {
            // This isn't a constructor, so add a return type.
            // Note that the stub file format doesn't require this to be correct,
            // so it would be acceptable to print "java.lang.Object" for every
            // method. A better type is printed if one is available to improve
            // the readability of the resulting stub file.
            String returnType = aMethodWrapper.getReturnType();
            printWriter.print(returnType);
            printWriter.print(" ");
        }
        printWriter.print(methodName);
        printWriter.print("(");

        StringJoiner parameters = new StringJoiner(", ");

        if (!aMethod.receiver.type.tlAnnotationsHere.isEmpty()
                || !aMethod.receiver.type.innerTypes.isEmpty()) {
            // Only output the receiver if it has an annotation.
            parameters.add(
                    formatAField(new AFieldWrapper(aMethod.receiver, basename), "this", basename));
        }
        for (Integer index : aMethodWrapper.getParameters().keySet()) {
            AFieldWrapper param = aMethodWrapper.getParameters().get(index);
            parameters.add(formatAField(param, param.getParameterName(), basename));
        }
        printWriter.print(parameters.toString());
        printWriter.println(");");
        printWriter.println();
    }

    /**
     * The implementation of {@link #write(ASceneWrapper, Writer)} and {@link #write(ASceneWrapper,
     * String)}. Prints imports, classes, method signatures, and fields in stub file format, all
     * with appropriate annotations.
     *
     * @param scene the scene to write
     * @param printWriter where to write the scene to
     */
    private static void writeImpl(ASceneWrapper scene, PrintWriter printWriter) {
        // Write out all imports
        ImportDefWriter importDefWriter;
        try {
            importDefWriter = new ImportDefWriter(scene, printWriter);
        } catch (DefException e) {
            throw new BugInCF(e);
        }
        importDefWriter.visit();
        printWriter.println();

        // For each class
        class_loop:
        for (Map.Entry<@BinaryName String, AClassWrapper> classEntry :
                scene.getClasses().entrySet()) {
            printClass(classEntry, printWriter);
        }
        printWriter.flush();
    }

    /**
     * Print the class body, or nothing if this is an anonymous inner class
     *
     * @param classEntry the class to print, as a Map entry. The key is the class name in binary
     *     form. The value is the AClassWrapper object representing the class.
     * @param printWriter the writer on which to print
     */
    private static void printClass(
            Map.Entry<@BinaryName String, AClassWrapper> classEntry, PrintWriter printWriter) {

        String classname = classEntry.getKey();
        String basename = basenamePart(classname);

        // Do not attempt to print stubs for anonymous inner classes, because the stub parser
        // cannot read them. (An anonymous inner class has a basename like Outer.1, so this
        // check ensures that the binary name's final segment after its last . is not only
        // composed of digits.)
        String innermostClassname = basename;
        while (innermostClassname.contains("$")) {
            innermostClassname =
                    innermostClassname.substring(innermostClassname.lastIndexOf('$') + 1);
            if (digitPattern.matcher(innermostClassname).matches()) {
                return;
            }
        }

        if ("package-info".equals(basename) || "module-info".equals(basename)) {
            return;
        }

        String pkg = packagePart(classname);
        if (!"".equals(pkg)) {
            printWriter.println("package " + pkg + ";");
        }

        AClassWrapper aClassWrapper = classEntry.getValue();

        int curlyCount = 1 + printClassDefinitions(basename, aClassWrapper, printWriter);

        if (aClassWrapper.isEnum()) {
            List<VariableElement> enumConstants = aClassWrapper.getEnumConstants();

            StringJoiner sj = new StringJoiner(", ");
            for (VariableElement enumConstant : enumConstants) {
                sj.add(enumConstant.getSimpleName());
            }
            if (sj.length() != 0) {
                printWriter.println(INDENT + "// enum constants:");
                printWriter.println();
                printWriter.print(INDENT + sj.toString());
                printWriter.println(";");
                printWriter.println();
            }
        }

        printWriter.println(INDENT + "// fields:");
        printWriter.println();
        printFields(aClassWrapper, innermostClassname, printWriter);

        // print method signatures
        printWriter.println(INDENT + "// methods:");
        printWriter.println();
        for (Map.Entry<String, AMethodWrapper> methodEntry :
                aClassWrapper.getMethods().entrySet()) {
            printMethodSignature(methodEntry.getValue(), innermostClassname, printWriter);
        }
        for (int i = 0; i < curlyCount; i++) {
            printWriter.println("}");
        }
    }

    /**
     * Prints the type parameters of the given class, enclosed in {@code <...>}.
     *
     * @param aClass the class whose type parameters should be printed
     * @param printWriter where to print the type parameters
     */
    private static void printTypeParameters(AClassWrapper aClass, PrintWriter printWriter) {
        TypeElement type = aClass.getTypeElement();
        if (type == null) {
            return;
        }
        List<? extends TypeParameterElement> typeParameters = type.getTypeParameters();
        if (typeParameters.isEmpty()) {
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
