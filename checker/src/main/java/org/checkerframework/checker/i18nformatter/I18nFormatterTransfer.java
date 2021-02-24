package org.checkerframework.checker.i18nformatter;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * The transfer function for the Internationalization Format String Checker.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
public class I18nFormatterTransfer extends CFTransfer {

    public I18nFormatterTransfer(CFAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<CFValue, CFStore> in) {
        I18nFormatterAnnotatedTypeFactory atypeFactory =
                (I18nFormatterAnnotatedTypeFactory) analysis.getTypeFactory();
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);
        I18nFormatterTreeUtil tu = atypeFactory.treeUtil;

        // If hasFormat is called, make sure that the format string is annotated correctly
        if (tu.isHasFormatCall(node, atypeFactory)) {
            CFStore thenStore = result.getRegularStore();
            CFStore elseStore = thenStore.copy();
            ConditionalTransferResult<CFValue, CFStore> newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
            Result<I18nConversionCategory[]> cats = tu.getHasFormatCallCategories(node);
            if (cats.value() == null) {
                tu.failure(cats, "i18nformat.indirect.arguments");
            } else {
                JavaExpression firstParam = JavaExpression.fromNode(node.getArgument(0));
                AnnotationMirror anno =
                        atypeFactory.treeUtil.categoriesToFormatAnnotation(cats.value());
                thenStore.insertValue(firstParam, anno);
            }
            return newResult;
        }

        // If isFormat is called, annotate the format string with I18nInvalidFormat
        if (tu.isIsFormatCall(node, atypeFactory)) {
            CFStore thenStore = result.getRegularStore();
            CFStore elseStore = thenStore.copy();
            ConditionalTransferResult<CFValue, CFStore> newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
            JavaExpression firstParam = JavaExpression.fromNode(node.getArgument(0));
            AnnotationBuilder builder =
                    new AnnotationBuilder(tu.processingEnv, I18nInvalidFormat.class);
            // No need to set a value of @I18nInvalidFormat
            builder.setValue("value", "");
            elseStore.insertValue(firstParam, builder.build());
            return newResult;
        }

        // @I18nMakeFormat that will be used to annotate ResourceBundle.getString() so that when the
        // getString() method is called, this will check if the given key exist in the translation
        // file and annotate the result string with the correct format annotation according to the
        // corresponding key's value
        if (tu.isMakeFormatCall(node, atypeFactory)) {
            Result<I18nConversionCategory[]> cats = tu.makeFormatCallCategories(node, atypeFactory);
            if (cats.value() == null) {
                tu.failure(cats, "i18nformat.key.not.found");
            } else {
                AnnotationMirror anno =
                        atypeFactory.treeUtil.categoriesToFormatAnnotation(cats.value());
                CFValue newResultValue =
                        analysis.createSingleAnnotationValue(
                                anno, result.getResultValue().getUnderlyingType());
                return new RegularTransferResult<>(newResultValue, result.getRegularStore());
            }
        }

        return result;
    }
}
