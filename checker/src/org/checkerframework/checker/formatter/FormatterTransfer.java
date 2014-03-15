package org.checkerframework.checker.formatter;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

public class FormatterTransfer extends
        CFAbstractTransfer<CFValue, CFStore, FormatterTransfer> {

    protected FormatterAnalysis analysis;
    protected FormatterChecker checker;

    public FormatterTransfer(FormatterAnalysis analysis, FormatterChecker checker) {
        super(analysis);
        this.analysis = analysis;
        this.checker = checker;
    }

    /**
     * Makes it so that the {@link FormatUtil#asFormat} method returns
     * a correctly annotated String.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<CFValue, CFStore> in) {
        FormatterAnnotatedTypeFactory atypeFactory = (FormatterAnnotatedTypeFactory) analysis
                .getTypeFactory();
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);
        FormatterTreeUtil tu = atypeFactory.treeUtil;

        if (tu.isAsFormatCall(node, atypeFactory)) {
            Result<ConversionCategory[]> cats = tu.asFormatCallCategories(node);
            if (cats.value() == null) {
                tu.failure(cats, "format.asformat.indirect.arguments");
            } else {
                AnnotationMirror anno = atypeFactory.treeUtil.categoriesToFormatAnnotation(cats.value());
                CFValue newResultValue = analysis
                        .createSingleAnnotationValue(anno,
                                result.getResultValue().getType()
                                        .getUnderlyingType());
                return new RegularTransferResult<>(newResultValue,
                        result.getRegularStore());
            }
        }

        return result;
    };
}
