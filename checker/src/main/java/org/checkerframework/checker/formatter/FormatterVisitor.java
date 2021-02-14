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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.FormatCall;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
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
            MethodTree enclosingMethod =
                    TreePathUtil.enclosingMethod(atypeFactory.getPath(fc.node));

            Result<String> errMissingFormat = fc.hasFormatAnnotation();
            if (errMissingFormat != null) {
                // The string's type has no @Format annotation.
                if (isWrappedFormatCall(fc, enclosingMethod)) {
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
                        Result<TypeMirror>[] argTypes = fc.getArgTypes();
                        int argl = argTypes.length;
                        int formatl = formatCats.length;
                        if (argl < formatl) {
                            // For assignments, format.missing.arguments is issued
                            // from commonAssignmentCheck.
                            // II.1
                            tu.failure(invc, "format.missing.arguments", formatl, argl);
                        } else {
                            if (argl > formatl) {
                                // II.2
                                tu.warning(invc, "format.excess.arguments", formatl, argl);
                            }
                            for (int i = 0; i < formatl; ++i) {
                                ConversionCategory formatCat = formatCats[i];
                                Result<TypeMirror> arg = argTypes[i];
                                TypeMirror argType = arg.value();

                                switch (formatCat) {
                                    case UNUSED:
                                        // I.2
                                        tu.warning(arg, "format.argument.unused", " " + (1 + i));
                                        break;
                                    case NULL:
                                        // I.3
                                        if (argType.getKind() == TypeKind.NULL) {
                                            tu.warning(arg, "format.specifier.null", " " + (1 + i));
                                        } else {
                                            tu.failure(arg, "format.specifier.null", " " + (1 + i));
                                        }
                                        break;
                                    case GENERAL:
                                        break;
                                    default:
                                        if (!fc.isValidArgument(formatCat, argType)) {
                                            // II.3
                                            ExecutableElement method =
                                                    TreeUtils.elementFromUse(node);
                                            CharSequence methodName =
                                                    ElementUtils.getSimpleNameOrDescription(method);
                                            tu.failure(
                                                    arg,
                                                    "argument.type.incompatible",
                                                    "in varargs position",
                                                    methodName,
                                                    argType,
                                                    formatCat);
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                    case ARRAY:
                        // III
                        if (!isWrappedFormatCall(fc, enclosingMethod)) {
                            tu.warning(invc, "format.indirect.arguments");
                        }
                        // TODO:  If it is explict array construction, such as "new Object[] {
                        // ... }", then we could treat it like the VARARGS case, analyzing each
                        // argument.  "new array" is probably rare, in the varargs position.
                        // fall through
                    case NULLARRAY:
                        for (ConversionCategory cat : formatCats) {
                            if (cat == ConversionCategory.NULL) {
                                // I.3
                                if (invc.value() == FormatterTreeUtil.InvocationType.NULLARRAY) {
                                    tu.warning(invc, "format.specifier.null", "");
                                } else {
                                    tu.failure(invc, "format.specifier.null", "");
                                }
                            }
                            if (cat == ConversionCategory.UNUSED) {
                                // I.2
                                tu.warning(invc, "format.argument.unused", "");
                            }
                        }
                        break;
                }
            }

            // Support -Ainfer command-line argument.
            WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
            if (wpi != null && forwardsArguments(node, enclosingMethod)) {
                wpi.addMethodDeclarationAnnotation(
                        TreeUtils.elementFromDeclaration(enclosingMethod),
                        atypeFactory.FORMATMETHOD);
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
     * @param enclosingMethod the method that contains the call
     * @return true if {@code fc} is a call to a format method that forwards its containing method's
     *     arguments
     */
    private boolean isWrappedFormatCall(FormatCall fc, @Nullable MethodTree enclosingMethod) {
        if (enclosingMethod == null) {
            return false;
        }
        ExecutableElement enclosingMethodElement =
                TreeUtils.elementFromDeclaration(enclosingMethod);
        boolean withinFormatMethod =
                (atypeFactory.getDeclAnnotation(enclosingMethodElement, FormatMethod.class)
                        != null);
        return withinFormatMethod && forwardsArguments(fc.node, enclosingMethod);
    }

    /**
     * Returns true if {@code fc} is within a method m, and fc's arguments are m's formal
     * parameters. In other words, fc forwards m's arguments.
     *
     * @param invocTree an invocation of a method
     * @param enclosingMethod the method that contains the call
     * @return true if {@code fc} is a call to a method that forwards its containing method's
     *     arguments
     */
    private boolean forwardsArguments(
            MethodInvocationTree invocTree, @Nullable MethodTree enclosingMethod) {

        if (enclosingMethod == null) {
            return false;
        }
        ExecutableElement enclosingMethodElement =
                TreeUtils.elementFromDeclaration(enclosingMethod);

        List<? extends ExpressionTree> args = invocTree.getArguments();
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

        if (args.size() != params.size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            ExpressionTree arg = args.get(i);
            if (!(arg instanceof IdentifierTree
                    && ((IdentifierTree) arg).getName() == params.get(i).getName())) {
                return false;
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
                && AnnotationUtils.areSameByName(rhs, FormatterAnnotatedTypeFactory.FORMAT_NAME)
                && AnnotationUtils.areSameByName(lhs, FormatterAnnotatedTypeFactory.FORMAT_NAME)) {
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
