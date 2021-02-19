package org.checkerframework.checker.formatter;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor7;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.ReturnsFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * This class provides a collection of utilities to ease working with syntax trees that have
 * something to do with Formatters.
 */
public class FormatterTreeUtil {
    /** The checker. */
    public final BaseTypeChecker checker;
    /** The processing environment. */
    public final ProcessingEnvironment processingEnv;

    /** The value() element/field of an @InvalidFormat annotation. */
    protected final ExecutableElement invalidFormatValueElement;

    // private final ExecutableElement formatArgTypesElement;

    public FormatterTreeUtil(BaseTypeChecker checker) {
        this.checker = checker;
        this.processingEnv = checker.getProcessingEnvironment();
        invalidFormatValueElement =
                TreeUtils.getMethod(
                        InvalidFormat.class.getCanonicalName(), "value", 0, processingEnv);
        /*
        this.formatArgTypesElement =
                TreeUtils.getMethod(
                        Format.class.getCanonicalName(),
                        "value",
                        0,
                        processingEnv);
         */
    }

    /** Describes the ways a format method may be invoked. */
    public static enum InvocationType {
        /**
         * The parameters are passed as varargs. For example:
         *
         * <blockquote>
         *
         * <pre>
         * String.format("%s %d", "Example", 7);
         * </pre>
         *
         * </blockquote>
         */
        VARARG,

        /**
         * The parameters are passed as array. For example:
         *
         * <blockquote>
         *
         * <pre>
         * Object[] a = new Object[]{"Example",7};
         * String.format("%s %d", a);
         * </pre>
         *
         * </blockquote>
         */
        ARRAY,

        /**
         * A null array is passed to the format method. This happens seldomly.
         *
         * <blockquote>
         *
         * <pre>
         * String.format("%s %d", (Object[])null);
         * </pre>
         *
         * </blockquote>
         */
        NULLARRAY;
    }

    /** A wrapper around a value of type E, plus an ExpressionTree location. */
    public static class Result<E> {
        private final E value;
        public final ExpressionTree location;

        public Result(E value, ExpressionTree location) {
            this.value = value;
            this.location = location;
        }

        public E value() {
            return value;
        }
    }

