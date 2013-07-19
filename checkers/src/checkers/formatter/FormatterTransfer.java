package checkers.formatter;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.CFAbstractTransfer;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.formatter.FormatterTreeUtil.Result;
import checkers.formatter.quals.ConversionCategory;
import dataflow.analysis.RegularTransferResult;
import dataflow.analysis.TransferInput;
import dataflow.analysis.TransferResult;
import dataflow.cfg.node.MethodInvocationNode;

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
                .getFactory();
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);
        FormatterTreeUtil tu = checker.treeUtil;

        if (tu.isAsFormatCall(node, atypeFactory)) {
            Result<ConversionCategory[]> cats = tu.asFormatCallCategories(node);
            if (cats.value() == null) {
                tu.failure(cats, "format.asformat.indirect.arguments");
            } else {
                AnnotationMirror anno = checker.treeUtil.categoriesToFormatAnnotation(cats.value());
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
