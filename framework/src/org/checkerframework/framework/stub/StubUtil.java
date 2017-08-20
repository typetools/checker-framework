package org.checkerframework.framework.stub;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.SimpleVoidVisitor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

/** Utility class for stub files */
public class StubUtil {

    /*package-scope*/ static TypeDeclaration<?> findDeclaration(
            String className, StubUnit indexFile) {
        int indexOfDot = className.lastIndexOf('.');

        if (indexOfDot == -1) {
            // classes not within a package needs to be the first in the index file
            assert !indexFile.getCompilationUnits().isEmpty();
            assert indexFile.getCompilationUnits().get(0).getPackageDeclaration() == null;
            return findDeclaration(className, indexFile.getCompilationUnits().get(0));
        }

        final String packageName = className.substring(0, indexOfDot);
        final String simpleName = className.substring(indexOfDot + 1);

        for (CompilationUnit cu : indexFile.getCompilationUnits()) {
            if (cu.getPackageDeclaration().isPresent()
                    && cu.getPackageDeclaration().get().getNameAsString().equals(packageName)) {
                TypeDeclaration<?> type = findDeclaration(simpleName, cu);
                if (type != null) {
                    return type;
                }
            }
        }

        // Couldn't find it
        return null;
    }

    /*package-scope*/ static TypeDeclaration<?> findDeclaration(
            TypeElement type, StubUnit indexFile) {
        return findDeclaration(type.getQualifiedName().toString(), indexFile);
    }

