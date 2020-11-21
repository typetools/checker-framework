package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import org.checkerframework.javacutil.BugInCF;

/** A utility class for working with javac and JavaParser representations of a source file. */
public class AjavaUtils {
    /**
     * Returns the path to the source file containing {@code element}
     *
     * @param element the type element to look at
     * @return path to the source file containing {@code element}
     */
    public static String getSourceFilePath(TypeElement element) {
        return ((ClassSymbol) element).sourcefile.toUri().getPath();
    }

    /**
     * Given the compilation unit node for a source file, returns the top level type definition with
     * the given name.
     *
     * @param root compilation unit to search
     * @param name name of a top level type declaration in {@code root}
     * @return a top level type declaration in {@code root} named {@code name}
     */
    public static TypeDeclaration<?> getTypeDeclarationByName(CompilationUnit root, String name) {
        Optional<ClassOrInterfaceDeclaration> classDecl = root.getClassByName(name);
        if (classDecl.isPresent()) {
            return classDecl.get();
        }

        Optional<ClassOrInterfaceDeclaration> interfaceDecl = root.getInterfaceByName(name);
        if (interfaceDecl.isPresent()) {
            return interfaceDecl.get();
        }

        Optional<EnumDeclaration> enumDecl = root.getEnumByName(name);
        if (enumDecl.isPresent()) {
            return enumDecl.get();
        }

        throw new BugInCF("Requesting declaration for type that doesn't exist: " + name);
    }
}
