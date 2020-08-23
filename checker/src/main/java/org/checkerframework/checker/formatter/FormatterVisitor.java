package org.checkerframework.checker.formatter;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.FormatCall;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Whenever a format method invocation is found in the syntax tree, checks are performed as
 * specified in the Format String Checker manual.
 *
 * @checker_framework.manual #formatter-guarantees Format String Checker
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

            Result<String> errMissingFormat = fc.hasFormatAnnotation();
            if (errMissingFormat != null) {
                // The string's type has no @Format annotation.
                if (isWrappedFormatCall(fc)) {
                    // Nothing to do, because call is legal.
                } else {
                    // I.1
                    tu.failure(errMissingFormat, "format.string.invalid", errMissingFormat.value());
                }
            } else {
                // The string has a @Format annotation.
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
                                            ExecutableElement method =
                                                    TreeUtils.elementFromUse(node);
                                            Name methodName = method.getSimpleName();
                                            tu.failure(
                                                    param,
                                                    "argument.type.incompatible",
                                                    "", // parameter name is not useful
                                                    methodName,
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

    /**
     * Returns true if {@code fc} is within a method m annotated as {@code @FormatMethod}, and fc's
     * arguments are m's formal parameters. In other words, fc forwards m's arguments to another
     * format method.
     *
     * @param fc an invocation of a format method
     * @return true if {@code fc} is a call to a format method that forwards its containing methods'
     *     arguments
     */
    private boolean isWrappedFormatCall(FormatCall fc) {

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(fc.node));
        if (enclosingMethod == null) {
            return false;
        }
        ExecutableElement enclosingMethodElement =
                TreeUtils.elementFromDeclaration(enclosingMethod);
        boolean withinFormatMethod =
                (atypeFactory.getDeclAnnotation(enclosingMethodElement, FormatMethod.class)
                        != null);
        if (!withinFormatMethod) {
            return false;
        }

        List<? extends ExpressionTree> args = fc.node.getArguments();
        List<? extends VariableTree> params = enclosingMethod.getParameters();
        List<? extends VariableElement> paramElements = enclosingMethodElement.getParameters();

        // Strip off leading Locale arguments.
        if (!args.isEmpty() && FormatterTreeUtil.isLocale(args.get(0), atypeFactory)) {
            args = args.subList(1, args.size());
        }
        if (!params.isEmpty()
                && TypesUtils.isDeclaredOfName(paramElements.get(0).asType(), "java.util.Locale")) {
            params = params.subList(1, params.size());
        }

        if (args.size() == params.size()) {
            for (int i = 0; i < args.size(); i++) {
                ExpressionTree arg = args.get(i);
                if (!(arg instanceof IdentifierTree
                        && ((IdentifierTree) arg).getName() == params.get(i).getName())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);

        AnnotationMirror rhs = valueType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);
        AnnotationMirror lhs = varType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);

        // From the manual: "It is legal to use a format string with fewer format specifiers
        // than required, but a warning is issued."
        // The format.missing.arguments warning is issued here for assignments.
        // For method calls, it is issued in visitMethodInvocation.
        if (rhs != null
                && lhs != null
                && AnnotationUtils.areSameByName(rhs, atypeFactory.FORMAT)
                && AnnotationUtils.areSameByName(lhs, atypeFactory.FORMAT)) {
            ConversionCategory[] rhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(rhs);
            ConversionCategory[] lhsArgTypes =
                    atypeFactory.treeUtil.formatAnnotationToCategories(lhs);

            if (rhsArgTypes.length < lhsArgTypes.length) {
                checker.reportWarning(
                        valueTree,
                        "format.missing.arguments",
                        varType.toString(),
                        valueType.toString());
            }
        }
    }
}
