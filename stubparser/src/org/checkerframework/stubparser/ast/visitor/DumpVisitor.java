/*
 * Copyright (C) 2007 Júlio Vilmar Gesser.
 * 
 * This file is part of Java 1.5 parser and Abstract Syntax Tree.
 *
 * Java 1.5 parser and Abstract Syntax Tree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java 1.5 parser and Abstract Syntax Tree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java 1.5 parser and Abstract Syntax Tree.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 05/10/2006
 */
package org.checkerframework.stubparser.ast.visitor;

import java.util.Iterator;
import java.util.List;

import org.checkerframework.stubparser.ast.BlockComment;
import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.ImportDeclaration;
import org.checkerframework.stubparser.ast.IndexUnit;
import org.checkerframework.stubparser.ast.LineComment;
import org.checkerframework.stubparser.ast.PackageDeclaration;
import org.checkerframework.stubparser.ast.TypeParameter;
import org.checkerframework.stubparser.ast.body.AnnotationDeclaration;
import org.checkerframework.stubparser.ast.body.AnnotationMemberDeclaration;
import org.checkerframework.stubparser.ast.body.BodyDeclaration;
import org.checkerframework.stubparser.ast.body.ClassOrInterfaceDeclaration;
import org.checkerframework.stubparser.ast.body.ConstructorDeclaration;
import org.checkerframework.stubparser.ast.body.EmptyMemberDeclaration;
import org.checkerframework.stubparser.ast.body.EmptyTypeDeclaration;
import org.checkerframework.stubparser.ast.body.EnumConstantDeclaration;
import org.checkerframework.stubparser.ast.body.EnumDeclaration;
import org.checkerframework.stubparser.ast.body.FieldDeclaration;
import org.checkerframework.stubparser.ast.body.InitializerDeclaration;
import org.checkerframework.stubparser.ast.body.JavadocComment;
import org.checkerframework.stubparser.ast.body.MethodDeclaration;
import org.checkerframework.stubparser.ast.body.ModifierSet;
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.TypeDeclaration;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.body.VariableDeclaratorId;
import org.checkerframework.stubparser.ast.expr.AnnotationExpr;
import org.checkerframework.stubparser.ast.expr.ArrayAccessExpr;
import org.checkerframework.stubparser.ast.expr.ArrayCreationExpr;
import org.checkerframework.stubparser.ast.expr.ArrayInitializerExpr;
import org.checkerframework.stubparser.ast.expr.AssignExpr;
import org.checkerframework.stubparser.ast.expr.BinaryExpr;
import org.checkerframework.stubparser.ast.expr.BooleanLiteralExpr;
import org.checkerframework.stubparser.ast.expr.CastExpr;
import org.checkerframework.stubparser.ast.expr.CharLiteralExpr;
import org.checkerframework.stubparser.ast.expr.ClassExpr;
import org.checkerframework.stubparser.ast.expr.ConditionalExpr;
import org.checkerframework.stubparser.ast.expr.DoubleLiteralExpr;
import org.checkerframework.stubparser.ast.expr.EnclosedExpr;
import org.checkerframework.stubparser.ast.expr.Expression;
import org.checkerframework.stubparser.ast.expr.FieldAccessExpr;
import org.checkerframework.stubparser.ast.expr.InstanceOfExpr;
import org.checkerframework.stubparser.ast.expr.IntegerLiteralExpr;
import org.checkerframework.stubparser.ast.expr.IntegerLiteralMinValueExpr;
import org.checkerframework.stubparser.ast.expr.LongLiteralExpr;
import org.checkerframework.stubparser.ast.expr.LongLiteralMinValueExpr;
import org.checkerframework.stubparser.ast.expr.MarkerAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.MemberValuePair;
import org.checkerframework.stubparser.ast.expr.MethodCallExpr;
import org.checkerframework.stubparser.ast.expr.NameExpr;
import org.checkerframework.stubparser.ast.expr.NormalAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.NullLiteralExpr;
import org.checkerframework.stubparser.ast.expr.ObjectCreationExpr;
import org.checkerframework.stubparser.ast.expr.QualifiedNameExpr;
import org.checkerframework.stubparser.ast.expr.SingleMemberAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.StringLiteralExpr;
import org.checkerframework.stubparser.ast.expr.SuperExpr;
import org.checkerframework.stubparser.ast.expr.ThisExpr;
import org.checkerframework.stubparser.ast.expr.UnaryExpr;
import org.checkerframework.stubparser.ast.expr.VariableDeclarationExpr;
import org.checkerframework.stubparser.ast.stmt.AssertStmt;
import org.checkerframework.stubparser.ast.stmt.BlockStmt;
import org.checkerframework.stubparser.ast.stmt.BreakStmt;
import org.checkerframework.stubparser.ast.stmt.CatchClause;
import org.checkerframework.stubparser.ast.stmt.ContinueStmt;
import org.checkerframework.stubparser.ast.stmt.DoStmt;
import org.checkerframework.stubparser.ast.stmt.EmptyStmt;
import org.checkerframework.stubparser.ast.stmt.ExplicitConstructorInvocationStmt;
import org.checkerframework.stubparser.ast.stmt.ExpressionStmt;
import org.checkerframework.stubparser.ast.stmt.ForStmt;
import org.checkerframework.stubparser.ast.stmt.ForeachStmt;
import org.checkerframework.stubparser.ast.stmt.IfStmt;
import org.checkerframework.stubparser.ast.stmt.LabeledStmt;
import org.checkerframework.stubparser.ast.stmt.ReturnStmt;
import org.checkerframework.stubparser.ast.stmt.Statement;
import org.checkerframework.stubparser.ast.stmt.SwitchEntryStmt;
import org.checkerframework.stubparser.ast.stmt.SwitchStmt;
import org.checkerframework.stubparser.ast.stmt.SynchronizedStmt;
import org.checkerframework.stubparser.ast.stmt.ThrowStmt;
import org.checkerframework.stubparser.ast.stmt.TryStmt;
import org.checkerframework.stubparser.ast.stmt.TypeDeclarationStmt;
import org.checkerframework.stubparser.ast.stmt.WhileStmt;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.PrimitiveType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.Type;
import org.checkerframework.stubparser.ast.type.VoidType;
import org.checkerframework.stubparser.ast.type.WildcardType;

