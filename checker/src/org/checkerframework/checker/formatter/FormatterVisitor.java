package org.checkerframework.checker.formatter;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.FormatCall;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Whenever a format method invocation is found in the syntax tree, the following checks happen,
 * read the code, seriously! (otherwise see manual 12.2)
 *
 * @author Konstantin Weitz
 */
public class FormatterVisitor extends BaseTypeVisitor<FormatterAnnotatedTypeFactory> {
    public FormatterVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        FormatterTreeUtil tu = atypeFactory.treeUtil;
        if (tu.isFormatCall(node, atypeFactory)) {
            FormatCall fc = atypeFactory.treeUtil.new FormatCall(node, atypeFactory);

            Result<String> sat = fc.isIllegalFormat();
            if (sat.value() != null) {
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
                            // For assignments, format.missing.arguments is issued
                            // from commonAssignmentCheck.
                            // II.1
                            tu.failure(invc, "format.missing.arguments", formatl, paraml);
                        } else {
                            if (paraml > formatl) {
                                // II.2
                                tu.warning(invc, "format.excess.arguments", formatl, paraml);
                            }
                            for (int i = 0; i < formatl; ++i) {
                                ConversionCategory formatCat = formatCats[i];
                                Result<TypeMirror> param = paramTypes[i];
                                TypeMirror paramType = param.value();

                                switch (formatCat) {
                                    case UNUSED:
                                        // I.2
                                        tu.warning(param, "format.argument.unused", " " + (1 + i));
                                        break;
                                    case NULL:
                                        // I.3
                                        tu.failure(param, "format.specifier.null", " " + (1 + i));
                                        break;
                                    case GENERAL:
                                        break;
                                    default:
                                        if (!fc.isValidParameter(formatCat, paramType)) {
                                            // II.3
                                            tu.failure(
                                                    param,
                                                    "argument.type.incompatible",
                                                    paramType,
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
                            if (cat == ConversionCategory.NULL) {
                                // I.3
                                tu.failure(invc, "format.specifier.null", "");
                            }
                            if (cat == ConversionCategory.UNUSED) {
                                // I.2
                                tu.warning(invc, "format.argument.unused", "");
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

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey) {
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);

        AnnotationMirror rhs = valueType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);
        AnnotationMirror lhs = varType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);

        // From the manual:
        // It is legal to use a format string with fewer format specifiers
        // than required, but a warning is issued.
        // The format.missing.arguments warning is issued here for assignments.
        // For method calls, it is issued in visitMethodInvocation.
        if (AnnotationUtils.areSameIgnoringValues(rhs, atypeFactory.FORMAT)
                && AnnotationUtils.areSameIgnoringValues(lhs, atypeFactory.FORMAT)) {
            ConversionCategory[] rhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(rhs);
            ConversionCategory[] lhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(lhs);

            if (rhsArgTypes.length < lhsArgTypes.length) {
                checker.report(
                        org.checkerframework.framework.source.Result.warning(
                                "format.missing.arguments",
                                varType.toString(),
                                valueType.toString()),
                        valueTree);
            }
        }
    }
}
