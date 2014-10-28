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
import org.checkerframework.dataflow.cfg.node.Node;
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
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;
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
 * Created by mcarthur on 10/22/14.
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

    public TransferInput<QualValue<Q>, QualStore<Q>> convertCfToQualInput(TransferInput<CFValue, CFStore> p) {
        if (p.containsTwoStores()) {
            return new QualTransferInput<Q>(qualAnalysis, p.getNode(), analysis,
                    new QualStore<Q>(p.getThenStore()), new QualStore<Q>(p.getElseStore()), qualAnalysis.getConverter());
        } else {
            return new QualTransferInput<Q>(qualAnalysis, p.getNode(), analysis,
                    new QualStore<Q>(p.getThenStore()), qualAnalysis.getConverter());
        }
    }

    public TransferInput<CFValue, CFStore> convertQualToCfInput(TransferInput<QualValue<Q>, QualStore<Q>> p) {
        if (p.containsTwoStores()) {
            return new TransferInput<>(p.getNode(), analysis, p.getThenStore().getUnderlyingStore(), p.getElseStore().getUnderlyingStore());
        } else {
            return new TransferInput<>(p.getNode(), analysis, p.getRegularStore().getUnderlyingStore());
        }
    }

    public TransferResult<CFValue, CFStore> convertQualToCfResult(TransferResult<QualValue<Q>, QualStore<Q>> p) {

        Map<TypeMirror, QualStore<Q>> exeStores = p.getExceptionalStores();
        Map<TypeMirror, CFStore> convertedExeStores = new HashMap<>();
        if (exeStores != null) {
            for (Map.Entry<TypeMirror, QualStore<Q>> entry : exeStores.entrySet()) {
                convertedExeStores.put(entry.getKey(), entry.getValue().getUnderlyingStore());
            }
        }

        if (p.containsTwoStores()) {
            return new ConditionalTransferResult<>(analysis.createAbstractValue(qualAnalysis.getConverter().getAnnotatedType(p.getResultValue().getType())),
                    p.getThenStore().getUnderlyingStore(), p.getElseStore().getUnderlyingStore(), convertedExeStores, p.storeChanged());
        } else {
            return new RegularTransferResult<>(analysis.createAbstractValue(qualAnalysis.getConverter().getAnnotatedType(p.getResultValue().getType())),
                    p.getRegularStore().getUnderlyingStore(), convertedExeStores, p.storeChanged());
        }
    }

    public  TransferResult<QualValue<Q>, QualStore<Q>> convertCfToQualResult(TransferResult<CFValue, CFStore> p) {
        Map<TypeMirror, CFStore> exeStores = p.getExceptionalStores();
        Map<TypeMirror, QualStore<Q>> convertedExeStores = new HashMap<>();
        if (exeStores != null) {
            for (Map.Entry<TypeMirror, CFStore> entry : exeStores.entrySet()) {
                convertedExeStores.put(entry.getKey(), new QualStore<Q>(entry.getValue()));
            }
        }

        if (p.containsTwoStores()) {
            return new ConditionalTransferResult<>(qualAnalysis.createAbstractValue(qualAnalysis.getConverter().getQualifiedType(p.getResultValue().getType())),
                    new QualStore<Q>(p.getThenStore()), new QualStore<Q>(p.getElseStore()), convertedExeStores, p.storeChanged());
        } else {
            return new RegularTransferResult<>(qualAnalysis.createAbstractValue(qualAnalysis.getConverter().getQualifiedType(p.getResultValue().getType())),
                    new QualStore<Q>(p.getRegularStore()), convertedExeStores, p.storeChanged());
        }
    }

    public QualStore<Q> superInitialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        CFStore initialStore = super.initialStore(underlyingAST, parameters);
        return new QualStore<>(initialStore);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNode(Node n, TransferInput<CFValue, CFStore> p) {
        return super.visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitValueLiteral(ValueLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
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
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatLiteral(FloatLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitDoubleLiteral(DoubleLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBooleanLiteral(BooleanLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitCharacterLiteral(CharacterLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringLiteral(StringLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNullLiteral(NullLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitValueLiteral(n, p);
    }

    // Unary operations
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMinus(NumericalMinusNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalPlus(NumericalPlusNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseComplement(BitwiseComplementNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNullChk(NullChkNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Binary operations
    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenate(StringConcatenateNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(NumericalAdditionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMultiplication(NumericalMultiplicationNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerDivision(IntegerDivisionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingDivision(FloatingDivisionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerRemainder(IntegerRemainderNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingRemainder(FloatingRemainderNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLeftShift(LeftShiftNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSignedRightShift(SignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitUnsignedRightShift(UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseAnd(BitwiseAndNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseOr(BitwiseOrNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseXor(BitwiseXorNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Compound assignments
    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Comparison operations
    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(LessThanNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(LessThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(GreaterThanNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(EqualToNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(NotEqualNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Conditional operations
    @Override
    public TransferResult<CFValue, CFStore> visitConditionalAnd(ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalOr(ConditionalOrNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalNot(ConditionalNotNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitTernaryExpression(TernaryExpressionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLocalVariable(LocalVariableNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(FieldAccessNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodAccess(MethodAccessNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitArrayAccess(ArrayAccessNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    public TransferResult<CFValue, CFStore> visitThisLiteral(ThisLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitImplicitThisLiteral(ImplicitThisLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitThisLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitExplicitThisLiteral(ExplicitThisLiteralNode n, TransferInput<CFValue, CFStore> p) {
        return visitThisLiteral(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSuper(SuperNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitReturn(ReturnNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    };

    @Override
    public TransferResult<CFValue, CFStore> visitStringConversion(StringConversionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    };

    @Override
    public TransferResult<CFValue, CFStore> visitNarrowingConversion(NarrowingConversionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    };

    @Override
    public TransferResult<CFValue, CFStore> visitWideningConversion(WideningConversionNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    };

    @Override
    public TransferResult<CFValue, CFStore> visitInstanceOf(InstanceOfNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitTypeCast(TypeCastNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Statements
    @Override
    public TransferResult<CFValue, CFStore> visitAssertionError(AssertionErrorNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSynchronized(SynchronizedNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitThrow(ThrowNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Cases
    @Override
    public TransferResult<CFValue, CFStore> visitCase(CaseNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Method and constructor invocations
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitObjectCreation(ObjectCreationNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMemberReference(FunctionalInterfaceNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitArrayCreation(ArrayCreationNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Type, package and class names
    @Override
    public TransferResult<CFValue, CFStore> visitArrayType(ArrayTypeNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitPrimitiveType(PrimitiveTypeNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitClassName(ClassNameNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitPackageName(PackageNameNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Parameterized types
    @Override
    public TransferResult<CFValue, CFStore> visitParameterizedType(ParameterizedTypeNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }

    // Marker nodes
    @Override
    public TransferResult<CFValue, CFStore> visitMarker(MarkerNode n, TransferInput<CFValue, CFStore> p) {
        return visitNode(n, p);
    }
    
}
