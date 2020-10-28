package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import org.checkerframework.javacutil.BugInCF;

public class AjavaUtils {
    public static String getSourceFilePath(TypeElement element) {
        return ((ClassSymbol) element).sourcefile.toUri().getPath();
    }

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
