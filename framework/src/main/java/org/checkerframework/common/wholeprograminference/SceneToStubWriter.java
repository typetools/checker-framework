package org.checkerframework.common.wholeprograminference;

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
 * SceneToStubWriter provides two static methods named <code>write</code> that write a given {@link
 * AScene} to a given {@link Writer} or filename, in stub file format. This class is the equivalent
 * of IndexFileWriter from the Annotation File Utilities, but outputs the results in the .astub
 * format.
 *
 * <p>You can use this writer instead of IndexFileWriter by passing the -AoutputStubs when running
 * with -Ainfer.
 */
public final class SceneToStubWriter {
    final AScene scene;

    private static final String INDENT = "    ";

    final PrintWriter printWriter;

    private Map<String, TypeMirror> basetypes;
    private Map<String, TypeElement> types;

    /**
     * AClass doesn't carry any information to differentiate classes from enums. The stub parser
     * can't parse an enum that's labeled "class", so we have to track that separately. The names in
     * this set are fully-qualified.
     */
    private Set<String> enumSet;

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

    private void printAnnotations(Collection<? extends Annotation> annos) {
        for (Annotation tla : annos) {
            if (!tla.def.name.contains("+")) {
                printWriter.print(' ');
                printAnnotation(tla);
            }
        }
    }

    private void printAnnotations(AElement e) {
        printAnnotations(e.tlAnnotationsHere);
    }

    private void printArrayComponentTypeAnnotation(ATypeElement e) {
        for (Map.Entry<InnerTypeLocation, ATypeElement> ite : e.innerTypes.entrySet()) {
            InnerTypeLocation loc = ite.getKey();
            AElement it = ite.getValue();
            if (loc.toString().contains("ARRAY")) {
                printAnnotations(it);
                printWriter.append(' ');
            }
        }
    }

    private void printAField(AField aField, String fieldName, String classname) {
        String basetype;
        if ("this".equals(fieldName)) {
            basetype = classname;
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

    private class ImportDefCollector extends DefCollector {
        ImportDefCollector() throws DefException {
            super(SceneToStubWriter.this.scene);
        }

        @Override
        protected void visitAnnotationDef(AnnotationDef d) {
            if (!d.name.contains("+")) {
                printWriter.println("import " + d.name + ";");
            }
        }
    }

    /** @return number of inner classes */
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
        printAnnotations(aClass);
        printWriter.print(" " + nameToPrint);
        printTypeParameters(classname);
        printWriter.println(" {");
        if ("".equals(rest)) {
            return 0;
        } else {
            return 1 + printClassDefinition(rest, classname, aClass);
        }
    }

    private void writeImpl() {
        // Write out all imports
        ImportDefCollector importDefCollector;
        try {
            importDefCollector = new ImportDefCollector();
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

    private SceneToStubWriter(
            AScene scene,
            Map<String, TypeMirror> basetypes,
            Map<String, TypeElement> types,
            Set<String> enumSet,
            Writer out) {
        this.scene = scene;
        this.basetypes = basetypes;
        this.types = types;
        this.enumSet = enumSet;
        printWriter = new PrintWriter(out);
        writeImpl();
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
     * Writes the annotations in <code>scene</code> to the file <code>filename</code> in stub file
     * format; see {@link #write(AScene, Map, Map, Set, Writer)}.
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

    private static String packagePart(String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? "" : className.substring(0, lastdot);
    }

    private static String basenamePart(String className) {
        int lastdot = className.lastIndexOf('.');
        return (lastdot == -1) ? className : className.substring(lastdot + 1);
    }
}
