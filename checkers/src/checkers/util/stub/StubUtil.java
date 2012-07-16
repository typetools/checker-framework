package checkers.util.stub;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.IndexUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.*;
import japa.parser.ast.type.*;
import japa.parser.ast.visitor.SimpleVoidVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import checkers.source.SourceChecker;

/**
 * Utility class for skeleton files
 */
public class StubUtil {

    /*package-scope*/ static TypeDeclaration findDeclaration(String className, IndexUnit indexFile) {
        int indexOfDot = className.lastIndexOf('.');

        if (indexOfDot == -1) {
            // classes not within a package needs to be the first in the index file
            assert !indexFile.getCompilationUnits().isEmpty();
            assert indexFile.getCompilationUnits().get(0).getPackage() == null;
            return findDeclaration(className, indexFile.getCompilationUnits().get(0));
        }

        final String packageName = className.substring(0, indexOfDot);
        final String simpleName = className.substring(indexOfDot + 1);

        for (CompilationUnit cu : indexFile.getCompilationUnits()) {
            if (cu.getPackage() != null && cu.getPackage().getName().getName().equals(packageName)) {
                TypeDeclaration type = findDeclaration(simpleName, cu);
                if (type != null)
                    return type;
            }
        }

        // Couldn't find it
        return null;
    }

    /*package-scope*/ static TypeDeclaration findDeclaration(TypeElement type, IndexUnit indexFile) {
        return findDeclaration(type.getQualifiedName().toString(), indexFile);
    }

    /*package-scope*/ static FieldDeclaration findDeclaration(VariableElement field, IndexUnit indexFile) {
        TypeDeclaration type = findDeclaration((TypeElement)field.getEnclosingElement(), indexFile);
        if (type == null)
            return null;

        for (BodyDeclaration member : type.getMembers()) {
            if (!(member instanceof FieldDeclaration))
                continue;
            FieldDeclaration decl = (FieldDeclaration)member;
            for (VariableDeclarator var : decl.getVariables())
                if (toString(var).equals(field.getSimpleName().toString()))
                    return decl;
        }
        return null;
    }

    /*package-scope*/ static BodyDeclaration findDeclaration(ExecutableElement method, IndexUnit indexFile) {
        TypeDeclaration type = findDeclaration((TypeElement)method.getEnclosingElement(), indexFile);
        if (type == null)
            return null;

        String methodRep = toString(method);

        for (BodyDeclaration member : type.getMembers()) {
            if (member instanceof MethodDeclaration) {
                if (toString((MethodDeclaration)member).equals(methodRep))
                    return member;
            } else if (member instanceof ConstructorDeclaration) {
                if (toString((ConstructorDeclaration)member).equals(methodRep))
                    return member;
            }
        }
        return null;
    }

    /*package-scope*/ static TypeDeclaration findDeclaration(String simpleName, CompilationUnit cu) {
        for (TypeDeclaration type : cu.getTypes()) {
            if (simpleName.equals(type.getName()))
                return type;
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
        return field.getId().getName();
    }

    /*package-scope*/ static String toString(FieldDeclaration field) {
        assert field.getVariables().size() == 1;
        return toString(field.getVariables().get(0));
    }

    /**
     * Returns the chosen canonical string of the method declaration.
     *
     * The canonical representation contains simple names of the types only.
     */
    /*package-scope*/ static String toString(ExecutableElement element) {
        StringBuilder sb = new StringBuilder();

        // note: constructor simple name is <init>
        sb.append(element.getSimpleName());
        sb.append("(");
        for (Iterator<? extends VariableElement> i = element.getParameters().iterator(); i.hasNext();) {
            sb.append(standarizeType(i.next().asType()));
            if (i.hasNext())
                sb.append(",");
        }
        sb.append(")");

        return sb.toString();
    }

    /*package-scope*/ static String toString(VariableElement element) {
        assert element.getKind().isField();
        return element.getSimpleName().toString();
    }

    /*package-scope*/ static String toString(Element element) {
        if (element instanceof ExecutableElement)
            return toString((ExecutableElement)element);
        else if (element instanceof VariableElement)
            return toString((VariableElement)element);
        else
            return null;
    }
    /**
     * A helper method that standarize type by printing simple names
     * instead of fully qualified names.
     *
     * This eliminates the need for imports.
     */
    private static String standarizeType(TypeMirror type) {
        switch (type.getKind()) {
        case ARRAY:
            return standarizeType(((ArrayType)type).getComponentType()) + "[]";
        case TYPEVAR:
            return ((TypeVariable)type).asElement().getSimpleName().toString();
        case DECLARED: {
            return ((DeclaredType)type).asElement().getSimpleName().toString();
        }
        default:
            if (type.getKind().isPrimitive())
                return type.toString();
        }
        SourceChecker.errorAbort("StubUtil: unhandled type: " + type);
        return null; // dead code
    }

    private final static class ElementPrinter extends SimpleVoidVisitor<Void> {
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
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
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
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                    Parameter p = i.next();
                    p.accept(this, arg);
                    if (p.isVarArgs())
                        sb.append("[]");
                    if (i.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
        }

        @Override
        public void visit(Parameter n, Void arg) {
            if (n.getId().getArrayCount() > 0) {
                SourceChecker.errorAbort("StubUtil: put array brackets on the type, not the variable: " + n);
            }
            n.getType().accept(this, arg);
        }

        // Types
        @Override
        public void visit(ClassOrInterfaceType n, Void arg) {
            sb.append(n.getName());
        }

        @Override
        public void visit(PrimitiveType n, Void arg) {
            switch (n.getType()) {
            case Boolean:
                sb.append("boolean");
                break;
            case Byte:
                sb.append("byte");
                break;
            case Char:
                sb.append("char");
                break;
            case Double:
                sb.append("double");
                break;
            case Float:
                sb.append("float");
                break;
            case Int:
                sb.append("int");
                break;
            case Long:
                sb.append("long");
                break;
            case Short:
                sb.append("short");
                break;
            default:
                SourceChecker.errorAbort("StubUtil: unknown type: " + n.getType());
            }
        }

        @Override
        public void visit(ReferenceType n, Void arg) {
            n.getType().accept(this, arg);
            for (int i = 0; i < n.getArrayCount(); ++i)
                sb.append("[]");
        }

        @Override
        public void visit(VoidType n, Void arg) {
            sb.append("void");
        }

        @Override
        public void visit(WildcardType n, Void arg) {
            // We don't write type arguments
            // TODO: Why?
            SourceChecker.errorAbort("StubUtil: don't print type args!");
        }
    }

    public static List<File> allStubFiles(String stub) {
        List<File> files = new ArrayList<File>();
        File stubFile = new File(stub);
        allStubFiles(stubFile, files);
        return files;
    }

    private static boolean isStub(File f) {
        return f.isFile() && f.getName().endsWith(".astub");
    }

    private static void allStubFiles(File stub, List<File> files) {
        if (isStub(stub))
            files.add(stub);
        else if (stub.isDirectory()) {
            for (File enclosed : stub.listFiles()) {
                if (isStub(enclosed))
                    files.add(enclosed);
                else if (enclosed.isDirectory())
                    allStubFiles(enclosed, files);
            }
        }
    }
}
