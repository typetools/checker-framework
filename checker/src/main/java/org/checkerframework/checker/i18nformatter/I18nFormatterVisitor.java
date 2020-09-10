package org.checkerframework.checker.i18nformatter;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.i18nformatter.I18nFormatterTreeUtil.FormatType;
import org.checkerframework.checker.i18nformatter.I18nFormatterTreeUtil.I18nFormatCall;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Whenever a method with {@link I18nFormatFor} annotation is invoked, it will perform the format
 * string verification.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
public class I18nFormatterVisitor extends BaseTypeVisitor<I18nFormatterAnnotatedTypeFactory> {

    public I18nFormatterVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        MethodInvocationNode nodeNode =
                atypeFactory.getFirstNodeOfKindForTree(tree, MethodInvocationNode.class);
        I18nFormatterTreeUtil tu = atypeFactory.treeUtil;
        I18nFormatCall fc = tu.createFormatForCall(tree, nodeNode, atypeFactory);
        if (fc != null) {
            checkInvocationFormatFor(fc);
            return p;
        } else {
            return super.visitMethodInvocation(tree, p);
        }
    }

    private void checkInvocationFormatFor(I18nFormatCall fc) {
        I18nFormatterTreeUtil tu = atypeFactory.treeUtil;
        Result<FormatType> type = fc.getFormatType();

        switch (type.value()) {
            case I18NINVALID:
                tu.failure(type, "i18nformat.string.invalid", fc.getInvalidError());
                break;
            case I18NFORMATFOR:
                if (!fc.isValidFormatForInvocation()) {
                    Result<FormatType> failureType = fc.getInvalidInvocationType();
                    tu.failure(failureType, "i18nformat.invalid.formatfor");
                }
                break;
            case I18NFORMAT:
                Result<InvocationType> invc = fc.getInvocationType();
                I18nConversionCategory[] formatCats = fc.getFormatCategories();
                switch (invc.value()) {
                    case VARARG:
                        Result<TypeMirror>[] paramTypes = fc.getParamTypes();
                        int paraml = paramTypes.length;
                        int formatl = formatCats.length;

                        // For assignments, i18nformat.missing.arguments and
                        // i18nformat.excess.arguments are issued
                        // from commonAssignmentCheck.
                        if (paraml < formatl) {
                            tu.warning(invc, "i18nformat.missing.arguments", formatl, paraml);
                        }
                        if (paraml > formatl) {
                            tu.warning(invc, "i18nformat.excess.arguments", formatl, paraml);
                        }
                        for (int i = 0; i < formatl && i < paraml; ++i) {
                            I18nConversionCategory formatCat = formatCats[i];
                            Result<TypeMirror> param = paramTypes[i];
                            TypeMirror paramType = param.value();
                            switch (formatCat) {
                                case UNUSED:
                                    tu.warning(param, "i18nformat.argument.unused", " " + (1 + i));
                                    break;
                                case GENERAL:
                                    break;
                                default:
                                    if (!fc.isValidParameter(formatCat, paramType)) {
                                        ExecutableElement method =
                                                TreeUtils.elementFromUse(fc.getTree());
                                        Name methodName = method.getSimpleName();
                                        tu.failure(
                                                param,
                                                "argument.type.incompatible",
                                                "", // parameter name is not useful
                                                methodName,
                                                paramType,
                                                formatCat);
                                    }
                            }
                        }
                        break;
                    case NULLARRAY:
                        // fall-through
                    case ARRAY:
                        for (I18nConversionCategory cat : formatCats) {
                            if (cat == I18nConversionCategory.UNUSED) {
                                tu.warning(invc, "i18nformat.argument.unused", "");
                            }
                        }
                        tu.warning(invc, "i18nformat.indirect.arguments");
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        AnnotationMirror rhs = valueType.getAnnotationInHierarchy(atypeFactory.I18NUNKNOWNFORMAT);
        AnnotationMirror lhs = varType.getAnnotationInHierarchy(atypeFactory.I18NUNKNOWNFORMAT);

        // i18nformat.missing.arguments and i18nformat.excess.arguments are issued here for
        // assignments.
        // For method calls, they are issued in checkInvocationFormatFor.
        if (rhs != null
                && lhs != null
                && AnnotationUtils.areSameByName(
                        rhs, I18nFormatterAnnotatedTypeFactory.I18NFORMAT_NAME)
                && AnnotationUtils.areSameByName(
                        lhs, I18nFormatterAnnotatedTypeFactory.I18NFORMAT_NAME)) {
            I18nConversionCategory[] rhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(rhs);
            I18nConversionCategory[] lhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(lhs);

            if (rhsArgTypes.length < lhsArgTypes.length) {
                // From the manual:
                // It is legal to use a format string with fewer format specifiers
                // than required, but a warning is issued.
                checker.reportWarning(
                        valueTree,
                        "i18nformat.missing.arguments",
                        varType.toString(),
                        valueType.toString());
            } else if (rhsArgTypes.length > lhsArgTypes.length) {
                // Since it is known that too many conversion categories were provided,
                // issue a more specific error message to that effect than
                // assignment.type.incompatible.
                checker.reportError(
                        valueTree,
                        "i18nformat.excess.arguments",
                        varType.toString(),
                        valueType.toString());
            }
        }

        // By calling super.commonAssignmentCheck last, any i18nformat.excess.arguments message
        // issued for a given line of code will take precedence over the
        // assignment.type.incompatible
        // issued by super.commonAssignmentCheck.
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
    }
}
