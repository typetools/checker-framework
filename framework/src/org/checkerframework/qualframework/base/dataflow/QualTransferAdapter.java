package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
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
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QualTransferAdapter adapts the {@link CFTransfer} to a {@link QualTransfer}.
 *
 * It does this by converting TransferInputs and TransferResults backed
 * by CFValue and CFStores to ones backed by QualValue and QualStores.
 *
 */
public class QualTransferAdapter<Q> extends CFTransfer {

    private final QualTransfer<Q> underlying;
    private final QualAnalysis<Q> qualAnalysis;

    public QualTransferAdapter(
            QualTransfer<Q> underlying,
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis,
            QualAnalysis<Q> qualAnalysis) {

        super(analysis);
        underlying.setAdapter(this);
        this.underlying = underlying;
        this.qualAnalysis = qualAnalysis;
    }

    /**
     * Convert a TransferInput backed by CFValue and CFStores to one backed by QualValue and QualStores.
     */
    public TransferInput<QualValue<Q>, QualStore<Q>> convertCfToQualInput(TransferInput<CFValue, CFStore> transferInput) {
        if (transferInput.containsTwoStores()) {
            return new TransferInput<>(transferInput.getNode(), qualAnalysis,
                    new QualStore<Q>(qualAnalysis, transferInput.getThenStore()),
                    new QualStore<Q>(qualAnalysis, transferInput.getElseStore()));
        } else {
            return new TransferInput<>(transferInput.getNode(), qualAnalysis,
                    new QualStore<Q>(qualAnalysis, transferInput.getThenStore()));
        }
    }

    /**
     * Convert a TransferInput backed by QualValue and QualStores to one backed by CFValue and CFStores.
     */
    public TransferInput<CFValue, CFStore> convertQualToCfInput(TransferInput<QualValue<Q>, QualStore<Q>> transferInput) {
        if (transferInput.containsTwoStores()) {
            return new TransferInput<>(transferInput.getNode(), analysis,
                    transferInput.getThenStore().getUnderlyingStore(), transferInput.getElseStore().getUnderlyingStore());
        } else {
            return new TransferInput<>(transferInput.getNode(), analysis,
                    transferInput.getRegularStore().getUnderlyingStore());
        }
    }

    /**
     * Convert a TransferResult backed by QualValue and QualStores to one backed by CFValue and CFStores.
     */
    public TransferResult<CFValue, CFStore> convertQualToCfResult(TransferResult<QualValue<Q>,
            QualStore<Q>> transferResult) {

        Map<TypeMirror, QualStore<Q>> exeStores = transferResult.getExceptionalStores();
        Map<TypeMirror, CFStore> convertedExeStores = new HashMap<>();
        if (exeStores != null) {
            for (Map.Entry<TypeMirror, QualStore<Q>> entry : exeStores.entrySet()) {
                convertedExeStores.put(entry.getKey(), entry.getValue().getUnderlyingStore());
            }
        }

        CFValue resultValue = null;
        if (transferResult.getResultValue() != null) {
            resultValue = analysis.createAbstractValue(
                    qualAnalysis.getConverter().getAnnotatedType(transferResult.getResultValue().getType()));
        }

        if (transferResult.containsTwoStores()) {
            return new ConditionalTransferResult<>(resultValue,
                    transferResult.getThenStore().getUnderlyingStore(),
                    transferResult.getElseStore().getUnderlyingStore(),
                    convertedExeStores, transferResult.storeChanged());
        } else {
            return new RegularTransferResult<>(resultValue,
                    transferResult.getRegularStore().getUnderlyingStore(),
                    convertedExeStores, transferResult.storeChanged());
        }
    }

    /**
     * Convert a TransferResult backed by CFValue and CFStores to one backed by QualValue and QualStores.
     */
    public  TransferResult<QualValue<Q>, QualStore<Q>> convertCfToQualResult(TransferResult<CFValue, CFStore> transferResult) {
        Map<TypeMirror, CFStore> exeStores = transferResult.getExceptionalStores();
        Map<TypeMirror, QualStore<Q>> convertedExeStores = new HashMap<>();
        if (exeStores != null) {
            for (Map.Entry<TypeMirror, CFStore> entry : exeStores.entrySet()) {
                convertedExeStores.put(entry.getKey(), new QualStore<Q>(qualAnalysis, entry.getValue()));
            }
        }

        QualValue<Q> resultValue = null;
        if (transferResult.getResultValue() != null) {
            resultValue = qualAnalysis.createAbstractValue(
                    qualAnalysis.getConverter().getQualifiedType(transferResult.getResultValue().getType()));
        }

        if (transferResult.containsTwoStores()) {
            return new ConditionalTransferResult<>(resultValue,
                    new QualStore<Q>(qualAnalysis, transferResult.getThenStore()),
                    new QualStore<Q>(qualAnalysis, transferResult.getElseStore()),
                    convertedExeStores, transferResult.storeChanged());
        } else {
            return new RegularTransferResult<>(resultValue,
                    new QualStore<Q>(qualAnalysis, transferResult.getRegularStore()),
                    convertedExeStores, transferResult.storeChanged());
        }
    }