/**
 * Builds a string representation of any AST node for printing.
 *
 * @author Julio Vilmar Gesser
 */

public final class DumpVisitor implements VoidVisitor<Object> {

    // Writes to StringBuilder buffer.
    private static class SourcePrinter {

        private int level = 0;

        private boolean indented = false;

        private final StringBuilder buf = new StringBuilder();

        public void indent() {
            level++;
        }

        public void unindent() {
            level--;
        }

        private void makeIndent() {
            for (int i = 0; i < level; i++) {
                buf.append("    ");
            }
        }

        public void print(String arg) {
            if (!indented) {
                makeIndent();
                indented = true;
            }
            buf.append(arg);
        }

        public void printLn(String arg) {
            print(arg);
            printLn();
        }

        public void printLn() {
            buf.append("\n");
            indented = false;
        }

        public String getSource() {
            return buf.toString();
        }

        /*@org.checkerframework.dataflow.qual.SideEffectFree*/
        @Override
        public String toString() {
            return getSource();
        }
    }

    private final SourcePrinter printer = new SourcePrinter();

    /** Return buffered contents of SourcePrinter. */
    public String getSource() {
        return printer.getSource();
    }

    /**
     * Print Java modifiers indicated by the bit flags in the argument.
     *
     * @param modifiers the Java modifiers represented as a set of bit flags
     */
    private void printModifiers(int modifiers) {
        if (ModifierSet.isPrivate(modifiers)) {
            printer.print("private ");
        }
        if (ModifierSet.isProtected(modifiers)) {
            printer.print("protected ");
        }
        if (ModifierSet.isPublic(modifiers)) {
            printer.print("public ");
        }
        if (ModifierSet.isAbstract(modifiers)) {
            printer.print("abstract ");
        }
        if (ModifierSet.isStatic(modifiers)) {
            printer.print("static ");
        }
        if (ModifierSet.isFinal(modifiers)) {
            printer.print("final ");
        }
        if (ModifierSet.isNative(modifiers)) {
            printer.print("native ");
        }
        if (ModifierSet.isStrictfp(modifiers)) {
            printer.print("strictfp ");
        }
        if (ModifierSet.isSynchronized(modifiers)) {
            printer.print("synchronized ");
        }
        if (ModifierSet.isTransient(modifiers)) {
            printer.print("transient ");
        }
        if (ModifierSet.isVolatile(modifiers)) {
            printer.print("volatile ");
        }
    }

