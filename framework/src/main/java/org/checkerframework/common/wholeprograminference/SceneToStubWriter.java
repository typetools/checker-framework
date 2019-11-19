package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
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
 * SceneToStubWriter provides two static methods named {@code write} that write a given {@link
 * AScene} to a given {@link Writer}, {@link #write(AScene, Map, Map, Set, Writer)}, or filename,
 * {@link #write(AScene, Map, Map, Set, Writer)}, in stub file format. This class is the equivalent
 * of {@code IndexFileWriter} from the Annotation File Utilities, but outputs the results in the
 * stub file format instead of jaif format.
 *
 * <p>You can use this writer instead of {@code IndexFileWriter} by passing the {@code
 * -Ainfer=stubs}.
 */
public final class SceneToStubWriter {

    /** How far to indent when writing members of a stub file. */
    private static final String INDENT = "    ";

    /** The printer where the stub file is written. */
    private final PrintWriter printWriter;

    /**
     * A map from the descriptions of ATypeElement_s to the corresponding base Java types, since
     * AScene_s don't carry that information. See the comment on the {@code basetypes} field of
     * {@link WholeProgramInferenceScenesHelper} for more information.
     */
    private Map<String, TypeMirror> basetypes;

    /**
     * A map from fully-qualified class names to the TypeElement that represents them. Computed by
     * {@link WholeProgramInferenceScenes}. Used to output the correct names of generic parameters
     * to classes, which the stub format requires.
     */
    private Map<String, TypeElement> types;

    /**
     * The fully-qualified name of enums to be written to a stub file.
     *
     * <p>The stub parser can't parse an enum that's labeled "class", but {@link AClass} doesn't
     * specify if a class is an enum. So track which classes are enums.
     */
    private Set<String> enumSet;

    /**
     * Stolen from {@code IndexFileWriter}. Prints a literal; used when printing the arguments of
     * annotations.
     */
    private void printValue(AnnotationFieldType aft, Object o) {
        if (aft instanceof AnnotationAFT) {
            printAnnotation((Annotation) o);
        } else if (aft instanceof ArrayAFT) {
            ArrayAFT aaft = (ArrayAFT) aft;
            printWriter.print('{');
            if (!(o instanceof List)) {
                printValue(aaft.elementType, o);
            } else {
                List<?> l = (List<?>) o;
                // watch out--could be an empty array of unknown type
                // (see AnnotationBuilder#addEmptyArrayField)
                if (aaft.elementType == null) {
                    if (l.size() != 0) {
                        throw new AssertionError();
                    }
                } else {
                    boolean first = true;
                    for (Object o2 : l) {
                        if (!first) {
                            printWriter.print(',');
                        }
                        printValue(aaft.elementType, o2);
                        first = false;
                    }
                }
            }
            printWriter.print('}');
        } else if (aft instanceof ClassTokenAFT) {
            printWriter.print(aft.format(o));
        } else if (aft instanceof BasicAFT && o instanceof String) {
            printWriter.print(Strings.escape((String) o));
        } else if (aft instanceof BasicAFT && o instanceof Long) {
            printWriter.print(o.toString() + "L");
        } else {
            printWriter.print(o.toString());
        }
    }

    /** Prints an annotation in Java source format. */
    private void printAnnotation(Annotation a) {
        printWriter.print("@" + a.def().name.substring(a.def().name.lastIndexOf('.') + 1));
        if (!a.fieldValues.isEmpty()) {
            printWriter.print('(');
            boolean first = true;
            for (Map.Entry<String, Object> f : a.fieldValues.entrySet()) {
                if (!first) {
                    printWriter.print(',');
                }
                printWriter.print(f.getKey() + "=");
                printValue(a.def().fieldTypes.get(f.getKey()), f.getValue());
                first = false;
            }
            printWriter.print(')');
        }
    }

    /**
     * Prints all annotations in {@code annos}, separated by spaces. See {@link #printAnnotation}.
     */
    private void printAnnotations(Collection<? extends Annotation> annos) {
        for (Annotation tla : annos) {
            if (!tla.def.name.contains("+")) {
                printWriter.print(' ');
                printAnnotation(tla);
            }
        }
    }

    /** Finds and prints the annotations on the component type of an array, if there are any. */
    private void printArrayComponentTypeAnnotation(ATypeElement e) {
        for (Map.Entry<InnerTypeLocation, ATypeElement> ite : e.innerTypes.entrySet()) {
            InnerTypeLocation loc = ite.getKey();
            AElement it = ite.getValue();
            if (loc.location.contains(TypePathEntry.ARRAY)) {
                printAnnotations(it.tlAnnotationsHere);
                printWriter.append(' ');
            }
        }
    }

    /**
     * Prints the stub representation of an AField, which represents any variable declaration. In
     * practice, this should either be a field or a method parameters, since there should be no
     * local variable declarations in a stub.
     *
     * <p>Takes two extra parameters beyond the AField: {@code fieldName} is the name to use for the
     * declaration in the stub file. This doesn't matter for parameters, but must be correct for
     * fields. {@code className} is the name of the enclosing class. This is only used for printing
     * the type of an explicit receiver parameter (i.e. a parameter named "this").
     */
    private void printAField(AField aField, String fieldName, String className) {
        String basetype;
        if ("this".equals(fieldName)) {
            basetype = className;
        } else if (basetypes.containsKey(aField.type.description.toString())) {
            basetype = basetypes.get(aField.type.description.toString()).toString();
        } else {
            basetype = "TODOTYPE";
        }

        // Annotations on arrays should be printed after the component type.
        // Don't even bother checking for annotations on component types,
        // since we can't infer them (yet?).
        if (basetype.contains("[")) {
            String component = basetype.substring(0, basetype.lastIndexOf('['));
            printArrayComponentTypeAnnotation(aField.type);
            printWriter.print(component);
            printWriter.print(" ");
            basetype = "[]";
        }
        printAnnotations(aField.type.tlAnnotationsHere);
        printWriter.print(" ");
        printWriter.print(basetype);
        printWriter.print(" ");
        printWriter.print(fieldName);
    }

    /**
     * Writes out an import statement for each annotation used in an {@link AScene}.
     *
     * <p>{@code DefCollector} is a facility in the annotation file utilities for determining which
     * annotations are used in a given AScene. Here, we use that construct to write out the proper
     * import statements into a stub file.
     */
    private class ImportDefCollector extends DefCollector {

        /**
         * Constructs a new ImportDefCollector, which will run on the given AScene when its {@code
         * visit} method is called.
         */
        ImportDefCollector(AScene scene) throws DefException {
            super(scene);
        }

        /**
         * Write an import statement for a given AnnotationDef. This is only called once per
         * annotation used in the scene.
         */
        @Override
        protected void visitAnnotationDef(AnnotationDef d) {
            if (!d.name.contains("+")) {
                printWriter.println("import " + d.name + ";");
            }
        }
    }

    /**
     * When an inner class is present in an AScene, its name is something like "Outer$Inner".
     * Writing a stub file with that name would be useless to the stub parser, which expects inner
     * classes to be properly nested.
     *
     * <p>For a given class, this prints out the hierarchy of outer classes, and returns the number
     * of curly braces to close with.
     *
     * @param basename the name of the class without the package, in Outer$Inner form
     * @param classname the fully-qualified name of the class in question, so that it can be printed
     *     as an enum if it is one
     * @param aClass the AClass representing the classname
     */
    private int printClassDefinition(String basename, String classname, AClass aClass) {

        String nameToPrint = basename;
        String rest = "";
        if (basename.contains("$")) {
            nameToPrint = basename.substring(0, basename.indexOf('$'));
            rest = basename.substring(basename.indexOf('$') + 1);
        }
        if (enumSet.contains(classname)) {
            printWriter.print("enum");
        } else {
            printWriter.print("class ");
        }
        printAnnotations(aClass.tlAnnotationsHere);
        printWriter.print(" " + nameToPrint);
        printTypeParameters(classname);
        printWriter.println(" {");
        if ("".equals(rest)) {
            return 0;
        } else {
            return 1 + printClassDefinition(rest, classname, aClass);
        }
    }

    /**
     * The implementation of {@link #write(AScene, Map, Map, Set, Writer)} and {@link #write(AScene,
     * Map, Map, Set, String)}. Prints imports, classes, method signatures, and fields; all with
     * appropriate annotations in stub file format.
     */
    private void writeImpl(AScene scene) {
        // Write out all imports
        ImportDefCollector importDefCollector;
        try {
            importDefCollector = new ImportDefCollector(scene);
        } catch (DefException e) {
            throw new BugInCF(e.getMessage(), e);
        }
        importDefCollector.visit();
        printWriter.println();

        // For each class
        for (Map.Entry<String, AClass> classEntry : scene.classes.entrySet()) {
            String classname = classEntry.getKey();
            AClass aClass = classEntry.getValue();
            String pkg = packagePart(classname);
            if (!"".equals(pkg)) {
                printWriter.println("package " + pkg + ";");
            }
            String basename = basenamePart(classname);
            int curlyCount = 1;

            if ("package-info".equals(basename)) {
                continue;
            } else {
                curlyCount += printClassDefinition(basename, classname, aClass);
            }

            // print fields, but not for enums (that causes the stub parser to reject the stub)
            if (!enumSet.contains(classname)) {
                for (Map.Entry<String, AField> fieldEntry : aClass.fields.entrySet()) {
                    String fieldName = fieldEntry.getKey();
                    AField aField = fieldEntry.getValue();
                    printWriter.println();
                    printWriter.print(INDENT);
                    printAField(aField, fieldName, classname);
                    printWriter.println(";");
                }
            }

            // print method signatures
            for (Map.Entry<String, AMethod> methodEntry : aClass.methods.entrySet()) {
                AMethod aMethod = methodEntry.getValue();
                printWriter.println();
                printWriter.print(INDENT);
                printAnnotations(aMethod.returnType.tlAnnotationsHere);
                printWriter.print(" ");
                String methodName =
                        aMethod.methodName.substring(0, aMethod.methodName.indexOf("("));
                // Use Java syntax for constructors.
                if ("<init>".equals(methodName)) {
                    methodName = basename;
                } else {
                    // only add a return type if this isn't a constructor
                    if (basetypes.containsKey(aMethod.returnType.description.toString())) {
                        printWriter.print(basetypes.get(aMethod.returnType.description.toString()));
                    } else {
                        printWriter.print("java.lang.Object");
                    }
                    printWriter.print(" ");
                }
                printWriter.print(methodName);
                printWriter.print("(");
                boolean firstParam = true;
                if (!aMethod.receiver.type.tlAnnotationsHere.isEmpty()
                        || !aMethod.receiver.type.innerTypes.isEmpty()) {
                    // Only output the receiver if there is something to
                    // say.  This is a bit inconsistent with the return
                    // type, but so be it.
                    printAField(aMethod.receiver, "this", classname);
                    firstParam = false;
                }
                for (Integer index : aMethod.parameters.keySet()) {
                    if (!firstParam) {
                        printWriter.print(", ");
                    }
                    AField param = aMethod.parameters.get(index);
                    // AMethod doesn't actually track real parameter names.
                    // Fortunately, the stub file parser also doesn't, so
                    // this code can safely ignore that problem and use a generic
                    // name instead.
                    //
                    // TODO: use the actual parse tree of the method
                    // to figure out the real parameter names, and then thread them
                    // through to here.
                    printAField(param, "param" + index, classname);
                    firstParam = false;
                }
                printWriter.println(");");
            }
            for (int i = 0; i < curlyCount; i++) {
                printWriter.println("}");
            }
        }
    }

    /**
     * Prints the type parameters of the class given by the fully-qualified name passes as {@code
     * classname}.
     */
    private void printTypeParameters(String classname) {
        TypeElement type = types.get(classname);
        if (type == null) {
            return;
        }
        List<? extends TypeParameterElement> typeParameters = type.getTypeParameters();
        if (typeParameters == null || typeParameters.isEmpty()) {
            return;
        }
        printWriter.print("<");
        boolean first = true;
        for (TypeParameterElement t : typeParameters) {
            if (!first) {
                printWriter.print(", ");
            }
            printWriter.print(t.getSimpleName().toString());
            first = false;
        }
        printWriter.print(">");
    }

    /**
     * Private constructor that initializes the printWriter and copies over relevant inputs to the
     * fields in which they are stored.
     */
    private SceneToStubWriter(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<String, TypeElement> types,
            Set<String> enumSet,
            Writer out) {
        this.basetypes = basetypes;
        this.types = types;
        this.enumSet = enumSet;
        printWriter = new PrintWriter(out);
        writeImpl(scene);
        printWriter.flush();
    }

    /** Writes the annotations in <code>scene</code> to <code>out</code> in stub file format. */
    public static void write(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<String, TypeElement> types,
            Set<String> enumSet,
            Writer out) {
        new SceneToStubWriter(scene, basetypes, types, enumSet, out);
    }

    /**
     * Writes the annotations in {@code scene}to the file {@code filename} in stub file format; see
     * {@link #write(AScene, Map, Map, Set, Writer)}.
     */
    public static void write(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<String, TypeElement> types,
            Set<String> enumSet,
            String filename)
            throws IOException {
        write(scene, basetypes, types, enumSet, new FileWriter(filename));
    }

    /** The part of a fully-qualified name that specifies the package. */
    private static String packagePart(String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? "" : className.substring(0, lastdot);
    }

    /** The part of a fully-qualified name that specifies the basename of the class. */
    private static String basenamePart(String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? className : className.substring(lastdot + 1);
    }
}