    /*package-scope*/ static FieldDeclaration findDeclaration(
            VariableElement field, StubUnit indexFile) {
        TypeDeclaration<?> type =
                findDeclaration((TypeElement) field.getEnclosingElement(), indexFile);
        if (type == null) {
            return null;
        }

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (!(member instanceof FieldDeclaration)) {
                continue;
            }
            FieldDeclaration decl = (FieldDeclaration) member;
            for (VariableDeclarator var : decl.getVariables()) {
                if (toString(var).equals(field.getSimpleName().toString())) {
                    return decl;
                }
            }
        }
        return null;
    }

    /*package-scope*/ static BodyDeclaration<?> findDeclaration(
            ExecutableElement method, StubUnit indexFile) {
        TypeDeclaration<?> type =
                findDeclaration((TypeElement) method.getEnclosingElement(), indexFile);
        if (type == null) {
            return null;
        }

        String methodRep = toString(method);

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof MethodDeclaration) {
                if (toString((MethodDeclaration) member).equals(methodRep)) {
                    return member;
                }
            } else if (member instanceof ConstructorDeclaration) {
                if (toString((ConstructorDeclaration) member).equals(methodRep)) {
                    return member;
                }
            }
        }
        return null;
    }

    /*package-scope*/ static TypeDeclaration<?> findDeclaration(
            String simpleName, CompilationUnit cu) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (simpleName.equals(type.getNameAsString())) {
                return type;
            }
        }
        // Couldn't find it
        return null;
    }

    /*package-scope*/ static String toString(MethodDeclaration method) {
        return ElementPrinter.toString(method);
    }

    /*package-scope*/ static String toString(ConstructorDeclaration constructor) {
        return ElementPrinter.toString(constructor);
    }

    /*package-scope*/ static String toString(VariableDeclarator field) {
        return field.getNameAsString();
    }

    /*package-scope*/ static String toString(FieldDeclaration field) {
        assert field.getVariables().size() == 1;
        return toString(field.getVariables().get(0));
    }

    /**
     * Returns the chosen canonical string of the method declaration.
     *
     * <p>The canonical representation contains simple names of the types only.
     */
    /*package-scope*/ static String toString(ExecutableElement element) {
        StringBuilder sb = new StringBuilder();

        // note: constructor simple name is <init>
        sb.append(element.getSimpleName());
        sb.append("(");
        for (Iterator<? extends VariableElement> i = element.getParameters().iterator();
                i.hasNext();
                ) {
            sb.append(standarizeType(i.next().asType()));
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        sb.append(")");

        return sb.toString();
    }

    /*package-scope*/ static String toString(VariableElement element) {
        assert element.getKind().isField();
        return element.getSimpleName().toString();
    }

    /*package-scope*/ static String toString(Element element) {
        if (element instanceof ExecutableElement) {
            return toString((ExecutableElement) element);
        } else if (element instanceof VariableElement) {
            return toString((VariableElement) element);
        } else {
            return null;
        }
    }

    /*package-scope*/ static Pair<String, String> partitionQualifiedName(String imported) {
        String typeName = imported.substring(0, imported.lastIndexOf("."));
        String name = imported.substring(imported.lastIndexOf(".") + 1);
        Pair<String, String> typeParts = Pair.of(typeName, name);
        return typeParts;
    }

    /**
     * A helper method that standarize type by printing simple names instead of fully qualified
     * names.
     *
     * <p>This eliminates the need for imports.
     */
    private static String standarizeType(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:
                return standarizeType(((ArrayType) type).getComponentType()) + "[]";
            case TYPEVAR:
                return ((TypeVariable) type).asElement().getSimpleName().toString();
            case DECLARED:
                {
                    return ((DeclaredType) type).asElement().getSimpleName().toString();
                }
            default:
                if (type.getKind().isPrimitive()) {
                    return type.toString();
                }
        }
        ErrorReporter.errorAbort("StubUtil: unhandled type: " + type);
        return null; // dead code
    }

    private static final class ElementPrinter extends SimpleVoidVisitor<Void> {
        public static String toString(Node n) {
            ElementPrinter printer = new ElementPrinter();
            n.accept(printer, null);
            return printer.getOutput();
        }

        private final StringBuilder sb = new StringBuilder();

        public String getOutput() {
            return sb.toString();
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            sb.append("<init>");

            sb.append("(");
            if (n.getParameters() != null) {
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                    Parameter p = i.next();
                    p.accept(this, arg);

                    if (i.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            sb.append(n.getName());

            sb.append("(");
            if (n.getParameters() != null) {
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                    Parameter p = i.next();
                    p.accept(this, arg);

                    if (i.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
        }

        @Override
        public void visit(Parameter n, Void arg) {
            n.getType().accept(this, arg);
            if (n.isVarArgs()) {
                sb.append("[]");
            }
        }

        // Types
        @Override
        public void visit(ClassOrInterfaceType n, Void arg) {
            sb.append(n.getName());
        }

        @Override
        public void visit(PrimitiveType n, Void arg) {
            switch (n.getType()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case CHAR:
                    sb.append("char");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case INT:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                default:
                    ErrorReporter.errorAbort("StubUtil: unknown type: " + n.getType());
            }
        }

        @Override
        public void visit(ReferenceType n, Void arg) {
            n.getElementType().accept(this, arg);
            for (int i = 0; i < n.getArrayLevel(); ++i) {
                sb.append("[]");
            }
        }

        @Override
        public void visit(com.github.javaparser.ast.type.ArrayType n, Void arg) {
            n.getComponentType().accept(this, arg);
            for (int i = 0; i < n.getArrayLevel(); ++i) {
                sb.append("[]");
            }
        }

        @Override
        public void visit(VoidType n, Void arg) {
            sb.append("void");
        }

        @Override
        public void visit(WildcardType n, Void arg) {
            // We don't write type arguments
            // TODO: Why?
            ErrorReporter.errorAbort("StubUtil: don't print type args!");
        }
    }

    public static List<StubResource> allStubFiles(String stub) {
        List<StubResource> resources = new ArrayList<StubResource>();
        File stubFile = new File(stub);
        if (stubFile.exists()) {
            allStubFiles(stubFile, resources);
        } else {
            // If the stubFile doesn't exist, maybe it is relative to the
            // current working directory, so try that.
            String workingDir =
                    System.getProperty("user.dir") + System.getProperty("file.separator");
            stubFile = new File(workingDir + stub);
            if (stubFile.exists()) {
                allStubFiles(stubFile, resources);
            }
        }
        return resources;
    }

    private static boolean isStub(File f) {
        return f.isFile() && isStub(f.getName());
    }

    private static boolean isStub(String path) {
        return path.endsWith(".astub");
    }

    private static boolean isJar(File f) {
        return f.isFile() && f.getName().endsWith(".jar");
    }

    /**
     * Side-effects {@code resources} by adding to it either {@code stub} if it is a stub file, or
     * all the contained stub files if {@code stub} is a jar file or a directory.
     */
    private static void allStubFiles(File stub, List<StubResource> resources) {
        if (isStub(stub)) {
            resources.add(new FileStubResource(stub));
        } else if (isJar(stub)) {
            JarFile file;
            try {
                file = new JarFile(stub);
            } catch (IOException e) {
                System.err.println("StubUtil: could not process JAR file: " + stub);
                return;
            }
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (isStub(entry.getName())) {
                    resources.add(new JarEntryStubResource(file, entry));
                }
            }
        } else if (stub.isDirectory()) {
            File[] directoryContents = stub.listFiles();
            Arrays.sort(
                    directoryContents,
                    new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
            for (File enclosed : directoryContents) {
                allStubFiles(enclosed, resources);
            }
        }
    }
}