    /**
     * Print out declarations from a stub file.
     *
     * @param members list of declarations
     * @param arg argument provided to the calling visitor method
     */
    private void printMembers(List<BodyDeclaration> members, Object arg) {
        for (BodyDeclaration member : members) {
            printer.printLn();
            member.accept(this, arg);
            printer.printLn();
        }
    }

    /**
     * Print a list of annotations on separate lines, ending with a newline.
     *
     * @param annotations list of {@link AnnotationExpr annotation expressions}
     * @param arg argument provided to the calling visitor method
     */
    private void printMemberAnnotations(List<AnnotationExpr> annotations, Object arg) {
        if (annotations != null) {
            for (AnnotationExpr a : annotations) {
                a.accept(this, arg);
                printer.printLn();
            }
        }
    }

    /**
     * Print a list of annotations, including a trailing space.
     *
     * @param annotations list of {@link AnnotationExpr annotation expressions}
     * @param arg argument provided to the calling visitor method
     */
    private void printAnnotations(List<AnnotationExpr> annotations,
            Object arg) {
        printAnnotations(annotations, arg, false);
    }

    /**
     * Print a list of annotations.  The final parameter determines whether to
     * issue a leading (if true) or trailing (if false) space.
     *
     * @param annotations list of {@link AnnotationExpr annotation expressions}
     * @param arg argument provided to the calling visitor method
     * @param isOnReceiver whether annotation is on a method receiver
     */
    private void printAnnotations(List<AnnotationExpr> annotations,
            Object arg, boolean isOnReceiver) {
        if (annotations != null) {
            Iterator<AnnotationExpr> i = annotations.iterator();
            if (i.hasNext()) {
                AnnotationExpr a = i.next();
                if (isOnReceiver) { printer.print(" "); }
                a.accept(this, arg);
                while (i.hasNext()) {
                    printer.print(" ");
                    a = i.next();
                    a.accept(this, arg);
                }
                if (!isOnReceiver) { printer.print(" "); }
            }
        }
    }

