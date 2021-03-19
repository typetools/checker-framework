package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;

/**
 * An implementation of JointJavacJavaParserVisitor where {@code process} methods do nothing. To use
 * this class, extend it and override a {@code process} method.
 */
public class DefaultJointVisitor extends JointJavacJavaParserVisitor {
    @Override
    public void processAnnotation(AnnotationTree javacTree, NormalAnnotationExpr javaParserNode) {}

    @Override
    public void processAnnotation(AnnotationTree javacTree, MarkerAnnotationExpr javaParserNode) {}

    @Override
    public void processAnnotation(
            AnnotationTree javacTree, SingleMemberAnnotationExpr javaParserNode) {}

    @Override
    public void processAnnotatedType(AnnotatedTypeTree javacTree, Node javaParserNode) {}

    @Override
    public void processArrayAccess(ArrayAccessTree javacTree, ArrayAccessExpr javaParserNode) {}

    @Override
    public void processArrayType(ArrayTypeTree javacTree, ArrayType javaParserNode) {}

    @Override
    public void processAssert(AssertTree javacTree, AssertStmt javaParserNode) {}

    @Override
    public void processAssignment(AssignmentTree javacTree, AssignExpr javaParserNode) {}

    @Override
    public void processBinary(BinaryTree javacTree, BinaryExpr javaParserNode) {}

    @Override
    public void processBlock(BlockTree javacTree, BlockStmt javaParserNode) {}

    @Override
    public void processBreak(BreakTree javacTree, BreakStmt javaParserNode) {}

    @Override
    public void processCase(CaseTree javacTree, SwitchEntry javaParserNode) {}

    @Override
    public void processCatch(CatchTree javacTree, CatchClause javaParserNode) {}

    @Override
    public void processClass(ClassTree javacTree, AnnotationDeclaration javaParserNode) {}

