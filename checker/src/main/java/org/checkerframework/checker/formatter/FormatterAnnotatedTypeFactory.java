package org.checkerframework.checker.formatter;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.checker.formatter.util.FormatUtil;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceJavaParserStorage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import scenelib.annotations.Annotation;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;

import java.util.IllegalFormatException;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/**
 * Adds {@link Format} to the type of tree, if it is a {@code String} or {@code char} literal that
 * represents a satisfiable format. The annotation's value is set to be a list of appropriate {@link
 * ConversionCategory} values for every parameter of the format.
 *
 * @see ConversionCategory
 */
public class FormatterAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @{@link UnknownFormat} annotation. */
    protected final AnnotationMirror UNKNOWNFORMAT =
            AnnotationBuilder.fromClass(elements, UnknownFormat.class);
    /** The @{@link FormatBottom} annotation. */
    protected final AnnotationMirror FORMATBOTTOM =
            AnnotationBuilder.fromClass(elements, FormatBottom.class);
    /** The @{@link FormatMethod} annotation. */
    protected final AnnotationMirror FORMATMETHOD =
            AnnotationBuilder.fromClass(elements, FormatMethod.class);

    /** The fully-qualified name of the {@link Format} qualifier. */
    protected static final @CanonicalName String FORMAT_NAME = Format.class.getCanonicalName();
    /** The fully-qualified name of the {@link InvalidFormat} qualifier. */
    protected static final @CanonicalName String INVALIDFORMAT_NAME =
            InvalidFormat.class.getCanonicalName();

    /** Syntax tree utilities. */
    protected final FormatterTreeUtil treeUtil = new FormatterTreeUtil(checker);

    /** Creates a FormatterAnnotatedTypeFactory. */
    public FormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        addAliasedDeclAnnotation(
                com.google.errorprone.annotations.FormatMethod.class,
                FormatMethod.class,
                FORMATMETHOD);

        this.postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new FormatterQualifierHierarchy();
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new FormatterTreeAnnotator(this));
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a method is annotated with {@code @FormatMethod}, remove any {@code @Format} annotation
     * from its first argument.
     */
    @Override
    public void prepareMethodForWriting(AMethod method) {
        if (hasFormatMethodAnno(method)) {
            AField param = method.parameters.get(0);
            if (param != null) {
                Set<Annotation> paramTypeAnnos = param.type.tlAnnotationsHere;
                paramTypeAnnos.removeIf(
                        a ->
                                a.def.name.equals(
                                        "org.checkerframework.checker.formatter.qual.Format"));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a method is annotated with {@code @FormatMethod}, remove any {@code @Format} annotation
     * from its first argument.
     */
    @Override
    public void prepareMethodForWriting(
            WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos) {
        if (hasFormatMethodAnno(methodAnnos)) {
            AnnotatedTypeMirror atm = methodAnnos.getParameterType(0);
            atm.removeAnnotationByClass(org.checkerframework.checker.formatter.qual.Format.class);
        }
    }

    /**
     * Returns true if the method has a {@code @FormatMethod} annotation.
     *
     * @param methodAnnos method annotations
     * @return true if the method has a {@code @FormatMethod} annotation
     */
    private boolean hasFormatMethodAnno(AMethod methodAnnos) {
        for (Annotation anno : methodAnnos.tlAnnotationsHere) {
            String annoName = anno.def.name;
            if (annoName.equals("org.checkerframework.checker.formatter.qual.FormatMethod")
                    || anno.def.name.equals("com.google.errorprone.annotations.FormatMethod")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the method has a {@code @FormatMethod} annotation.
     *
     * @param methodAnnos method annotations
     * @return true if the method has a {@code @FormatMethod} annotation
     */
    private boolean hasFormatMethodAnno(
            WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos) {
        Set<AnnotationMirror> declarationAnnos = methodAnnos.getDeclarationAnnotations();
        return AnnotationUtils.containsSameByClass(
                        declarationAnnos,
                        org.checkerframework.checker.formatter.qual.FormatMethod.class)
                || AnnotationUtils.containsSameByName(
                        declarationAnnos, "com.google.errorprone.annotations.FormatMethod");
    }

    /** The tree annotator for the Format String Checker. */
    private class FormatterTreeAnnotator extends TreeAnnotator {
        /**
         * Create the tree annotator for the Format String Checker.
         *
         * @param atypeFactory the Format String Checker type factory
         */
        public FormatterTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(UNKNOWNFORMAT)) {
                String format = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    format = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    format = Character.toString((Character) tree.getValue());
                }
                if (format != null) {
                    AnnotationMirror anno;
                    try {
                        ConversionCategory[] cs = FormatUtil.formatParameterCategories(format);
                        anno =
                                FormatterAnnotatedTypeFactory.this.treeUtil
                                        .categoriesToFormatAnnotation(cs);
                    } catch (IllegalFormatException e) {
                        anno =
                                FormatterAnnotatedTypeFactory.this.treeUtil
                                        .exceptionToInvalidFormatAnnotation(e);
                    }
                    type.addAnnotation(anno);
                }
            }
            return super.visitLiteral(tree, type);
        }
    }

    /** Qualifier hierarchy for the Formatter Checker. */
    class FormatterQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** Qualifier kind for the @{@link Format} annotation. */
        private final QualifierKind FORMAT_KIND;

        /** Qualifier kind for the @{@link InvalidFormat} annotation. */
        private final QualifierKind INVALIDFORMAT_KIND;

        /** Creates a {@link FormatterQualifierHierarchy}. */
        public FormatterQualifierHierarchy() {
            super(FormatterAnnotatedTypeFactory.this.getSupportedTypeQualifiers(), elements);
            FORMAT_KIND = getQualifierKind(FORMAT_NAME);
            INVALIDFORMAT_KIND = getQualifierKind(INVALIDFORMAT_NAME);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == FORMAT_KIND && superKind == FORMAT_KIND) {
                ConversionCategory[] rhsArgTypes = treeUtil.formatAnnotationToCategories(subAnno);
                ConversionCategory[] lhsArgTypes = treeUtil.formatAnnotationToCategories(superAnno);

                if (rhsArgTypes.length > lhsArgTypes.length) {
                    return false;
                }

                for (int i = 0; i < rhsArgTypes.length; ++i) {
                    if (!ConversionCategory.isSubsetOf(lhsArgTypes[i], rhsArgTypes[i])) {
                        return false;
                    }
                }
                return true;
            } else if (subKind == INVALIDFORMAT_KIND && superKind == INVALIDFORMAT_KIND) {
                return true;
            }
            throw new BugInCF("Unexpected kinds: %s %s", subKind, superKind);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror anno1,
                QualifierKind qualifierKind1,
                AnnotationMirror anno2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1.isBottom()) {
                return anno2;
            } else if (qualifierKind2.isBottom()) {
                return anno1;
            } else if (qualifierKind1 == FORMAT_KIND && qualifierKind2 == FORMAT_KIND) {
                ConversionCategory[] shorterArgTypesList =
                        treeUtil.formatAnnotationToCategories(anno1);
                ConversionCategory[] longerArgTypesList =
                        treeUtil.formatAnnotationToCategories(anno2);
                if (shorterArgTypesList.length > longerArgTypesList.length) {
                    ConversionCategory[] temp = longerArgTypesList;
                    longerArgTypesList = shorterArgTypesList;
                    shorterArgTypesList = temp;
                }

                // From the manual:
                // It is legal to use a format string with fewer format specifiers
                // than required, but a warning is issued.

                ConversionCategory[] resultArgTypes =
                        new ConversionCategory[longerArgTypesList.length];

                for (int i = 0; i < shorterArgTypesList.length; ++i) {
                    resultArgTypes[i] =
                            ConversionCategory.intersect(
                                    shorterArgTypesList[i], longerArgTypesList[i]);
                }
                for (int i = shorterArgTypesList.length; i < longerArgTypesList.length; ++i) {
                    resultArgTypes[i] = longerArgTypesList[i];
                }
                return treeUtil.categoriesToFormatAnnotation(resultArgTypes);
            } else if (qualifierKind1 == INVALIDFORMAT_KIND
                    && qualifierKind2 == INVALIDFORMAT_KIND) {

                assert !anno1.getElementValues().isEmpty();
                assert !anno2.getElementValues().isEmpty();

                if (AnnotationUtils.areSame(anno1, anno2)) {
                    return anno1;
                }

                return treeUtil.stringToInvalidFormatAnnotation(
                        "("
                                + treeUtil.invalidFormatAnnotationToErrorMessage(anno1)
                                + " or "
                                + treeUtil.invalidFormatAnnotationToErrorMessage(anno2)
                                + ")");
            }

            return UNKNOWNFORMAT;
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror anno1,
                QualifierKind qualifierKind1,
                AnnotationMirror anno2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1.isTop()) {
                return anno2;
            } else if (qualifierKind2.isTop()) {
                return anno1;
            } else if (qualifierKind1 == FORMAT_KIND && qualifierKind2 == FORMAT_KIND) {
                ConversionCategory[] anno1ArgTypes = treeUtil.formatAnnotationToCategories(anno1);
                ConversionCategory[] anno2ArgTypes = treeUtil.formatAnnotationToCategories(anno2);

                // From the manual:
                // It is legal to use a format string with fewer format specifiers
                // than required, but a warning is issued.
                int length = anno1ArgTypes.length;
                if (anno2ArgTypes.length < length) {
                    length = anno2ArgTypes.length;
                }

                ConversionCategory[] anno3ArgTypes = new ConversionCategory[length];

                for (int i = 0; i < length; ++i) {
                    anno3ArgTypes[i] = ConversionCategory.union(anno1ArgTypes[i], anno2ArgTypes[i]);
                }
                return treeUtil.categoriesToFormatAnnotation(anno3ArgTypes);
            } else if (qualifierKind1 == INVALIDFORMAT_KIND
                    && qualifierKind2 == INVALIDFORMAT_KIND) {

                assert !anno1.getElementValues().isEmpty();
                assert !anno2.getElementValues().isEmpty();

                if (AnnotationUtils.areSame(anno1, anno2)) {
                    return anno1;
                }

                return treeUtil.stringToInvalidFormatAnnotation(
                        "("
                                + treeUtil.invalidFormatAnnotationToErrorMessage(anno1)
                                + " and "
                                + treeUtil.invalidFormatAnnotationToErrorMessage(anno2)
                                + ")");
            }

            return FORMATBOTTOM;
        }
    }
}
