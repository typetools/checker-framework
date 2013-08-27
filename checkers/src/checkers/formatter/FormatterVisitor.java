package checkers.formatter;

import checkers.basetype.BaseTypeVisitor;
import checkers.formatter.FormatterTreeUtil.FormatCall;
import checkers.formatter.FormatterTreeUtil.InvocationType;
import checkers.formatter.FormatterTreeUtil.Result;
import checkers.formatter.quals.ConversionCategory;

import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Whenever a format method invocation is found in the syntax tree,
 * the following checks happen, read the code, seriously! (otherwise see manual 12.2)
 *
 * @author Konstantin Weitz
 */
public class FormatterVisitor extends BaseTypeVisitor<FormatterChecker, FormatterAnnotatedTypeFactory> {
    public FormatterVisitor(FormatterChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        FormatterTreeUtil tu = checker.treeUtil;
        if (tu.isFormatCall(node, atypeFactory)) {
            FormatCall fc = checker.treeUtil.new FormatCall(node, atypeFactory);

            Result<String> sat = fc.isIllegalFormat();
            if (sat.value() != null){
                // I.1
                tu.failure(sat, "format.string.invalid", sat.value());
            } else {
                Result<InvocationType> invc = fc.getInvocationType();
                ConversionCategory[] formatCats = fc.getFormatCategories();
                switch (invc.value()) {
                case VARARG:
                    Result<TypeMirror>[] paramTypes = fc.getParamTypes();
                    int paraml = paramTypes.length;
                    int formatl = formatCats.length;
                    if (paraml < formatl) {
                        // II.1
                        tu.failure(invc, "format.missing.arguments", formatl,paraml);
                    } else {
                        if (paraml > formatl) {
                            // II.2
                            tu.warning(invc, "format.excess.arguments", formatl,paraml);
                        }
                        for (int i=0; i<formatl; ++i) {
                            ConversionCategory formatCat = formatCats[i];
                            Result<TypeMirror> param = paramTypes[i];
                            TypeMirror paramType = param.value();

                            switch (formatCat) {
                            case UNUSED:
                                // I.2
                                tu.warning(param, "format.argument.unused");
                                break;
                            case NULL:
                                // I.3
                                tu.failure(param, "format.argument.null");
                                break;
                            case GENERAL:
                                break;
                            default:
                                if (!fc.isValidParameter(formatCat,paramType)) {
                                    // II.3
                                    tu.failure(param, "argument.type.incompatible", paramType,
                                            formatCat);
                                }
                                break;
                            }
                        }
                    }
                    break;
                case NULLARRAY:
                    /* continue */
                case ARRAY:
                    for (ConversionCategory cat : formatCats) {
                        if (cat == ConversionCategory.NULL){
                            // I.3
                            tu.failure(invc, "format.argument.null");
                        }
                        if (cat == ConversionCategory.UNUSED){
                            // I.2
                            tu.warning(invc, "format.argument.unused");
                        }
                    }
                    // III
                    tu.warning(invc, "format.indirect.arguments");
                    break;
                }
            }
        }
        return super.visitMethodInvocation(node, p);
    }
}
