package org.checkerframework.common.jaifinference;

import java.io.File;
import java.io.IOException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.apache.commons.io.FileUtils;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.InternalUtils;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/**
 * The purpose of this class is to allow a whole-program type inference with
 * the aid of .jaif files.
 *
 * To enable this feature on your Checker, your Transfer must override this
 * Transfer instead of CFTransfer. In case you are using the default CFTransfer
 * and do not have a Transfer implementation, your ATF must override the method
 * createTransferFunction and in the implementation it must return an instance
 * of JaifInferenceTransfer.
 */
public class JaifInferenceTransfer extends CFTransfer {

    private static final String INIT = "<init>";

    public JaifInferenceTransfer(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
        String jaifFilesFolder = analysis.getTypeFactory().getProcessingEnv().
                getOptions().get("jaifFilesFolder");
        // If a folder containing .jaif files is passed as argument, use it.
        if (jaifFilesFolder != null) {
            try {
                FileUtils.copyDirectory(new File(jaifFilesFolder),
                        new File(JaifInferenceUtils.JAIF_FILES_PATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Looks up for the field type on the .jaif file and updates
     * the store accordingly.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(FieldAccessNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> res = super.visitFieldAccess(n, p);
        CFStore store = res.getRegularStore();
        CFValue value = res.getResultValue();
        ClassSymbol ele = getClassSymbol(n, n.getReceiver());
        AnnotatedTypeMirror jaifType = JaifInferenceUtils.getFieldTypeInJaif(ele, n,
                analysis.getTypeFactory());

        if (jaifType != null && analysis.getTypeFactory().
                getQualifierHierarchy().isSubtype(jaifType.getAnnotations(),
                        value.getType().getAnnotations())) {
            value = new CFValue((CFAbstractAnalysis<CFValue, ?, ?>) analysis,
                    jaifType);
        }
        return new RegularTransferResult<>(finishValue(value, store), store);
    }

    /**
     * Looks up for the return type of the method on the .jaif file and updates
     * the store accordingly.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> res = super.visitMethodInvocation(n, in);
        CFStore thenStore = res.getThenStore();
        CFStore elseStore = res.getElseStore();
        CFValue resValue = res.getResultValue();
        ExecutableElement method = n.getTarget().getMethod();
        // How to make the following check more elegant?
        if (!n.toString().equals(INIT)) {
            ClassSymbol clazzSymbol = getClassSymbol(n.getTarget(),
                    n.getTarget().getReceiver());
            AnnotatedTypeMirror jaifType = JaifInferenceUtils.getMethodReturnTypeInJaif(
                    clazzSymbol, method, analysis.getTypeFactory());
            if (jaifType != null && analysis.getTypeFactory().
                    getQualifierHierarchy().isSubtype(jaifType.getAnnotations(),
                    resValue.getType().getAnnotations())) {
                resValue = new CFValue((CFAbstractAnalysis<CFValue, ?, ?>) analysis, jaifType);
            }
        }
        return new ConditionalTransferResult<>(finishValue(resValue, thenStore,
                elseStore), thenStore, elseStore);
    }

    /**
     * If the LHS of the assignment is a class field, this field's type will be
     * updated on the respective .jaif file.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n,
            TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> res = super.visitAssignment(n, in);
        Receiver expr = FlowExpressions.internalReprOf(analysis.getTypeFactory(),
                n.getTarget());

        if (!expr.containsUnknown() && expr instanceof FieldAccess) {
            Node lhs = n.getTarget();
            ClassSymbol clazzSymbol = getClassSymbol(lhs, ((FieldAccessNode)lhs)
                    .getReceiver());
            JaifInferenceUtils.updateFieldTypeInJaif(n.getTarget(), n.getExpression(),
                    clazzSymbol, analysis.getTypeFactory());
        }
        return res;
    }

    /**
     * Updates the return type of the method on the respective .jaif file.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitReturn(ReturnNode n,
            TransferInput<CFValue, CFStore> p) {
        ClassTree classTree = analysis.getContainingClass(n.getTree());
        if (classTree != null) {
            ClassSymbol classSymbol = (ClassSymbol) InternalUtils.symbol(classTree);
            JaifInferenceUtils.updateMethodReturnTypeInJaif(n, classSymbol,
                    analysis.getContainingMethod(n.getTree()),
                    analysis.getTypeFactory());
        }
        return super.visitReturn(n, p);
    }

    /**
     * Auxiliary method that returns the ClassSymbol of the class encapsulating
     * the node n passed as parameter.
     */
    private ClassSymbol getClassSymbol(Node n, Node receiverNode) {
        if (receiverNode instanceof ImplicitThisLiteralNode) {
            ClassTree classTree = analysis.getContainingClass(n.getTree());
            if (classTree == null) {
                return null;
            }
            return (ClassSymbol) InternalUtils.symbol(classTree);
        }
        Element symbol = InternalUtils.symbol(receiverNode.getTree());
        if (symbol instanceof ClassSymbol) {
            return (ClassSymbol) symbol;
        } else if (symbol instanceof VarSymbol) {
            return ((VarSymbol)symbol).enclClass();
        }
        return null;
    }
}
