package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.Optional;
import org.checkerframework.javacutil.BugInCF;

/** Utility methods for working with JavaParser. */
public class JavaParserUtils {
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

        Optional<CompilationUnit.Storage> storage = root.getStorage();
        if (storage.isPresent()) {
            throw new BugInCF("Type " + name + " not found in " + storage.get().getPath());
        } else {
            throw new BugInCF("Type " + name + " not found in " + root);
        }
    }

    /**
     * Side-effects {@code node} by removing all annotations from anywhere inside its subtree.
     *
     * @param node a JavaParser Node
     */
    public static void clearAnnotations(Node node) {
        node.accept(new ClearAnnotationsVisitor(), null);
    }

    /** A visitor that clears all annotations from a JavaParser AST. */
    private static class ClearAnnotationsVisitor extends VoidVisitorWithDefaultAction {
        @Override
        public void defaultAction(Node node) {
            for (Node child : new ArrayList<>(node.getChildNodes())) {
                if (child instanceof AnnotationExpr) {
                    node.remove(child);
                }
            }
        }
    }

    /**
     * Side-effects node by combining any added String literals in node's subtree into their
     * concatenation. For example, the expression {@code "a" + "b"} becomes {@code "ab"}. This
     * occurs even if, when reading from left to right, the two string literals are not added
     * directly. For example, the expression {@code 1 + "a" + "b"} parses as {@code (1 + "a") +
     * "b"}}, but it is transformed into {@code 1 + "ab"}.
     *
     * <p>This is the same transformation performed by javac automatically. Javac seems to ignore
     * string literals surrounded in parentheses, so this method does as well.
     *
     * @param node a JavaParser Node
     */
    public static void concatenateAddedStringLiterals(Node node) {
        node.accept(new StringLiteralConcatenateVisitor(), null);
    }

    /** Visitor that combines added String literals, see {@link #concatenateAddedStringLiterals}. */
    public static class StringLiteralConcatenateVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(BinaryExpr node, Void p) {
            super.visit(node, p);
            if (node.getOperator() == BinaryExpr.Operator.PLUS
                    && node.getRight().isStringLiteralExpr()) {
                String right = node.getRight().asStringLiteralExpr().asString();
                if (node.getLeft().isStringLiteralExpr()) {
                    String left = node.getLeft().asStringLiteralExpr().asString();
                    node.replace(new StringLiteralExpr(left + right));
                } else if (node.getLeft().isBinaryExpr()) {
                    BinaryExpr leftExpr = node.getLeft().asBinaryExpr();
                    if (leftExpr.getOperator() == BinaryExpr.Operator.PLUS
                            && leftExpr.getRight().isStringLiteralExpr()) {
                        String left = leftExpr.getRight().asStringLiteralExpr().asString();
                        node.replace(
                                new BinaryExpr(
                                        leftExpr.getLeft(),
                                        new StringLiteralExpr(left + right),
                                        BinaryExpr.Operator.PLUS));
                    }
                }
            }
        }
    }
}