    /**
     * Print a list of type arguments in between angle brackets.
     *
     * @param args type arguments
     * @param arg argument provided to the calling visitor method
     */
    private void printTypeArgs(List<Type> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<Type> i = args.iterator(); i.hasNext();) {
                Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    /**
     * Print a list of type parameters in between angle brackets.
     *
     * @param args type parameters
     * @param arg argument provided to the calling visitor method
     */
    private void printTypeParameters(List<TypeParameter> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
                TypeParameter t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    /**
     * Print a list of method arguments in between parentheses.
     *
     * @param args method arguments
     * @param arg argument provided to the calling visitor method
     */
    private void printArguments(List<Expression> args, Object arg) {
        printer.print("(");
        if (args != null) {
            for (Iterator<Expression> i = args.iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    /**
     * Print a javadoc comment.
     *
     * @param javadoc comment
     * @param arg argument provided to the calling visitor method
     */
    private void printJavadoc(JavadocComment javadoc, Object arg) {
        if (javadoc != null) {
            javadoc.accept(this, arg);
        }
    }

    // Visitor interface implementation follows

    public void visit(IndexUnit n, Object arg) {
        for (CompilationUnit unit : n.getCompilationUnits()) {
            visit(unit, arg);
            printer.printLn();
            printer.printLn();
        }
    }

    public void visit(CompilationUnit n, Object arg) {
        if (n.getPackage() != null) {
            n.getPackage().accept(this, arg);
        }
        if (n.getImports() != null) {
            for (ImportDeclaration i : n.getImports()) {
                i.accept(this, arg);
            }
            printer.printLn();
        }
        if (n.getTypes() != null) {
            for (Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
                i.next().accept(this, arg);
                printer.printLn();
                if (i.hasNext()) {
                    printer.printLn();
                }
            }
        }
    }

    public void visit(PackageDeclaration n, Object arg) {
        printMemberAnnotations(n.getAnnotations(), arg);
        printer.print("package ");
        n.getName().accept(this, arg);
        printer.printLn(";");
        printer.printLn();
    }

    public void visit(NameExpr n, Object arg) {
        printer.print(n.getName());
    }

    public void visit(QualifiedNameExpr n, Object arg) {
        n.getQualifier().accept(this, arg);
        printer.print(".");
        printer.print(n.getName());
    }

    public void visit(ImportDeclaration n, Object arg) {
        printer.print("import ");
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getName().accept(this, arg);
        if (n.isAsterisk()) {
            printer.print(".*");
        }
        printer.printLn(";");
    }

    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        if (n.isInterface()) {
            printer.print("interface ");
        } else {
            printer.print("class ");
        }

        printer.print(n.getName());

        printTypeParameters(n.getTypeParameters(), arg);

        if (n.getExtends() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.printLn(" {");
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(EmptyTypeDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printer.print(";");
    }

    public void visit(JavadocComment n, Object arg) {
        printer.print("/**");
        printer.print(n.getContent());
        printer.printLn("*/");
    }

    public void visit(ClassOrInterfaceType n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printer.print(n.getName());
        printTypeArgs(n.getTypeArgs(), arg);
    }

    public void visit(TypeParameter n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printer.print(n.getName());
        if (n.getTypeBound() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(" & ");
                }
            }
        }
    }

    public void visit(PrimitiveType n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);

        switch (n.getType()) {
            case Boolean:
                printer.print("boolean");
                break;
            case Byte:
                printer.print("byte");
                break;
            case Char:
                printer.print("char");
                break;
            case Double:
                printer.print("double");
                break;
            case Float:
                printer.print("float");
                break;
            case Int:
                printer.print("int");
                break;
            case Long:
                printer.print("long");
                break;
            case Short:
                printer.print("short");
                break;
        }
    }

    public void visit(ReferenceType n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);

        n.getType().accept(this, arg);
        for (int i = 0; i < n.getArrayCount(); i++) {
            if (!n.getAnnotationsAtLevel(i).isEmpty()) {
                printer.print(" ");
                printAnnotations(n.getAnnotationsAtLevel(i), arg);
            }
            printer.print("[]");
        }
    }

    public void visit(WildcardType n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printer.print("?");
        if (n.getExtends() != null) {
            printer.print(" extends ");
            n.getExtends().accept(this, arg);
        }
        if (n.getSuper() != null) {
            printer.print(" super ");
            n.getSuper().accept(this, arg);
        }
    }

    public void visit(FieldDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());
        n.getType().accept(this, arg);

        printer.print(" ");
        for (Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
            VariableDeclarator var = i.next();
            var.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }

        printer.print(";");
    }

    public void visit(VariableDeclarator n, Object arg) {
        n.getId().accept(this, arg);
        if (n.getInit() != null) {
            printer.print(" = ");
            n.getInit().accept(this, arg);
        }
    }

    public void visit(VariableDeclaratorId n, Object arg) {
        printer.print(n.getName());
        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }
    }

    public void visit(ArrayInitializerExpr n, Object arg) {
        printer.print("{");
        if (n.getValues() != null) {
            printer.print(" ");
            for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
                Expression expr = i.next();
                expr.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(" ");
        }
        printer.print("}");
    }

    public void visit(VoidType n, Object arg) {
        printer.print("void");
    }

    public void visit(ArrayAccessExpr n, Object arg) {
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
    }

    public void visit(ArrayCreationExpr n, Object arg) {
        printer.print("new ");
        n.getType().accept(this, arg);

        if (n.getDimensions() != null) {
            for (Expression dim : n.getDimensions()) {
                printer.print("[");
                dim.accept(this, arg);
                printer.print("]");
            }
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
        } else {
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
            printer.print(" ");
            n.getInitializer().accept(this, arg);
        }
    }

