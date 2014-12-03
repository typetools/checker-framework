package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.ArrayTypeNode;
import org.checkerframework.dataflow.cfg.node.AssertionErrorNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.dataflow.cfg.node.BitwiseComplementNode;
import org.checkerframework.dataflow.cfg.node.BitwiseOrNode;
import org.checkerframework.dataflow.cfg.node.BitwiseXorNode;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.CharacterLiteralNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.DoubleLiteralNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FloatLiteralNode;
import org.checkerframework.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.dataflow.cfg.node.FunctionalInterfaceNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.LongLiteralNode;
import org.checkerframework.dataflow.cfg.node.MarkerNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NullChkNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMinusNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalPlusNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.PackageNameNode;
import org.checkerframework.dataflow.cfg.node.ParameterizedTypeNode;
import org.checkerframework.dataflow.cfg.node.PrimitiveTypeNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.ShortLiteralNode;
import org.checkerframework.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.cfg.node.SuperNode;
import org.checkerframework.dataflow.cfg.node.SynchronizedNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;

import java.util.List;

/**
 * A {@link TransferFunction} for the Qual Framework.
 *
 * Extenders should override the methods of interest.
 *
 * The default for each method is to call the {@link QualTransferAdapter} which calls the corresponding
 * method in CFTransfer.
 *
 */
public class QualTransfer<Q> implements TransferFunction<QualValue<Q>, QualStore<Q>> {

    protected final QualAnalysis<Q> analysis;
    protected QualTransferAdapter<Q> adapter;

    public QualTransfer(QualAnalysis<Q> analysis) {
        this.analysis = analysis;
    }

    public void setAdapter(QualTransferAdapter<Q> adapter) {
        this.adapter = adapter;
    }

    @Override
    public QualStore<Q> initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        return adapter.superInitialStore(underlyingAST, parameters);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitShortLiteral(ShortLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitShortLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitIntegerLiteral(IntegerLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitIntegerLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitLongLiteral(LongLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitLongLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitFloatLiteral(FloatLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitFloatLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitDoubleLiteral(DoubleLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitDoubleLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitBooleanLiteral(BooleanLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitBooleanLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitCharacterLiteral(CharacterLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitCharacterLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitStringLiteral(StringLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitStringLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNullLiteral(NullLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNullLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNumericalMinus(NumericalMinusNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNumericalMinus(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNumericalPlus(NumericalPlusNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNumericalPlus(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitBitwiseComplement(BitwiseComplementNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitBitwiseComplement(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNullChk(NullChkNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNullChk(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitStringConcatenate(StringConcatenateNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitStringConcatenate(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNumericalAddition(NumericalAdditionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNumericalAddition(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNumericalSubtraction(NumericalSubtractionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNumericalSubtraction(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNumericalMultiplication(NumericalMultiplicationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNumericalMultiplication(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitIntegerDivision(IntegerDivisionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitIntegerDivision(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitFloatingDivision(FloatingDivisionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitFloatingDivision(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitIntegerRemainder(IntegerRemainderNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitIntegerRemainder(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitFloatingRemainder(FloatingRemainderNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitFloatingRemainder(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitLeftShift(LeftShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitLeftShift(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitSignedRightShift(SignedRightShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitSignedRightShift(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitUnsignedRightShift(UnsignedRightShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitUnsignedRightShift(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitBitwiseAnd(BitwiseAndNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitBitwiseAnd(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitBitwiseOr(BitwiseOrNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitBitwiseOr(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitBitwiseXor(BitwiseXorNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitBitwiseXor(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitStringConcatenateAssignment(StringConcatenateAssignmentNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitStringConcatenateAssignment(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitLessThan(LessThanNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitLessThan(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitLessThanOrEqual(LessThanOrEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitLessThanOrEqual(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitGreaterThan(GreaterThanNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitGreaterThan(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitGreaterThanOrEqual(GreaterThanOrEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitGreaterThanOrEqual(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitEqualTo(EqualToNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitEqualTo(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNotEqual(NotEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNotEqual(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitConditionalAnd(ConditionalAndNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitConditionalAnd(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitConditionalOr(ConditionalOrNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitConditionalOr(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitConditionalNot(ConditionalNotNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitConditionalNot(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitTernaryExpression(TernaryExpressionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitTernaryExpression(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitAssignment(AssignmentNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitAssignment(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitLocalVariable(LocalVariableNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitLocalVariable(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitVariableDeclaration(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitFieldAccess(FieldAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitFieldAccess(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitMethodAccess(MethodAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitMethodAccess(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitArrayAccess(ArrayAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitArrayAccess(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitImplicitThisLiteral(ImplicitThisLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitImplicitThisLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitExplicitThisLiteral(ExplicitThisLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitExplicitThisLiteral(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitSuper(SuperNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitSuper(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitReturn(ReturnNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitReturn(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitStringConversion(StringConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitStringConversion(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitNarrowingConversion(NarrowingConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitNarrowingConversion(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitWideningConversion(WideningConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitWideningConversion(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitInstanceOf(InstanceOfNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitInstanceOf(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitTypeCast(TypeCastNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitTypeCast(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitSynchronized(SynchronizedNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitSynchronized(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitAssertionError(AssertionErrorNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitAssertionError(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitThrow(ThrowNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitThrow(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitCase(CaseNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitCase(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitMethodInvocation(MethodInvocationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitMethodInvocation(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitObjectCreation(ObjectCreationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitObjectCreation(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitMemberReference(FunctionalInterfaceNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitMemberReference(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitArrayCreation(ArrayCreationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitArrayCreation(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitArrayType(ArrayTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitArrayType(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitPrimitiveType(PrimitiveTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitPrimitiveType(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitClassName(ClassNameNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitClassName(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitPackageName(PackageNameNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitPackageName(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitParameterizedType(ParameterizedTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitParameterizedType(n, p);
    }

    @Override
    public TransferResult<QualValue<Q>, QualStore<Q>> visitMarker(MarkerNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        return adapter.superVisitMarker(n, p);
    }
}
