package org.checkerframework.checker.i18nformatter;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

/**
 *
 * Needed for flow analysis to implement {@link I18nFormatUtil#hasFormat},
 * {@link I18nFormatUtil#isFormat}, and MakeFormat in {@link I18nFormatterTransfer}.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 */
public class I18nFormatterAnalysis extends CFAbstractAnalysis<CFValue, CFStore, I18nFormatterTransfer> {
    public I18nFormatterAnalysis(BaseTypeChecker checker, I18nFormatterAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public I18nFormatterTransfer createTransferFunction() {
        return new I18nFormatterTransfer(this, (I18nFormatterChecker) checker);
    }

    @Override
    public CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return defaultCreateAbstractValue(this, type);
    }
}