    public void visit(AssignExpr n, Object arg) {
        n.getTarget().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case assign:
                printer.print("=");
                break;
            case and:
                printer.print("&=");
                break;
            case or:
                printer.print("|=");
                break;
            case xor:
                printer.print("^=");
                break;
            case plus:
                printer.print("+=");
                break;
            case minus:
                printer.print("-=");
                break;
            case rem:
                printer.print("%=");
                break;
            case slash:
                printer.print("/=");
                break;
            case star:
                printer.print("*=");
                break;
            case lShift:
                printer.print("<<=");
                break;
            case rSignedShift:
                printer.print(">>=");
                break;
            case rUnsignedShift:
                printer.print(">>>=");
                break;
        }
        printer.print(" ");
        n.getValue().accept(this, arg);
    }

    public void visit(BinaryExpr n, Object arg) {
        n.getLeft().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case or:
                printer.print("||");
                break;
            case and:
                printer.print("&&");
                break;
            case binOr:
                printer.print("|");
                break;
            case binAnd:
                printer.print("&");
                break;
            case xor:
                printer.print("^");
                break;
            case equals:
                printer.print("==");
                break;
            case notEquals:
                printer.print("!=");
                break;
            case less:
                printer.print("<");
                break;
            case greater:
                printer.print(">");
                break;
            case lessEquals:
                printer.print("<=");
                break;
            case greaterEquals:
                printer.print(">=");
                break;
            case lShift:
                printer.print("<<");
                break;
            case rSignedShift:
                printer.print(">>");
                break;
            case rUnsignedShift:
                printer.print(">>>");
                break;
            case plus:
                printer.print("+");
                break;
            case minus:
                printer.print("-");
                break;
            case times:
                printer.print("*");
                break;
            case divide:
                printer.print("/");
                break;
            case remainder:
                printer.print("%");
                break;
        }
        printer.print(" ");
        n.getRight().accept(this, arg);
    }

    public void visit(CastExpr n, Object arg) {
        printer.print("(");
        n.getType().accept(this, arg);
        printer.print(") ");
        n.getExpr().accept(this, arg);
    }

    public void visit(ClassExpr n, Object arg) {
        n.getType().accept(this, arg);
        printer.print(".class");
    }

    public void visit(ConditionalExpr n, Object arg) {
        n.getCondition().accept(this, arg);
        printer.print(" ? ");
        n.getThenExpr().accept(this, arg);
        printer.print(" : ");
        n.getElseExpr().accept(this, arg);
    }

    public void visit(EnclosedExpr n, Object arg) {
        printer.print("(");
        n.getInner().accept(this, arg);
        printer.print(")");
    }

    public void visit(FieldAccessExpr n, Object arg) {
        n.getScope().accept(this, arg);
        printer.print(".");
        printer.print(n.getField());
    }

    public void visit(InstanceOfExpr n, Object arg) {
        n.getExpr().accept(this, arg);
        printer.print(" instanceof ");
        n.getType().accept(this, arg);
    }

    public void visit(CharLiteralExpr n, Object arg) {
        printer.print("'");
        printer.print(n.getValue());
        printer.print("'");
    }

    public void visit(DoubleLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(StringLiteralExpr n, Object arg) {
        printer.print("\"");
        printer.print(n.getValue());
        printer.print("\"");
    }

    public void visit(BooleanLiteralExpr n, Object arg) {
        printer.print(String.valueOf(n.getValue()));
    }

    public void visit(NullLiteralExpr n, Object arg) {
        printer.print("null");
    }

    public void visit(ThisExpr n, Object arg) {
        if (n.getClassExpr() != null) {
            n.getClassExpr().accept(this, arg);
            printer.print(".");
        }
        printer.print("this");
    }

    public void visit(SuperExpr n, Object arg) {
        if (n.getClassExpr() != null) {
            n.getClassExpr().accept(this, arg);
            printer.print(".");
        }
        printer.print("super");
    }

    public void visit(MethodCallExpr n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printTypeArgs(n.getTypeArgs(), arg);
        printer.print(n.getName());
        printArguments(n.getArgs(), arg);
    }

    public void visit(ObjectCreationExpr n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }

        printer.print("new ");

        printTypeArgs(n.getTypeArgs(), arg);
        n.getType().accept(this, arg);

        printArguments(n.getArgs(), arg);

        if (n.getAnonymousClassBody() != null) {
            printer.printLn(" {");
            printer.indent();
            printMembers(n.getAnonymousClassBody(), arg);
            printer.unindent();
            printer.print("}");
        }
    }

    public void visit(UnaryExpr n, Object arg) {
        switch (n.getOperator()) {
            case positive:
                printer.print("+");
                break;
            case negative:
                printer.print("-");
                break;
            case inverse:
                printer.print("~");
                break;
            case not:
                printer.print("!");
                break;
            case preIncrement:
                printer.print("++");
                break;
            case preDecrement:
                printer.print("--");
                break;
        }

        n.getExpr().accept(this, arg);

        switch (n.getOperator()) {
            case posIncrement:
                printer.print("++");
                break;
            case posDecrement:
                printer.print("--");
                break;
        }
    }

    public void visit(ConstructorDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        if (n.getTypeParameters() != null) {
            printTypeParameters(n.getTypeParameters(), arg);
            printer.print(" ");
        }
        printer.print(n.getName());

        printer.print("(");
        if (n.getParameters() != null) {
            for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        printAnnotations(n.getReceiverAnnotations(), arg, true);

        if (n.getThrows() != null) {
            printer.print(" throws ");
            for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
                NameExpr name = i.next();
                name.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        if (n.getBlock() == null) {
            printer.print(";");
        } else {
            printer.print(" ");
            n.getBlock().accept(this, arg);
        }
    }

    public void visit(MethodDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printTypeParameters(n.getTypeParameters(), arg);
        if (n.getTypeParameters() != null) {
            printer.print(" ");
        }

        n.getType().accept(this, arg);
        printer.print(" ");
        printer.print(n.getName());

        printer.print("(");
        if (n.getParameters() != null) {
            for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }

        printAnnotations(n.getReceiverAnnotations(), arg, true);

        if (n.getThrows() != null) {
            printer.print(" throws ");
            for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
                NameExpr name = i.next();
                name.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (n.getBody() == null) {
            printer.print(";");
        } else {
            printer.print(" ");
            n.getBody().accept(this, arg);
        }
    }

    public void visit(Parameter n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        if (n.isVarArgs()) {
            printer.print("...");
        }
        printer.print(" ");
        n.getId().accept(this, arg);
    }

    public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
        if (n.isThis()) {
            printTypeArgs(n.getTypeArgs(), arg);
            printer.print("this");
        } else {
            if (n.getExpr() != null) {
                n.getExpr().accept(this, arg);
                printer.print(".");
            }
            printTypeArgs(n.getTypeArgs(), arg);
            printer.print("super");
        }
        printArguments(n.getArgs(), arg);
        printer.print(";");
    }

    public void visit(VariableDeclarationExpr n, Object arg) {
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");

        for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
            VariableDeclarator v = i.next();
            v.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
    }

    public void visit(TypeDeclarationStmt n, Object arg) {
        n.getTypeDeclaration().accept(this, arg);
    }

    public void visit(AssertStmt n, Object arg) {
        printer.print("assert ");
        n.getCheck().accept(this, arg);
        if (n.getMessage() != null) {
            printer.print(" : ");
            n.getMessage().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(BlockStmt n, Object arg) {
        printer.printLn("{");
        if (n.getStmts() != null) {
            printer.indent();
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                printer.printLn();
            }
            printer.unindent();
        }
        printer.print("}");

    }

    public void visit(LabeledStmt n, Object arg) {
        printer.print(n.getLabel());
        printer.print(": ");
        n.getStmt().accept(this, arg);
    }

    public void visit(EmptyStmt n, Object arg) {
        printer.print(";");
    }

    public void visit(ExpressionStmt n, Object arg) {
        n.getExpression().accept(this, arg);
        printer.print(";");
    }

    public void visit(SwitchStmt n, Object arg) {
        printer.print("switch(");
        n.getSelector().accept(this, arg);
        printer.printLn(") {");
        if (n.getEntries() != null) {
            printer.indent();
            for (SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
            }
            printer.unindent();
        }
        printer.print("}");

    }

    public void visit(SwitchEntryStmt n, Object arg) {
        if (n.getLabel() != null) {
            printer.print("case ");
            n.getLabel().accept(this, arg);
            printer.print(":");
        } else {
            printer.print("default:");
        }
        printer.printLn();
        printer.indent();
        if (n.getStmts() != null) {
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                printer.printLn();
            }
        }
        printer.unindent();
    }

    public void visit(BreakStmt n, Object arg) {
        printer.print("break");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    public void visit(ReturnStmt n, Object arg) {
        printer.print("return");
        if (n.getExpr() != null) {
            printer.print(" ");
            n.getExpr().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(EnumDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("enum ");
        printer.print(n.getName());

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.printLn(" {");
        printer.indent();
        if (n.getEntries() != null) {
            printer.printLn();
            for (Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
                EnumConstantDeclaration e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (n.getMembers() != null) {
            printer.printLn(";");
            printMembers(n.getMembers(), arg);
        } else {
            if (n.getEntries() != null) {
                printer.printLn();
            }
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(EnumConstantDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printer.print(n.getName());

        if (n.getArgs() != null) {
            printArguments(n.getArgs(), arg);
        }

        if (n.getClassBody() != null) {
            printer.printLn(" {");
            printer.indent();
            printMembers(n.getClassBody(), arg);
            printer.unindent();
            printer.printLn("}");
        }
    }

    public void visit(EmptyMemberDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printer.print(";");
    }

    public void visit(InitializerDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getBlock().accept(this, arg);
    }

    public void visit(IfStmt n, Object arg) {
        printer.print("if (");
        n.getCondition().accept(this, arg);
        printer.print(") ");
        n.getThenStmt().accept(this, arg);
        if (n.getElseStmt() != null) {
            printer.print(" else ");
            n.getElseStmt().accept(this, arg);
        }
    }

    public void visit(WhileStmt n, Object arg) {
        printer.print("while (");
        n.getCondition().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    public void visit(ContinueStmt n, Object arg) {
        printer.print("continue");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    public void visit(DoStmt n, Object arg) {
        printer.print("do ");
        n.getBody().accept(this, arg);
        printer.print(" while (");
        n.getCondition().accept(this, arg);
        printer.print(");");
    }

    public void visit(ForeachStmt n, Object arg) {
        printer.print("for (");
        n.getVariable().accept(this, arg);
        printer.print(" : ");
        n.getIterable().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    public void visit(ForStmt n, Object arg) {
        printer.print("for (");
        if (n.getInit() != null) {
            for (Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print("; ");
        if (n.getCompare() != null) {
            n.getCompare().accept(this, arg);
        }
        printer.print("; ");
        if (n.getUpdate() != null) {
            for (Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    public void visit(ThrowStmt n, Object arg) {
        printer.print("throw ");
        n.getExpr().accept(this, arg);
        printer.print(";");
    }

    public void visit(SynchronizedStmt n, Object arg) {
        printer.print("synchronized (");
        n.getExpr().accept(this, arg);
        printer.print(") ");
        n.getBlock().accept(this, arg);
    }

    public void visit(TryStmt n, Object arg) {
        printer.print("try ");
        n.getTryBlock().accept(this, arg);
        if (n.getCatchs() != null) {
            for (CatchClause c : n.getCatchs()) {
                c.accept(this, arg);
            }
        }
        if (n.getFinallyBlock() != null) {
            printer.print(" finally ");
            n.getFinallyBlock().accept(this, arg);
        }
    }

    public void visit(CatchClause n, Object arg) {
        printer.print(" catch (");
        n.getExcept().accept(this, arg);
        printer.print(") ");
        n.getCatchBlock().accept(this, arg);

    }

    public void visit(AnnotationDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("@interface ");
        printer.print(n.getName());
        printer.printLn(" {");
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(AnnotationMemberDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");
        printer.print(n.getName());
        printer.print("()");
        if (n.getDefaultValue() != null) {
            printer.print(" default ");
            n.getDefaultValue().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(MarkerAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
    }

    public void visit(SingleMemberAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        n.getMemberValue().accept(this, arg);
        printer.print(")");
    }

    public void visit(NormalAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        if (n.getPairs() != null) {
            for (Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext();) {
                MemberValuePair m = i.next();
                m.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    public void visit(MemberValuePair n, Object arg) {
        printer.print(n.getName());
        printer.print(" = ");
        n.getValue().accept(this, arg);
    }

    public void visit(LineComment n, Object arg) {
        printer.print("//");
        printer.printLn(n.getContent());
    }

    public void visit(BlockComment n, Object arg) {
        printer.print("/*");
        printer.print(n.getContent());
        printer.printLn("*/");
    }

}
