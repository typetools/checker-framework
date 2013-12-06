/*
 * Created on 30/06/2008
 */
package cfjapa.parser.ast.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import cfjapa.parser.ast.BlockComment;
import cfjapa.parser.ast.CompilationUnit;
import cfjapa.parser.ast.ImportDeclaration;
import cfjapa.parser.ast.LineComment;
import cfjapa.parser.ast.Node;
import cfjapa.parser.ast.PackageDeclaration;
import cfjapa.parser.ast.TypeParameter;
import cfjapa.parser.ast.body.AnnotationDeclaration;
import cfjapa.parser.ast.body.AnnotationMemberDeclaration;
import cfjapa.parser.ast.body.ClassOrInterfaceDeclaration;
import cfjapa.parser.ast.body.ConstructorDeclaration;
import cfjapa.parser.ast.body.EmptyMemberDeclaration;
import cfjapa.parser.ast.body.EmptyTypeDeclaration;
import cfjapa.parser.ast.body.EnumConstantDeclaration;
import cfjapa.parser.ast.body.EnumDeclaration;
import cfjapa.parser.ast.body.FieldDeclaration;
import cfjapa.parser.ast.body.InitializerDeclaration;
import cfjapa.parser.ast.body.JavadocComment;
import cfjapa.parser.ast.body.MethodDeclaration;
import cfjapa.parser.ast.body.Parameter;
import cfjapa.parser.ast.body.VariableDeclarator;
import cfjapa.parser.ast.body.VariableDeclaratorId;
import cfjapa.parser.ast.expr.ArrayAccessExpr;
import cfjapa.parser.ast.expr.ArrayCreationExpr;
import cfjapa.parser.ast.expr.ArrayInitializerExpr;
import cfjapa.parser.ast.expr.AssignExpr;
import cfjapa.parser.ast.expr.BinaryExpr;
import cfjapa.parser.ast.expr.BooleanLiteralExpr;
import cfjapa.parser.ast.expr.CastExpr;
import cfjapa.parser.ast.expr.CharLiteralExpr;
import cfjapa.parser.ast.expr.ClassExpr;
import cfjapa.parser.ast.expr.ConditionalExpr;
import cfjapa.parser.ast.expr.DoubleLiteralExpr;
import cfjapa.parser.ast.expr.EnclosedExpr;
import cfjapa.parser.ast.expr.FieldAccessExpr;
import cfjapa.parser.ast.expr.InstanceOfExpr;
import cfjapa.parser.ast.expr.IntegerLiteralExpr;
import cfjapa.parser.ast.expr.IntegerLiteralMinValueExpr;
import cfjapa.parser.ast.expr.LongLiteralExpr;
import cfjapa.parser.ast.expr.LongLiteralMinValueExpr;
import cfjapa.parser.ast.expr.MarkerAnnotationExpr;
import cfjapa.parser.ast.expr.MemberValuePair;
import cfjapa.parser.ast.expr.MethodCallExpr;
import cfjapa.parser.ast.expr.NameExpr;
import cfjapa.parser.ast.expr.NormalAnnotationExpr;
import cfjapa.parser.ast.expr.NullLiteralExpr;
import cfjapa.parser.ast.expr.ObjectCreationExpr;
import cfjapa.parser.ast.expr.QualifiedNameExpr;
import cfjapa.parser.ast.expr.SingleMemberAnnotationExpr;
import cfjapa.parser.ast.expr.StringLiteralExpr;
import cfjapa.parser.ast.expr.SuperExpr;
import cfjapa.parser.ast.expr.ThisExpr;
import cfjapa.parser.ast.expr.UnaryExpr;
import cfjapa.parser.ast.expr.VariableDeclarationExpr;
import cfjapa.parser.ast.stmt.AssertStmt;
import cfjapa.parser.ast.stmt.BlockStmt;
import cfjapa.parser.ast.stmt.BreakStmt;
import cfjapa.parser.ast.stmt.CatchClause;
import cfjapa.parser.ast.stmt.ContinueStmt;
import cfjapa.parser.ast.stmt.DoStmt;
import cfjapa.parser.ast.stmt.EmptyStmt;
import cfjapa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import cfjapa.parser.ast.stmt.ExpressionStmt;
import cfjapa.parser.ast.stmt.ForStmt;
import cfjapa.parser.ast.stmt.ForeachStmt;
import cfjapa.parser.ast.stmt.IfStmt;
import cfjapa.parser.ast.stmt.LabeledStmt;
import cfjapa.parser.ast.stmt.ReturnStmt;
import cfjapa.parser.ast.stmt.SwitchEntryStmt;
import cfjapa.parser.ast.stmt.SwitchStmt;
import cfjapa.parser.ast.stmt.SynchronizedStmt;
import cfjapa.parser.ast.stmt.ThrowStmt;
import cfjapa.parser.ast.stmt.TryStmt;
import cfjapa.parser.ast.stmt.TypeDeclarationStmt;
import cfjapa.parser.ast.stmt.WhileStmt;
import cfjapa.parser.ast.test.classes.DumperTestClass;
import cfjapa.parser.ast.type.ClassOrInterfaceType;
import cfjapa.parser.ast.type.PrimitiveType;
import cfjapa.parser.ast.type.ReferenceType;
import cfjapa.parser.ast.type.VoidType;
import cfjapa.parser.ast.type.WildcardType;
import cfjapa.parser.ast.visitor.VoidVisitorAdapter;

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
