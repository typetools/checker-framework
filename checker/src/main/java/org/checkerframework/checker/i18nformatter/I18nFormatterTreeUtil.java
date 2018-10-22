package org.checkerframework.checker.i18nformatter;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.SimpleTypeVisitor7;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.i18nformatter.qual.I18nChecksFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nValidFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class provides a collection of utilities to ease working with syntax trees that have
 * something to do with I18nFormatters.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
public class I18nFormatterTreeUtil {
    public final BaseTypeChecker checker;
    public final ProcessingEnvironment processingEnv;

    public I18nFormatterTreeUtil(BaseTypeChecker checker) {
        this.checker = checker;
        this.processingEnv = checker.getProcessingEnvironment();
    }

    /** Describe the format annotation type. */
    public enum FormatType {
        I18NINVALID,
        I18NFORMAT,
        I18NFORMATFOR
    }

    /**
     * Takes an exception that describes an invalid formatter string and returns a syntax trees
     * element that represents a {@link I18nInvalidFormat} annotation with the exception's error
     * message as value.
     */
    public AnnotationMirror exceptionToInvalidFormatAnnotation(IllegalArgumentException ex) {
        return stringToInvalidFormatAnnotation(ex.getMessage());
    }

    /**
     * Takes an invalid formatter string and returns a syntax trees element that represents a {@link
     * I18nInvalidFormat} annotation with the invalid formatter string as value.
     */
    // package-private
    AnnotationMirror stringToInvalidFormatAnnotation(String invalidFormatString) {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, I18nInvalidFormat.class.getCanonicalName());
        builder.setValue("value", invalidFormatString);
        return builder.build();
    }

    /**
     * Takes a syntax tree element that represents a {@link I18nInvalidFormat} annotation, and
     * returns its value.
     */
    public String invalidFormatAnnotationToErrorMessage(AnnotationMirror anno) {
        return "\"" + AnnotationUtils.getElementValue(anno, "value", String.class, true) + "\"";
    }

    /**
     * Takes a list of ConversionCategory elements, and returns a syntax tree element that
     * represents a {@link I18nFormat} annotation with the list as value.
     */
    public AnnotationMirror categoriesToFormatAnnotation(I18nConversionCategory[] args) {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, I18nFormat.class.getCanonicalName());
        builder.setValue("value", args);
        return builder.build();
    }

    /**
     * Takes a syntax tree element that represents a {@link I18nFormat} annotation, and returns its
     * value.
     */
    public I18nConversionCategory[] formatAnnotationToCategories(AnnotationMirror anno) {
        List<I18nConversionCategory> list =
                AnnotationUtils.getElementValueEnumArray(
                        anno, "value", I18nConversionCategory.class, false);
        return list.toArray(new I18nConversionCategory[] {});
    }

    public boolean isHasFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = node.getTarget().getMethod();
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nChecksFormat.class);
        return anno != null;
    }

    public boolean isIsFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = node.getTarget().getMethod();
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nValidFormat.class);
        return anno != null;
    }

    public boolean isMakeFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = node.getTarget().getMethod();
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nMakeFormat.class);
        return anno != null;
    }

    /** Reports an error. Takes a {@link Result} to report the location. */
    public final <E> void failure(Result<E> res, @CompilerMessageKey String msg, Object... args) {
        checker.report(
                org.checkerframework.framework.source.Result.failure(msg, args), res.location);
    }

    /** Reports an warning. Takes a {@link Result} to report the location. */
    public final <E> void warning(Result<E> res, @CompilerMessageKey String msg, Object... args) {
        checker.report(
                org.checkerframework.framework.source.Result.warning(msg, args), res.location);
    }

    private I18nConversionCategory[] asFormatCallCategoriesLowLevel(MethodInvocationNode node) {
        Node vararg = node.getArgument(1);
        if (vararg instanceof ArrayCreationNode) {
            List<Node> convs = ((ArrayCreationNode) vararg).getInitializers();
            I18nConversionCategory[] res = new I18nConversionCategory[convs.size()];
            for (int i = 0; i < convs.size(); i++) {
                Node conv = convs.get(i);
                if (conv instanceof FieldAccessNode) {
                    if (typeMirrorToClass(((FieldAccessNode) conv).getType())
                            == I18nConversionCategory.class) {
                        res[i] =
                                I18nConversionCategory.valueOf(
                                        ((FieldAccessNode) conv).getFieldName());
                        continue; /* avoid returning null */
                    }
                }
                return null;
            }
            return res;
        }
        return null;
    }

    public Result<I18nConversionCategory[]> getHasFormatCallCategories(MethodInvocationNode node) {
        return new Result<>(asFormatCallCategoriesLowLevel(node), node.getTree());
    }

    public Result<I18nConversionCategory[]> makeFormatCallCategories(
            MethodInvocationNode node, I18nFormatterAnnotatedTypeFactory atypeFactory) {
        Map<String, String> translations = atypeFactory.translations;
        Node firstParam = node.getArgument(0);
        Result<I18nConversionCategory[]> ret = new Result<>(null, node.getTree());

        // Now only work with a literal string
        if (firstParam != null && (firstParam instanceof StringLiteralNode)) {
            String s = ((StringLiteralNode) firstParam).getValue();
            if (translations.containsKey(s)) {
                String value = translations.get(s);
                ret = new Result<>(I18nFormatUtil.formatParameterCategories(value), node.getTree());
            }
        }
        return ret;
    }

    /** Returns an I18nFormatCall instance, only if FormatFor is called. Otherwise, returns null. */
    public I18nFormatCall createFormatForCall(
            MethodInvocationTree tree,
            MethodInvocationNode node,
            I18nFormatterAnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType methodAnno = atypeFactory.getAnnotatedType(method);
        for (AnnotatedTypeMirror paramType : methodAnno.getParameterTypes()) {
            // find @FormatFor
            if (paramType.getAnnotation(I18nFormatFor.class) != null) {
                return atypeFactory.treeUtil.new I18nFormatCall(tree, node, atypeFactory);
            }
        }
        return null;
    }

    /**
     * Represents a format method invocation in the syntax tree.
     *
     * <p>An I18nFormatCall instance can only be instantiated by createFormatForCall method
     */
    public class I18nFormatCall {

        private final ExpressionTree tree;
        private ExpressionTree formatArg;
        private final AnnotatedTypeFactory atypeFactory;
        private List<? extends ExpressionTree> args;
        private String invalidMessage;

        private AnnotatedTypeMirror formatAnno;

        public I18nFormatCall(
                MethodInvocationTree tree,
                MethodInvocationNode node,
                AnnotatedTypeFactory atypeFactory) {
            this.tree = tree;
            this.atypeFactory = atypeFactory;
            List<? extends ExpressionTree> theargs = (tree).getArguments();
            this.args = null;
            ExecutableElement method = TreeUtils.elementFromUse(tree);
            AnnotatedExecutableType methodAnno = atypeFactory.getAnnotatedType(method);
            initialCheck(theargs, method, node, methodAnno);
        }

        @Override
        public String toString() {
            return this.tree.toString();
        }

        /**
         * This method checks the validity of the FormatFor. If it is valid, this.args will be set
         * to the correct parameter arguments. Otherwise, it will be still null.
         */
        private void initialCheck(
                List<? extends ExpressionTree> theargs,
                ExecutableElement method,
                MethodInvocationNode node,
                AnnotatedExecutableType methodAnno) {
            int paramIndex = -1;
            Receiver paramArg = null;
            int i = 0;
            for (AnnotatedTypeMirror paramType : methodAnno.getParameterTypes()) {
                if (paramType.getAnnotation(I18nFormatFor.class) != null) {
                    this.formatArg = theargs.get(i);
                    this.formatAnno = atypeFactory.getAnnotatedType(formatArg);

                    if (!typeMirrorToClass(paramType.getUnderlyingType()).equals(String.class)) {
                        // Invalid FormatFor invocation
                        return;
                    }
                    FlowExpressionContext flowExprContext =
                            FlowExpressionContext.buildContextForMethodUse(
                                    node, checker.getContext());
                    String formatforArg =
                            AnnotationUtils.getElementValue(
                                    paramType.getAnnotation(I18nFormatFor.class),
                                    "value",
                                    String.class,
                                    false);
                    if (flowExprContext != null) {
                        try {
                            paramArg =
                                    FlowExpressionParseUtil.parse(
                                            formatforArg,
                                            flowExprContext,
                                            atypeFactory.getPath(tree),
                                            true);
                            paramIndex = flowExprContext.arguments.indexOf(paramArg);
                        } catch (FlowExpressionParseException e) {
                            // report errors here
                            checker.report(
                                    org.checkerframework.framework.source.Result.failure(
                                            "i18nformat.invalid.formatfor"),
                                    tree);
                        }
                    }
                    break;
                }
                i++;
            }

            if (paramIndex != -1) {
                VariableElement param = method.getParameters().get(paramIndex);
                if (param.asType().getKind().equals(TypeKind.ARRAY)) {
                    this.args = theargs.subList(paramIndex, theargs.size());
                } else {
                    this.args = theargs.subList(paramIndex, paramIndex + 1);
                }
            }
        }

        public Result<FormatType> getFormatType() {
            FormatType type;
            if (isValidFormatForInvocation()) {
                if (formatAnno.hasAnnotation(I18nFormat.class)) {
                    type = FormatType.I18NFORMAT;
                } else if (formatAnno.hasAnnotation(I18nFormatFor.class)) {
                    type = FormatType.I18NFORMATFOR;
                } else {
                    type = FormatType.I18NINVALID;
                    invalidMessage = "(is a @I18nFormat annotation missing?)";
                    AnnotationMirror inv = formatAnno.getAnnotation(I18nInvalidFormat.class);
                    if (inv != null) {
                        invalidMessage =
                                AnnotationUtils.getElementValue(inv, "value", String.class, true);
                    }
                }
            } else {
                // If the FormatFor is invalid, it's still I18nFormatFor type but invalid,
                // and we can't do anything else
                type = FormatType.I18NFORMATFOR;
            }
            return new Result<>(type, formatArg);
        }

        public final String getInvalidError() {
            return invalidMessage;
        }

        public boolean isValidFormatForInvocation() {
            return this.args != null;
        }

        /**
         * Returns the type of method invocation.
         *
         * @see InvocationType
         */
        public final Result<InvocationType> getInvocationType() {
            InvocationType type = InvocationType.VARARG;

            if (args.size() == 1) {
                final ExpressionTree first = args.get(0);
                TypeMirror argType = atypeFactory.getAnnotatedType(first).getUnderlyingType();
                // figure out if argType is an array
                type =
                        argType.accept(
                                new SimpleTypeVisitor7<InvocationType, Class<Void>>() {
                                    @Override
                                    protected InvocationType defaultAction(
                                            TypeMirror e, Class<Void> p) {
                                        // not an array
                                        return InvocationType.VARARG;
                                    }

                                    @Override
                                    public InvocationType visitArray(ArrayType t, Class<Void> p) {
                                        // it's an array, now figure out if it's a
                                        // (Object[])null array
                                        return first.accept(
                                                new SimpleTreeVisitor<
                                                        InvocationType, Class<Void>>() {
                                                    @Override
                                                    protected InvocationType defaultAction(
                                                            Tree node, Class<Void> p) {
                                                        // just a normal array
                                                        return InvocationType.ARRAY;
                                                    }

                                                    @Override
                                                    public InvocationType visitTypeCast(
                                                            TypeCastTree node, Class<Void> p) {
                                                        // it's a (Object[])null
                                                        return atypeFactory
                                                                                .getAnnotatedType(
                                                                                        node
                                                                                                .getExpression())
                                                                                .getUnderlyingType()
                                                                                .getKind()
                                                                        == TypeKind.NULL
                                                                ? InvocationType.NULLARRAY
                                                                : InvocationType.ARRAY;
                                                    }
                                                },
                                                p);
                                    }

                                    @Override
                                    public InvocationType visitNull(NullType t, Class<Void> p) {
                                        return InvocationType.NULLARRAY;
                                    }
                                },
                                Void.TYPE);
            }

            ExpressionTree loc;
            loc = ((MethodInvocationTree) tree).getMethodSelect();
            if (type != InvocationType.VARARG && args.size() > 0) {
                loc = args.get(0);
            }
            return new Result<>(type, loc);
        }

        public Result<FormatType> getInvalidInvocationType() {
            return new Result<>(FormatType.I18NFORMATFOR, formatArg);
        }

        /**
         * Returns the conversion category for every parameter.
         *
         * @see I18nConversionCategory
         */
        public final I18nConversionCategory[] getFormatCategories() {
            AnnotationMirror anno = formatAnno.getAnnotation(I18nFormat.class);
            return formatAnnotationToCategories(anno);
        }

        public final Result<TypeMirror>[] getParamTypes() {
            // One to suppress warning in javac, the other to suppress warning in Eclipse...
            @SuppressWarnings({"rawtypes", "unchecked"})
            Result<TypeMirror>[] res = new Result[args.size()];
            for (int i = 0; i < res.length; ++i) {
                ExpressionTree arg = args.get(i);
                TypeMirror argType = atypeFactory.getAnnotatedType(arg).getUnderlyingType();
                res[i] = new Result<>(argType, arg);
            }
            return res;
        }

        public boolean isValidParameter(I18nConversionCategory formatCat, TypeMirror paramType) {
            Class<? extends Object> type = typeMirrorToClass(paramType);
            if (type == null) {
                // we did not recognize the parameter type
                return false;
            }
            for (Class<? extends Object> c : formatCat.types) {
                if (c.isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final Class<? extends Object> typeMirrorToClass(final TypeMirror type) {
        return type.accept(
                new SimpleTypeVisitor7<Class<? extends Object>, Class<Void>>() {
                    @Override
                    public Class<? extends Object> visitPrimitive(PrimitiveType t, Class<Void> v) {
                        switch (t.getKind()) {
                            case BOOLEAN:
                                return Boolean.class;
                            case BYTE:
                                return Byte.class;
                            case CHAR:
                                return Character.class;
                            case SHORT:
                                return Short.class;
                            case INT:
                                return Integer.class;
                            case LONG:
                                return Long.class;
                            case FLOAT:
                                return Float.class;
                            case DOUBLE:
                                return Double.class;
                            default:
                                return null;
                        }
                    }

                    @Override
                    public Class<? extends Object> visitDeclared(DeclaredType dt, Class<Void> v) {
                        return dt.asElement()
                                .accept(
                                        new SimpleElementVisitor7<
                                                Class<? extends Object>, Class<Void>>() {
                                            @Override
                                            public Class<? extends Object> visitType(
                                                    TypeElement e, Class<Void> v) {
                                                try {
                                                    return Class.forName(
                                                            e.getQualifiedName().toString());
                                                } catch (ClassNotFoundException e1) {
                                                    return null; // the lookup should work for all
                                                    // the classes we care about
                                                }
                                            }
                                        },
                                        Void.TYPE);
                    }
                },
                Void.TYPE);
    }
}