    @Override
    public void processClass(ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {}

    @Override
    public void processClass(ClassTree javacTree, EnumDeclaration javaParserNode) {}

    @Override
    public void processCompilationUnit(
            CompilationUnitTree javacTree, CompilationUnit javaParserNode) {}

    @Override
    public void processConditionalExpression(
            ConditionalExpressionTree javacTree, ConditionalExpr javaParserNode) {}

    @Override
    public void processContinue(ContinueTree javacTree, ContinueStmt javaParserNode) {}

    @Override
    public void processDoWhileLoop(DoWhileLoopTree javacTree, DoStmt javaParserNode) {}

    @Override
    public void processEmptyStatement(EmptyStatementTree javacTree, EmptyStmt javaParserNode) {}

    @Override
    public void processEnhancedForLoop(EnhancedForLoopTree javacTree, ForEachStmt javaParserNode) {}

    @Override
    public void processExports(ExportsTree javacTree, ModuleExportsDirective javaParserNode) {}

    @Override
    public void processExpressionStatemen(
            ExpressionStatementTree javacTree, ExpressionStmt javaParserNode) {}

    @Override
    public void processForLoop(ForLoopTree javacTree, ForStmt javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, ClassOrInterfaceType javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, Name javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, NameExpr javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, SimpleName javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, SuperExpr javaParserNode) {}

    @Override
    public void processIdentifier(IdentifierTree javacTree, ThisExpr javaParserNode) {}

    @Override
    public void processIf(IfTree javacTree, IfStmt javaParserNode) {}

    @Override
    public void processImport(ImportTree javacTree, ImportDeclaration javaParserNode) {}

    @Override
    public void processInstanceOf(InstanceOfTree javacTree, InstanceOfExpr javaParserNode) {}

    @Override
    public void processIntersectionType(
            IntersectionTypeTree javacTree, IntersectionType javaParserNode) {}

    @Override
    public void processLabeledStatement(
            LabeledStatementTree javacTree, LabeledStmt javaParserNode) {}

    @Override
    public void processLambdaExpression(
            LambdaExpressionTree javacTree, LambdaExpr javaParserNode) {}

    @Override
    public void processLiteral(LiteralTree javacTree, BinaryExpr javaParserNode) {}

    @Override
    public void processLiteral(LiteralTree javacTree, UnaryExpr javaParserNode) {}

    @Override
    public void processLiteral(LiteralTree javacTree, LiteralExpr javaParserNode) {}

    @Override
    public void processMemberReference(
            MemberReferenceTree javacTree, MethodReferenceExpr javaParserNode) {}

    @Override
    public void processMemberSelect(MemberSelectTree javacTree, ClassExpr javaParserNode) {}

    @Override
    public void processMemberSelect(
            MemberSelectTree javacTree, ClassOrInterfaceType javaParserNode) {}

    @Override
    public void processMemberSelect(MemberSelectTree javacTree, FieldAccessExpr javaParserNode) {}

    @Override
    public void processMemberSelect(MemberSelectTree javacTree, Name javaParserNode) {}

    @Override
    public void processMemberSelect(MemberSelectTree javacTree, ThisExpr javaParserNode) {}

    @Override
    public void processMemberSelect(MemberSelectTree javacTree, SuperExpr javaParserNode) {}

    @Override
    public void processMethod(MethodTree javacTree, MethodDeclaration javaParserNode) {}

    @Override
    public void processMethod(MethodTree javacTree, ConstructorDeclaration javaParserNode) {}

    @Override
    public void processMethod(MethodTree javacTree, AnnotationMemberDeclaration javaParserNode) {}

    @Override
    public void processMethodInvocation(
            MethodInvocationTree javacTree, ExplicitConstructorInvocationStmt javaParserNode) {}

    @Override
    public void processMethodInvocation(
            MethodInvocationTree javacTree, MethodCallExpr javaParserNode) {}

    @Override
    public void processModule(ModuleTree javacTree, ModuleDeclaration javaParserNode) {}

    @Override
    public void processNewClass(NewClassTree javacTree, ObjectCreationExpr javaParserNode) {}

    @Override
    public void processOpens(OpensTree javacTree, ModuleOpensDirective javaParserNode) {}

    @Override
    public void processOther(Tree javacTree, Node javaParserNode) {}

    @Override
    public void processPackage(PackageTree javacTree, PackageDeclaration javaParserNode) {}

    @Override
    public void processParameterizedType(
            ParameterizedTypeTree javacTree, ClassOrInterfaceType javaParserNode) {}

    @Override
    public void processParenthesized(ParenthesizedTree javacTree, EnclosedExpr javaParserNode) {}

    @Override
    public void processPrimitiveType(PrimitiveTypeTree javacTree, PrimitiveType javaParserNode) {}

    @Override
    public void processPrimitiveType(PrimitiveTypeTree javacTree, VoidType javaParserNode) {}

    @Override
    public void processProvides(ProvidesTree javacTree, ModuleProvidesDirective javaParserNode) {}

    @Override
    public void processRequires(RequiresTree javacTree, ModuleRequiresDirective javaParserNode) {}

    @Override
    public void processReturn(ReturnTree javacTree, ReturnStmt javaParserNode) {}

    @Override
    public void processSwitch(SwitchTree javacTree, SwitchStmt javaParserNode) {}

    @Override
    public void processSynchronized(SynchronizedTree javacTree, SynchronizedStmt javaParserNode) {}

    @Override
    public void processThrow(ThrowTree javacTree, ThrowStmt javaParserNode) {}

    @Override
    public void processTry(TryTree javacTree, TryStmt javaParserNode) {}

    @Override
    public void processTypeCast(TypeCastTree javacTree, CastExpr javaParserNode) {}

    @Override
    public void processTypeParameter(TypeParameterTree javacTree, TypeParameter javaParserNode) {}

    @Override
    public void processUnary(UnaryTree javacTree, UnaryExpr javaParserNode) {}

    @Override
    public void processUnionType(UnionTypeTree javacTree, UnionType javaParserNode) {}

    @Override
    public void processUses(UsesTree javacTree, ModuleUsesDirective javaParserNode) {}

    @Override
    public void processVariable(VariableTree javacTree, EnumConstantDeclaration javaParserNode) {}

    @Override
    public void processVariable(VariableTree javacTree, Parameter javaParserNode) {}

    @Override
    public void processVariable(VariableTree javacTree, ReceiverParameter javaParserNode) {}

    @Override
    public void processVariable(VariableTree javacTree, VariableDeclarator javaParserNode) {}

    @Override
    public void processWhileLoop(WhileLoopTree javacTree, WhileStmt javaParserNode) {}

    @Override
    public void processWildcard(WildcardTree javacTree, WildcardType javaParserNode) {}

    @Override
    public void processCompoundAssignment(
            CompoundAssignmentTree javacTree, AssignExpr javaParserNode) {}
}
