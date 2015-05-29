package org.checkerframework.checker.i18nformatter;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;

/**
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 */
public class I18nFormatterTransfer extends CFAbstractTransfer<CFValue, CFStore, I18nFormatterTransfer> {

    protected I18nFormatterAnalysis analysis;
    protected I18nFormatterChecker checker;

    public I18nFormatterTransfer(I18nFormatterAnalysis analysis, I18nFormatterChecker checker) {
        super(analysis);
        this.analysis = analysis;
        this.checker = checker;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode node,
            TransferInput<CFValue, CFStore> in) {
        I18nFormatterAnnotatedTypeFactory atypeFactory = (I18nFormatterAnnotatedTypeFactory) analysis
                .getTypeFactory();
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);
        I18nFormatterTreeUtil tu = atypeFactory.treeUtil;

        // If hasFormat is called, make sure that the format string is annotated correctly
        if (tu.isHasFormatCall(node, atypeFactory)) {
            CFStore thenStore = result.getRegularStore();
            CFStore elseStore = thenStore.copy();
            ConditionalTransferResult<CFValue, CFStore> newResult = new ConditionalTransferResult<>(
                    result.getResultValue(), thenStore, elseStore);

            FlowExpressionContext context = FlowExpressionParseUtil.buildFlowExprContextForUse(node, atypeFactory.getContext());
            try {
                Receiver firstParam = FlowExpressionParseUtil
                        .parse("#1", context, atypeFactory.getPath(node.getTree()));
                Result<I18nConversionCategory[]> cats = tu.getHasFormatCallCategories(node);
                if (cats.value() == null) {
                    tu.failure(cats, "i18nformat.indirect.arguments");
                } else {
                    AnnotationMirror anno = atypeFactory.treeUtil.categoriesToFormatAnnotation(cats.value());
                    thenStore.insertValue(firstParam, anno);
                }
            } catch (FlowExpressionParseException e) {
                // errors are reported at declaration site
            }
            return newResult;
        }

        // If isFormat is called, annotate the format string with I18nInvalidFormat
        if (tu.isIsFormatCall(node, atypeFactory)) {
            CFStore thenStore = result.getRegularStore();
            CFStore elseStore = thenStore.copy();
            ConditionalTransferResult<CFValue, CFStore> newResult = new ConditionalTransferResult<>(
                    result.getResultValue(), thenStore, elseStore);

            FlowExpressionContext context = FlowExpressionParseUtil.buildFlowExprContextForUse(node, atypeFactory.getContext());

            try {
                Receiver firstParam = FlowExpressionParseUtil
                        .parse("#1", context, atypeFactory.getPath(node.getTree()));
                AnnotationBuilder builder = new AnnotationBuilder(tu.processingEnv, I18nInvalidFormat.class.getCanonicalName());
                // No need to set a value of @I18nInvalidFormat
                builder.setValue("value", "");
                elseStore.insertValue(firstParam, builder.build());

            } catch (FlowExpressionParseException e) {
                // errors are reported at declaration site
            }
            return newResult;
        }

        // @I18nMakeFormat that will be used to annotate ResourceBundle.getString()
        // so that when the getString() method is called, this will check if the given key exist in the translation file
        // and annotate the result string with the correct format annotation according to the corresponding key's value
        if (tu.isMakeFormatCall(node, atypeFactory)) {
            Result<I18nConversionCategory[]> cats = tu.makeFormatCallCategories(node, atypeFactory);
            if (cats.value() == null) {
                tu.failure(cats, "i18nformat.key.not.found");
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