    public QualStore<Q> superInitialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        CFStore initialStore = super.initialStore(underlyingAST, parameters);
        return new QualStore<>(qualAnalysis, initialStore);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitShortLiteral(ShortLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitShortLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitShortLiteral(ShortLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitShortLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerLiteral(IntegerLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitIntegerLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitIntegerLiteral(IntegerLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitIntegerLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLongLiteral(LongLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitLongLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitLongLiteral(LongLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitLongLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatLiteral(FloatLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitFloatLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitFloatLiteral(FloatLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitFloatLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitDoubleLiteral(DoubleLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitDoubleLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitDoubleLiteral(DoubleLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitDoubleLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBooleanLiteral(BooleanLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitBooleanLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitBooleanLiteral(BooleanLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitBooleanLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitCharacterLiteral(CharacterLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitCharacterLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitCharacterLiteral(CharacterLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitCharacterLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringLiteral(StringLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitStringLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitStringLiteral(StringLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNullLiteral(NullLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNullLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNullLiteral(NullLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNullLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Unary operations
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMinus(NumericalMinusNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNumericalMinus(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNumericalMinus(NumericalMinusNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalMinus(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalPlus(NumericalPlusNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNumericalPlus(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNumericalPlus(NumericalPlusNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalPlus(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseComplement(BitwiseComplementNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitBitwiseComplement(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitBitwiseComplement(BitwiseComplementNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitBitwiseComplement(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNullChk(NullChkNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNullChk(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNullChk(NullChkNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNullChk(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Binary operations
    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenate(StringConcatenateNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitStringConcatenate(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitStringConcatenate(StringConcatenateNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenate(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(NumericalAdditionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNumericalAddition(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNumericalAddition(NumericalAdditionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalAddition(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNumericalSubtraction(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNumericalSubtraction(NumericalSubtractionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalSubtraction(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMultiplication(NumericalMultiplicationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNumericalMultiplication(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNumericalMultiplication(NumericalMultiplicationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalMultiplication(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerDivision(IntegerDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitIntegerDivision(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitIntegerDivision(IntegerDivisionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitIntegerDivision(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingDivision(FloatingDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitFloatingDivision(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitFloatingDivision(FloatingDivisionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitFloatingDivision(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerRemainder(IntegerRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitIntegerRemainder(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitIntegerRemainder(IntegerRemainderNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitIntegerRemainder(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingRemainder(FloatingRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitFloatingRemainder(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitFloatingRemainder(FloatingRemainderNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitFloatingRemainder(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLeftShift(LeftShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitLeftShift(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitLeftShift(LeftShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitLeftShift(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSignedRightShift(SignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitSignedRightShift(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitSignedRightShift(SignedRightShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitSignedRightShift(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitUnsignedRightShift(UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitUnsignedRightShift(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitUnsignedRightShift(UnsignedRightShiftNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitUnsignedRightShift(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseAnd(BitwiseAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitBitwiseAnd(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitBitwiseAnd(BitwiseAndNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitBitwiseAnd(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseOr(BitwiseOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitBitwiseOr(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitBitwiseOr(BitwiseOrNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitBitwiseOr(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseXor(BitwiseXorNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitBitwiseXor(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitBitwiseXor(BitwiseXorNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitBitwiseXor(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Compound assignments
    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitStringConcatenateAssignment(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitStringConcatenateAssignment(StringConcatenateAssignmentNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenateAssignment(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Comparison operations
    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(LessThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitLessThan(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitLessThan(LessThanNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitLessThan(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(LessThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitLessThanOrEqual(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitLessThanOrEqual(LessThanOrEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(GreaterThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitGreaterThan(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitGreaterThan(GreaterThanNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitGreaterThanOrEqual(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitGreaterThanOrEqual(GreaterThanOrEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(EqualToNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitEqualTo(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitEqualTo(EqualToNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitEqualTo(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(NotEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNotEqual(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNotEqual(NotEqualNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNotEqual(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Conditional operations
    @Override
    public TransferResult<CFValue, CFStore> visitConditionalAnd(ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitConditionalAnd(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitConditionalAnd(ConditionalAndNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitConditionalAnd(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalOr(ConditionalOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitConditionalOr(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitConditionalOr(ConditionalOrNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitConditionalOr(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalNot(ConditionalNotNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitConditionalNot(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitConditionalNot(ConditionalNotNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitConditionalNot(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitTernaryExpression(TernaryExpressionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitTernaryExpression(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitTernaryExpression(TernaryExpressionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitAssignment(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitAssignment(AssignmentNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLocalVariable(LocalVariableNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitLocalVariable(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitLocalVariable(LocalVariableNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitLocalVariable(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitVariableDeclaration(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitVariableDeclaration(VariableDeclarationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitVariableDeclaration(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(FieldAccessNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitFieldAccess(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitFieldAccess(FieldAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitFieldAccess(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodAccess(MethodAccessNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitMethodAccess(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitMethodAccess(MethodAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitMethodAccess(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitArrayAccess(ArrayAccessNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitArrayAccess(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitArrayAccess(ArrayAccessNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitArrayAccess(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitImplicitThisLiteral(ImplicitThisLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitImplicitThisLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitImplicitThisLiteral(ImplicitThisLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitImplicitThisLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitExplicitThisLiteral(ExplicitThisLiteralNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitExplicitThisLiteral(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitExplicitThisLiteral(ExplicitThisLiteralNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitExplicitThisLiteral(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSuper(SuperNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitSuper(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitSuper(SuperNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitSuper(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitReturn(ReturnNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitReturn(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    };

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitReturn(ReturnNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitReturn(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringConversion(StringConversionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitStringConversion(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    };

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitStringConversion(StringConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConversion(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNarrowingConversion(NarrowingConversionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitNarrowingConversion(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    };

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitNarrowingConversion(NarrowingConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitNarrowingConversion(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitWideningConversion(WideningConversionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitWideningConversion(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    };

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitWideningConversion(WideningConversionNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitWideningConversion(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitInstanceOf(InstanceOfNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitInstanceOf(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitInstanceOf(InstanceOfNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitInstanceOf(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitTypeCast(TypeCastNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitTypeCast(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitTypeCast(TypeCastNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitTypeCast(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Statements
    @Override
    public TransferResult<CFValue, CFStore> visitAssertionError(AssertionErrorNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitAssertionError(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitAssertionError(AssertionErrorNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitAssertionError(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSynchronized(SynchronizedNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitSynchronized(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitSynchronized(SynchronizedNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitSynchronized(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitThrow(ThrowNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitThrow(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitThrow(ThrowNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitThrow(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Cases
    @Override
    public TransferResult<CFValue, CFStore> visitCase(CaseNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitCase(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitCase(CaseNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitCase(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Method and constructor invocations
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitMethodInvocation(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitMethodInvocation(MethodInvocationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitObjectCreation(ObjectCreationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitObjectCreation(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitObjectCreation(ObjectCreationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitObjectCreation(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMemberReference(FunctionalInterfaceNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitMemberReference(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitMemberReference(FunctionalInterfaceNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitMemberReference(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitArrayCreation(ArrayCreationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitArrayCreation(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitArrayCreation(ArrayCreationNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitArrayCreation(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Type, package and class names
    @Override
    public TransferResult<CFValue, CFStore> visitArrayType(ArrayTypeNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitArrayType(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitArrayType(ArrayTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitArrayType(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitPrimitiveType(PrimitiveTypeNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitPrimitiveType(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitPrimitiveType(PrimitiveTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitPrimitiveType(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitClassName(ClassNameNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitClassName(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitClassName(ClassNameNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitClassName(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitPackageName(PackageNameNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitPackageName(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitPackageName(PackageNameNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitPackageName(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Parameterized types
    @Override
    public TransferResult<CFValue, CFStore> visitParameterizedType(ParameterizedTypeNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitParameterizedType(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitParameterizedType(ParameterizedTypeNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitParameterizedType(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }

    // Marker nodes
    @Override
    public TransferResult<CFValue, CFStore> visitMarker(MarkerNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<QualValue<Q>, QualStore<Q>> result = underlying.visitMarker(n, convertCfToQualInput(p));
        return convertQualToCfResult(result);
    }

    public TransferResult<QualValue<Q>, QualStore<Q>> superVisitMarker(MarkerNode n, TransferInput<QualValue<Q>, QualStore<Q>> p) {
        TransferResult<CFValue, CFStore> result = super.visitMarker(n, convertQualToCfInput(p));
        return convertCfToQualResult(result);
    }
}
