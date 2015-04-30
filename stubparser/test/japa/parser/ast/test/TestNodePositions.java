/*
 * Created on 30/06/2008
 */
package org.checkerframework.stubparser.ast.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.checkerframework.stubparser.ast.BlockComment;
import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.ImportDeclaration;
import org.checkerframework.stubparser.ast.LineComment;
import org.checkerframework.stubparser.ast.Node;
import org.checkerframework.stubparser.ast.PackageDeclaration;
import org.checkerframework.stubparser.ast.TypeParameter;
import org.checkerframework.stubparser.ast.body.AnnotationDeclaration;
import org.checkerframework.stubparser.ast.body.AnnotationMemberDeclaration;
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
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.body.VariableDeclaratorId;
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
import org.checkerframework.stubparser.ast.stmt.SwitchEntryStmt;
import org.checkerframework.stubparser.ast.stmt.SwitchStmt;
import org.checkerframework.stubparser.ast.stmt.SynchronizedStmt;
import org.checkerframework.stubparser.ast.stmt.ThrowStmt;
import org.checkerframework.stubparser.ast.stmt.TryStmt;
import org.checkerframework.stubparser.ast.stmt.TypeDeclarationStmt;
import org.checkerframework.stubparser.ast.stmt.WhileStmt;
import org.checkerframework.stubparser.ast.test.classes.DumperTestClass;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.PrimitiveType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.VoidType;
import org.checkerframework.stubparser.ast.type.WildcardType;
import org.checkerframework.stubparser.ast.visitor.VoidVisitorAdapter;

import org.junit.Test;

/**
 * @author Julio Vilmar Gesser
 */
public class TestNodePositions {

    @Test
    public void testNodePositions() throws Exception {
        String source = Helper.readClass("./test", DumperTestClass.class);
        CompilationUnit cu = Helper.parserString(source);

        cu.accept(new TestVisitor(source), null);
    }

    void doTest(String source, Node node) {
        String parsed = node.toString();

        assertTrue(node.getClass().getName() + ": " + parsed, node.getBeginLine() >= 0);
        assertTrue(node.getClass().getName() + ": " + parsed, node.getBeginColumn() >= 0);
        assertTrue(node.getClass().getName() + ": " + parsed, node.getEndLine() >= 0);
        assertTrue(node.getClass().getName() + ": " + parsed, node.getEndColumn() >= 0);

        if (node.getBeginLine() == node.getEndLine()) {
            assertTrue(node.getClass().getName() + ": " + parsed, node.getBeginColumn() <= node.getEndColumn());
        } else {
            assertTrue(node.getClass().getName() + ": " + parsed, node.getBeginLine() <= node.getEndLine());
        }

        String substr = substring(source, node.getBeginLine(), node.getBeginColumn(), node.getEndLine(), node.getEndColumn());
        assertEquals(node.getClass().getName(), trimLines(parsed), trimLines(substr));
    }

    private String trimLines(String str) {
        String[] split = str.split("\n");
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            ret.append(split[i].trim());
            if (i < split.length - 1) {
                ret.append("\n");
            }
        }

        return ret.toString();
    }

    private String substring(String source, int beginLine, int beginColumn, int endLine, int endColumn) {
        int pos = 0;
        while (beginLine > 1) {
            if (source.charAt(pos) == '\n') {
                beginLine--;
                endLine--;
            }
            pos++;
        }
        int start = pos + beginColumn - 1;

        while (endLine > 1) {
            if (source.charAt(pos) == '\n') {
                endLine--;
            }
            pos++;
        }
        int end = pos + endColumn;

        return source.substring(start, end);
    }

    class TestVisitor extends VoidVisitorAdapter<Object> {

        private final String source;

        public TestVisitor(String source) {
            this.source = source;
        }

        @Override
        public void visit(AnnotationDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(AnnotationMemberDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ArrayAccessExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ArrayCreationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ArrayInitializerExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(AssertStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(AssignExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(BinaryExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(BlockComment n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(BlockStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(BooleanLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(BreakStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(CastExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(CatchClause n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(CharLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ClassExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ClassOrInterfaceType n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(CompilationUnit n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ConditionalExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ConstructorDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ContinueStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(DoStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(DoubleLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EmptyMemberDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EmptyStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EmptyTypeDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EnclosedExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EnumConstantDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(EnumDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ExpressionStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(FieldAccessExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(FieldDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ForeachStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ForStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(IfStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ImportDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(InitializerDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(InstanceOfExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(IntegerLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(IntegerLiteralMinValueExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(JavadocComment n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(LabeledStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(LineComment n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(LongLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(LongLiteralMinValueExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(MarkerAnnotationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(MemberValuePair n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodCallExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(NameExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(NormalAnnotationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(NullLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ObjectCreationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(PackageDeclaration n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(Parameter n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(PrimitiveType n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(QualifiedNameExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ReferenceType n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ReturnStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(SingleMemberAnnotationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(StringLiteralExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(SuperExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(SwitchEntryStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(SwitchStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(SynchronizedStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ThisExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(ThrowStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(TryStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(TypeDeclarationStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(TypeParameter n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(UnaryExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(VariableDeclarationExpr n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(VariableDeclarator n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(VariableDeclaratorId n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(VoidType n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(WhileStmt n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

        @Override
        public void visit(WildcardType n, Object arg) {
            doTest(source, n);
            super.visit(n, arg);
        }

    }

}
