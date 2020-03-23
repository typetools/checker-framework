package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.common.wholeprograminference.scenelib.AClassWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.AFieldWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.AMethodWrapper;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
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

    /** A pattern matching one or more digits. */
    private static final Pattern digitPattern = Pattern.compile(".*\\$\\d+(\\$.*|$)");

    /** How far to indent when writing members of a stub file. */
    private static final String INDENT = "    ";

    /**
     * Writes the annotations in {@code scene} to {@code out} in stub file format.
     *
     * @param scene the scene to write out
     * @param out the Writer to output to
     */
    public static void write(ASceneWrapper scene, Writer out) {
        writeImpl(scene, new PrintWriter(out));
    }

    /**
     * Writes the annotations in {@code scene} to the file {@code filename} in stub file format.
     *
     * @param scene the scene to write out
     * @param filename the path of the file to write
     * @throws IOException if there is trouble writing the file
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
    // Substrings of binary names are also binary names; the empty string
    // is a dot-separated identifier (the default package).
    @SuppressWarnings("signature:return.type.incompatible")
    private static @DotSeparatedIdentifiers String packagePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? "" : className.substring(0, lastdot);
    }

    /**
     * Returns the part of a binary name that specifies the basename of the class.
     *
     * @param className a binary name
     * @return the part of the name representing the class's name without its package
     */
    // Substrings of binary names are also binary names.
    @SuppressWarnings("signature:return.type.incompatible")
    private static @BinaryName String basenamePart(@BinaryName String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? className : className.substring(lastdot + 1);
    }

    /**
     * Formats a literal argument of an annotation. Copied from {@code IndexFileWriter#printValue}
     * in the Annotation File Utilities (which the jaif printer uses), but modified to not print
     * directly and instead return the result to be printed.
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
     * Formats the component types of an array via recursive descent through the array's scene-lib
     * structure.
     *
     * @param e the array's scenelib type element
     * @param arrayType the string representation of the array's type
     * @return the type formatted to be written to Java source code
     */
    private static String formatArrayType(ATypeElement e, String arrayType) {
        StringBuilder result = new StringBuilder();
        return formatArrayTypeImpl(e, arrayType, result);
    }

    /**
     * The implementation of formatArrayType. Java array types have a somewhat unintuitive syntax:
     * see <a
     * href="https://checkerframework.org/jsr308/specification/java-annotation-design.html#array-syntax">this
     * explanation</a>. Basically, given the type {@code @Foo int @Bar [] @Baz []}, the iteration
     * order in the scene-lib representation is {@code @Bar []}, then {@code @Baz []}, and then
     * finally {@code @Foo int}. This implementation therefore passes a string builder with the
     * result of the array types seen so far, to handle multidimensional arrays. That builder is
     * appended to the final component type in the base case.
     *
     * @param e same as above, but can become null if scene-lib did not fill in the inner types,
     *     which happens when they do not have annotations
     * @param arrayType same as above
     * @param result the string builder containing the array types seen so far
     * @return the formatted string, as above, with a trailing space
     */
    private static String formatArrayTypeImpl(
            @Nullable ATypeElement e, String arrayType, StringBuilder result) {
        String nextComponentType =
                arrayType.indexOf('[') == -1
                        ? null
                        : arrayType.substring(0, arrayType.lastIndexOf('['));
        // base case when the component is a non-array type
        if (nextComponentType == null) {
            String componentAsString = arrayType + " " + result.toString();
            if (e != null) {
                return formatAnnotations(e.tlAnnotationsHere) + componentAsString;
            } else {
                return componentAsString;
            }
        } else {
            if (e != null) {
                result.append(formatAnnotations(e.tlAnnotationsHere));
            }
            result.append("[] ");
        }
        // find the next array type, if scene-lib is tracking information about it
        ATypeElement innerType = null;
        if (e != null) {
            for (Map.Entry<InnerTypeLocation, ATypeElement> ite : e.innerTypes.entrySet()) {
                InnerTypeLocation loc = ite.getKey();
                ATypeElement it = ite.getValue();
                if (loc.location.contains(TypePathEntry.ARRAY)) {
                    innerType = it;
                }
            }
        }

        return formatArrayTypeImpl(innerType, nextComponentType, result);
    }

    /**
     * Formats a single formal parameter declaration.
     *
     * @param param the AFieldWrapper that represents the parameter
     * @param parameterName the name of the parameter to display in the stub file. Stub files
     *     disregard formal parameter names, so this is aesthetic in almost all cases. The exception
     *     is the receiver parameter, whose name must be "this".
     * @param basename the type name to use for the receiver parameter. Only used when the previous
     *     argument is exactly the String "this".
     * @return the formatted formal parameter, as if it were written in Java source code
     */
    private static String formatParameter(
            AFieldWrapper param, String parameterName, String basename) {
        return formatAFieldImpl(param, parameterName, basename);
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
     * <p>Usually, {@link #formatParameter(AFieldWrapper, String, String)} should be called to
     * format method parameters, and {@link #printField(AFieldWrapper, String, PrintWriter, String)}
     * should be called to print field declarations. Both use this method as their underlying
     * implementation.
     *
     * @param aField the field declaration or formal parameter declaration to format
     * @param fieldName the name to use for the declaration in the stub file. This doesn't matter
     *     for parameters, but must be correct for fields.
     * @param className the simple name of the enclosing class. This is only used for printing the
     *     type of an explicit receiver parameter (i.e., a parameter named "this").
     * @return a String suitable to print in a stub file
     */
    private static String formatAFieldImpl(
            AFieldWrapper aField, String fieldName, String className) {
        StringBuilder result = new StringBuilder();
        String basetype;
        if ("this".equals(fieldName)) {
            basetype = className;
        } else {
            basetype = aField.getType();
        }

        // anonymous static classes shouldn't be printed with the "anonymous" tag that the AScene
        // library uses
        if (basetype.startsWith("<anonymous ")) {
            basetype = basetype.substring("<anonymous ".length(), basetype.length() - 1);
        }

        // fields don't need their generic types, and sometimes they are wrong. Just don't print
        // them.
        while (basetype.contains("<")) {
            basetype = basetype.substring(0, basetype.indexOf('<'));
        }

        if (basetype.contains("[")) {
            String formattedArrayType = formatArrayType(aField.getTheField().type, basetype);
            result.append(formattedArrayType); // formatArrayType adds a trailing space
        } else {
            result.append(formatAnnotations(aField.getTheField().type.tlAnnotationsHere));
            result.append(basetype + " "); // must add trailing space directly
        }
        result.append(fieldName);
        return result.toString();
    }

    /** Writes an import statement for each annotation used in an {@link AScene}. */
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
     * Return true if the given annotation is an internal JDK annotations, whose name includes '+'.
     *
     * @param annotationName the name of the annotation
     * @return true iff this is an internal JDK annotation
     */
    private static boolean isInternalJDKAnnotation(String annotationName) {
        return annotationName.contains("+");
    }

    /**
     * Print the hierarchy of outer classes up to and including the given class, and return the
     * number of curly braces to close with. The classes are printed with appropriate opening curly
     * braces, in standard Java style.
     *
     * <p>When an inner class is present in an AScene, its name is something like "Outer$Inner".
     * Writing a stub file with that name would be useless to the stub parser, which expects inner
     * classes to be properly nested, as in Java source code.
     *
     * @param basename the binary name of the class with the package part stripped
     * @param aClass the AClass for {@code classname}
     * @param printWriter the writer where the class definition should be printed
     * @return the number of outer classes within which this class is nested
     */
    private static int printClassDefinitions(
            String basename, AClassWrapper aClass, PrintWriter printWriter) {

        String[] classNames = StringUtils.split(basename, '$');

        for (int i = 0; i < classNames.length; i++) {
            String nameToPrint = classNames[i];
            printWriter.print(indents(i));
            // For any outer class, print "class".  For a leaf class, print "enum" or "class".
            if (i == classNames.length - 1 && aClass.isEnum()) {
                printWriter.print("enum ");
            } else {
                printWriter.print("class ");
            }
            printWriter.print(formatAnnotations(aClass.getAnnotations()));
            printWriter.print(nameToPrint);
            printTypeParameters(aClass, printWriter);
            printWriter.println(" {");
            printWriter.println();
        }
        return classNames.length;
    }

    /**
     * Prints all the fields of a given class
     *
     * @param aClass the class whose fields should be printed
     * @param printWriter the writer on which to print the fields
     * @param indentLevel the indent string
     */
    private static void printFields(
            AClassWrapper aClass, PrintWriter printWriter, String indentLevel) {

        if (aClass.getFields().isEmpty()) {
            return;
        }

        printWriter.println(indentLevel + "// fields:");
        printWriter.println();
        for (Map.Entry<String, AFieldWrapper> fieldEntry : aClass.getFields().entrySet()) {
            String fieldName = fieldEntry.getKey();
            AFieldWrapper aField = fieldEntry.getValue();
            printField(aField, fieldName, printWriter, indentLevel);
        }
    }

    /**
     * Prints a field declaration, including a trailing semicolon and a newline.
     *
     * @param aField the field declaration
     * @param fieldName the name of the field
     * @param printWriter the writer on which to print
     * @param indentLevel the indent string
     */
    private static void printField(
            AFieldWrapper aField, String fieldName, PrintWriter printWriter, String indentLevel) {
        printWriter.print(indentLevel);
        printWriter.print(formatAFieldImpl(aField, fieldName, null));
        printWriter.println(";");
        printWriter.println();
    }

    /**
     * Prints a method declaration in stub file format (i.e., without a method body).
     *
     * @param aMethodWrapper the method to print
     * @param basename the simple name of the containing class. Used only to determine if the method
     *     being printed is the constructor of an inner class.
     * @param printWriter where to print the method signature
     * @param indentLevel the indent string
     */
    private static void printMethodDeclaration(
            AMethodWrapper aMethodWrapper,
            String basename,
            PrintWriter printWriter,
            String indentLevel) {

        AMethod aMethod = aMethodWrapper.getAMethod();

        printWriter.print(indentLevel);

        // type parameters
        printTypeParameters(aMethodWrapper.getTypeParameters(), printWriter);

        printWriter.print(formatAnnotations(aMethod.returnType.tlAnnotationsHere));
        // Needed because AMethod stores the name with the parameters, to distinguish
        // between overloaded methods.
        String methodName = aMethod.methodName.substring(0, aMethod.methodName.indexOf("("));
        // Use Java syntax for constructors.
        if ("<init>".equals(methodName)) {
            methodName = basename;
        } else {
            // This isn't a constructor, so add a return type.
            // Note that the stub file format doesn't require this to be correct,
            // so it would be acceptable to print "java.lang.Object" for every
            // method. A better type is printed if one is available to improve
            // the readability of the resulting stub file.
            printWriter.print(formatAnnotations(aMethod.returnType.tlAnnotationsHere));
            String returnType = aMethodWrapper.getReturnType();
            printWriter.print(returnType);
            printWriter.print(" ");
        }
        printWriter.print(methodName);
        printWriter.print("(");

        StringJoiner parameters = new StringJoiner(", ");

        if (!aMethod.receiver.type.tlAnnotationsHere.isEmpty()) {
            // Only output the receiver if it has an annotation.
            parameters.add(
                    formatParameter(
                            AFieldWrapper.createReceiverParameter(aMethod.receiver, basename),
                            "this",
                            basename));
        }
        for (Integer index : aMethodWrapper.getParameters().keySet()) {
            AFieldWrapper param = aMethodWrapper.getParameters().get(index);
            parameters.add(formatParameter(param, param.getParameterName(), basename));
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

        // sort by package name so that output is deterministic and default package
        // comes first
        List<@BinaryName String> classes = new ArrayList<>(scene.getClasses().keySet());
        Collections.sort(classes, Comparator.comparing(SceneToStubWriter::packagePart));

        // For each class
        for (@BinaryName String clazz : classes) {
            printClass(clazz, scene.getClasses().get(clazz), printWriter);
        }
        printWriter.flush();
    }

    /**
     * Print the class body, or nothing if this is an anonymous inner class
     *
     * @param classname the class name
     * @param aClassWrapper the representation of the class
     * @param printWriter the writer on which to print
     */
    private static void printClass(
            @BinaryName String classname, AClassWrapper aClassWrapper, PrintWriter printWriter) {

        String basename = basenamePart(classname);

        if ("package-info".equals(basename) || "module-info".equals(basename)) {
            return;
        }

        // Do not attempt to print stubs for anonymous inner classes or their inner classes, because
        // the stub parser
        // cannot read them. (An anonymous inner class has a basename like Outer$1, so this
        // check ensures that no single class name is exclusively composed of digits.)
        if (digitPattern.matcher(basename).matches()) {
            return;
        }

        String innermostClassname =
                basename.contains("$")
                        ? basename.substring(basename.lastIndexOf('$') + 1)
                        : basename;

        String pkg = packagePart(classname);
        if (!"".equals(pkg)) {
            printWriter.println("package " + pkg + ";");
        }

        int curlyCount = printClassDefinitions(basename, aClassWrapper, printWriter);

        String indentLevel = indents(curlyCount);

        if (aClassWrapper.isEnum()) {
            List<VariableElement> enumConstants = aClassWrapper.getEnumConstants();
            if (enumConstants.size() != 0) {
                StringJoiner sj = new StringJoiner(", ");
                for (VariableElement enumConstant : enumConstants) {
                    sj.add(enumConstant.getSimpleName());
                }

                printWriter.println(indentLevel + "// enum constants:");
                printWriter.println();
                printWriter.println(indentLevel + sj.toString() + ";");
                printWriter.println();
            }
        }

        printFields(aClassWrapper, printWriter, indentLevel);

        if (aClassWrapper.getMethods().keySet().size() != 0) {
            // print method signatures
            printWriter.println(indentLevel + "// methods:");
            printWriter.println();
            for (Map.Entry<String, AMethodWrapper> methodEntry :
                    aClassWrapper.getMethods().entrySet()) {
                printMethodDeclaration(
                        methodEntry.getValue(), innermostClassname, printWriter, indentLevel);
            }
        }
        for (int i = curlyCount - 1; i >= 0; i--) {
            String indents = indents(i);
            printWriter.println(indents + "}");
        }
    }

    /**
     * Return a string containing n indents
     *
     * @param n the number of indents
     * @return a string containing that many indents
     */
    private static String indents(int n) {
        return Collections.nCopies(n, INDENT).stream().reduce(String::concat).orElse("");
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
        printTypeParameters(typeParameters, printWriter);
    }

    /**
     * Prints the given type parameters.
     *
     * @param typeParameters the type element to print
     * @param printWriter where to print the type parameters
     */
    private static void printTypeParameters(
            List<? extends TypeParameterElement> typeParameters, PrintWriter printWriter) {
        if (typeParameters.isEmpty()) {
            return;
        }
        StringJoiner sj = new StringJoiner(", ", "<", ">");
        for (TypeParameterElement t : typeParameters) {
            sj.add(t.getSimpleName().toString());
        }
        printWriter.print(sj.toString());
    }
}