    /**
     * Returns true if the call is to a method with the @ReturnsFormat annotation. An example of
     * such a method is FormatUtil.asFormat.
     */
    public boolean isAsFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = node.getTarget().getMethod();
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, ReturnsFormat.class);
        return anno != null;
    }

    private ConversionCategory[] asFormatCallCategoriesLowLevel(MethodInvocationNode node) {
        Node vararg = node.getArgument(1);
        if (!(vararg instanceof ArrayCreationNode)) {
            return null;
        }
        List<Node> convs = ((ArrayCreationNode) vararg).getInitializers();
        ConversionCategory[] res = new ConversionCategory[convs.size()];
        for (int i = 0; i < convs.size(); ++i) {
            Node conv = convs.get(i);
            if (conv instanceof FieldAccessNode) {
                Class<? extends Object> clazz =
                        TypesUtils.getClassFromType(((FieldAccessNode) conv).getType());
                if (clazz == ConversionCategory.class) {
                    res[i] = ConversionCategory.valueOf(((FieldAccessNode) conv).getFieldName());
                    continue; /* avoid returning null */
                }
            }
            return null;
        }
        return res;
    }

    public Result<ConversionCategory[]> asFormatCallCategories(MethodInvocationNode node) {
        // TODO make sure the method signature looks good
        return new Result<>(asFormatCallCategoriesLowLevel(node), node.getTree());
    }

    /** Returns true if {@code node} is a call to a method annotated with {@code @FormatMethod}. */
    public boolean isFormatCall(MethodInvocationTree node, AnnotatedTypeFactory atypeFactory) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, FormatMethod.class);
        return anno != null;
    }

    /**
     * Returns true if the given ExpressionTree has type java.util.Locale.
     *
     * @param e an expression
     * @param atypeFactory the type factory
     * @return true if the given ExpressionTree has type java.util.Locale
     */
    public static boolean isLocale(ExpressionTree e, AnnotatedTypeFactory atypeFactory) {
        return (TypesUtils.getClassFromType(atypeFactory.getAnnotatedType(e).getUnderlyingType())
                == Locale.class);
    }

    /** Represents a format method invocation in the syntax tree. */
    public class FormatCall {
        private final AnnotatedTypeMirror formatAnno;
        private final List<? extends ExpressionTree> args;
        final MethodInvocationTree node;
        private final ExpressionTree formatArg;
        private final AnnotatedTypeFactory atypeFactory;

        public FormatCall(MethodInvocationTree node, AnnotatedTypeFactory atypeFactory) {
            this.node = node;
            // TODO figure out how to make passing of environment
            // objects such as atypeFactory, processingEnv, ... nicer
            this.atypeFactory = atypeFactory;
            List<? extends ExpressionTree> theargs;

            theargs = node.getArguments();
            if (isLocale(theargs.get(0), atypeFactory)) {
                // call with Locale as first argument
                theargs = theargs.subList(1, theargs.size());
            }

            // TODO Check that the first parameter exists and is a string.
            formatArg = theargs.get(0);
            formatAnno = atypeFactory.getAnnotatedType(formatArg);
            this.args = theargs.subList(1, theargs.size());
        }

        /**
         * Returns an error description if the format-string argument's type is <em>not</em>
         * annotated as {@code @Format}. Returns null if it is annotated.
         *
         * @result an error description if the format string is not annotated as {@code @Format}, or
         *     null if it is
         */
        public final Result<String> errMissingFormatAnnotation() {
            if (!formatAnno.hasAnnotation(Format.class)) {
                String msg = "(is a @Format annotation missing?)";
                AnnotationMirror inv = formatAnno.getAnnotation(InvalidFormat.class);
                if (inv != null) {
                    msg = invalidFormatAnnotationToErrorMessage(inv);
                }
                return new Result<>(msg, formatArg);
            }
            return null;
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
                                        // it's an array, now figure out if it's a (Object[])null
                                        // array
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

            ExpressionTree loc = node.getMethodSelect();
            if (type != InvocationType.VARARG && !args.isEmpty()) {
                loc = args.get(0);
            }
            return new Result<>(type, loc);
        }

        /**
         * Returns the conversion category for every parameter.
         *
         * @see ConversionCategory
         */
        public final ConversionCategory[] getFormatCategories() {
            AnnotationMirror anno = formatAnno.getAnnotation(Format.class);
            return formatAnnotationToCategories(anno);
        }

        /**
         * Returns the types of the arguments to the call. Use {@link #isValidArgument} and {@link
         * #isArgumentNull} to work with the result.
         *
         * @return the types of the arguments to the call
         */
        public final Result<TypeMirror>[] getArgTypes() {
            // One to suppress warning in javac, the other to suppress warning in Eclipse...
            @SuppressWarnings({"rawtypes", "unchecked"})
            Result<TypeMirror>[] res = new Result[args.size()];
            for (int i = 0; i < res.length; ++i) {
                ExpressionTree arg = args.get(i);
                TypeMirror argType;
                if (TreeUtils.isNullExpression(arg)) {
                    argType = atypeFactory.getProcessingEnv().getTypeUtils().getNullType();
                } else {
                    argType = atypeFactory.getAnnotatedType(arg).getUnderlyingType();
                }
                res[i] = new Result<>(argType, arg);
            }
            return res;
        }

        /**
         * Checks if the type of an argument returned from {@link #getArgTypes()} is valid for the
         * passed ConversionCategory.
         *
         * @param formatCat a format specifier
         * @param argType an argument type
         * @return true if the argument can be passed to the format specifier
         */
        public final boolean isValidArgument(ConversionCategory formatCat, TypeMirror argType) {
            if (argType.getKind() == TypeKind.NULL || isArgumentNull(argType)) {
                return true;
            }
            Class<? extends Object> type = TypesUtils.getClassFromType(argType);
            return formatCat.isAssignableFrom(type);
        }

        /**
         * Checks if the argument returned from {@link #getArgTypes()} is a {@code null} expression.
         *
         * @param type a type
         * @return true if the argument is a {@code null} expression
         */
        public final boolean isArgumentNull(TypeMirror type) {
            // TODO: Just check whether it is the VOID TypeMirror.

            // is it the null literal
            return type.accept(
                    new SimpleTypeVisitor7<Boolean, Class<Void>>() {
                        @Override
                        protected Boolean defaultAction(TypeMirror e, Class<Void> p) {
                            // it's not the null literal
                            return false;
                        }

                        @Override
                        public Boolean visitNull(NullType t, Class<Void> p) {
                            // it's the null literal
                            return true;
                        }
                    },
                    Void.TYPE);
        }
    }

    // The failure() method is required so that FormatterTransfer, which has no access to the
    // FormatterChecker, can report errors.
    /**
     * Reports an error.
     *
     * @param res used for source location information
     * @param msgKey the diagnostic message key
     * @param args arguments to the diagnostic message
     */
    public final void failure(Result<?> res, @CompilerMessageKey String msgKey, Object... args) {
        checker.reportError(res.location, msgKey, args);
    }

    /**
     * Reports a warning.
     *
     * @param res used for source location information
     * @param msgKey the diagnostic message key
     * @param args arguments to the diagnostic message
     */
    public final void warning(Result<?> res, @CompilerMessageKey String msgKey, Object... args) {
        checker.reportWarning(res.location, msgKey, args);
    }

    /**
     * Takes an exception that describes an invalid formatter string and, returns a syntax trees
     * element that represents a {@link InvalidFormat} annotation with the exception's error message
     * as value.
     */
    public AnnotationMirror exceptionToInvalidFormatAnnotation(IllegalFormatException ex) {
        return stringToInvalidFormatAnnotation(ex.getMessage());
    }

    /**
     * Creates an {@link InvalidFormat} annotation with the given string as its value.
     *
     * @param invalidFormatString an invalid formatter string
     * @return an {@link InvalidFormat} annotation with the given string as its value
     */
    // package-private
    AnnotationMirror stringToInvalidFormatAnnotation(String invalidFormatString) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, InvalidFormat.class);
        builder.setValue("value", invalidFormatString);
        return builder.build();
    }

    /**
     * Gets the value() element/field out of an InvalidFormat annotation.
     *
     * @param anno an InvalidFormat annotation
     * @return its value() element/field
     */
    private String getInvalidFormatValue(AnnotationMirror anno) {
        return (String) anno.getElementValues().get(invalidFormatValueElement).getValue();
    }

    /**
     * Takes a syntax tree element that represents a {@link InvalidFormat} annotation, and returns
     * its value.
     *
     * @param anno an InvalidFormat annotation
     * @return its value() element/field
     */
    public String invalidFormatAnnotationToErrorMessage(AnnotationMirror anno) {
        return "\"" + getInvalidFormatValue(anno) + "\"";
    }

    /**
     * Creates a {@code @}{@link Format} annotation with the given list as its value.
     *
     * @param args conversion categories for the {@code @Format} annotation
     * @return a {@code @}{@link Format} annotation with the given list as its value
     */
    public AnnotationMirror categoriesToFormatAnnotation(ConversionCategory[] args) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Format.class);
        builder.setValue("value", args);
        return builder.build();
    }

    /**
     * Takes a syntax tree element that represents a {@link Format} annotation, and returns its
     * value.
     */
    public ConversionCategory[] formatAnnotationToCategories(AnnotationMirror anno) {
        List<ConversionCategory> list =
                AnnotationUtils.getElementValueEnumArray(
                        anno, "value", ConversionCategory.class, false);
        return list.toArray(new ConversionCategory[] {});
    }
}
