package org.checkerframework.checker.linear;

import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.checker.linear.qual.Normal;
import com.sun.source.tree.Tree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Marks a {@link Linear} variable as {@link Unusable} once the variable is:
 * <p>
 * <ol>
 * <li value="1">the receiver of a method call</li>
 * <li value="2">TODO</li>
 * </ol>
 */
public class LinearAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    final AnnotationMirror LINEAR, UNUSABLE, NORMAL;

    public LinearAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        LINEAR = AnnotationBuilder.fromClass(elements, Linear.class);
        UNUSABLE = AnnotationBuilder.fromClass(elements, Unusable.class);
        NORMAL = AnnotationBuilder.fromClass(elements, Normal.class);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(
                Linear.class, Normal.class, Unusable.class);
    }

    @Override
    public CFTransfer createFlowTransferFunction(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new LinearFlow(analysis);
    }

    private class LinearFlow extends CFTransfer {
        private final AnnotatedTypeFactory factory;

        LinearFlow(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
            super(analysis);
            factory = analysis.getTypeFactory();
        }

        @Override
        public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode node,
                                                                      TransferInput<CFValue, CFStore> in) {
            TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);

            @Nullable Tree receiverTree = node.getTarget().getReceiver().getTree();
            if (receiverTree != null) {
                AnnotatedTypeMirror type = factory.getAnnotatedType(receiverTree);
                if (type.isAnnotatedInHierarchy(LINEAR)) {
                    return new RegularTransferResult<>(analysis.createSingleAnnotationValue(UNUSABLE,
                            result.getResultValue().getUnderlyingType()), in.getRegularStore());
                }
            }

            return result;
        }
    }

}
