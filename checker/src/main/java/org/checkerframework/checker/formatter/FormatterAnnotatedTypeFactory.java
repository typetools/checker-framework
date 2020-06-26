package org.checkerframework.checker.formatter;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.IllegalFormatException;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.ComplexQualifierHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

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

    /** The fully-qualified name of the {@link Format} qualifier. */
    protected static final String FORMAT_NAME =
            "org.checkerframework.checker.formatter.qual.Format";
    /** The fully-qualified name of the {@link InvalidFormat} qualifier. */
    protected static final String INVALIDFORMAT_NAME =
            "org.checkerframework.checker.formatter.qual.InvalidFormat";

    /** Syntax tree utilities. */
    protected final FormatterTreeUtil treeUtil = new FormatterTreeUtil(checker);

    /** Creates a FormatterAnnotatedTypeFactory. */
    public FormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

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

    private class FormatterTreeAnnotator extends TreeAnnotator {
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
    class FormatterQualifierHierarchy extends ComplexQualifierHierarchy {

        /** Qualifier kind for {@link Format} annotation. */
        private final QualifierKind FORMAT_KIND;

        /** Qualifier kind for {@link InvalidFormat} annotation. */
        private final QualifierKind INVALIDFORMAT_KIND;

        /** Creates a {@link FormatterQualifierHierarchy}. */
        public FormatterQualifierHierarchy() {
            super(FormatterAnnotatedTypeFactory.this.getSupportedTypeQualifiers(), elements);
            FORMAT_KIND = getQualifierKind(FORMAT_NAME);
            INVALIDFORMAT_KIND = getQualifierKind(INVALIDFORMAT_NAME);
        }

        @Override
        protected boolean isSubtype(
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
        protected AnnotationMirror leastUpperBound(
                AnnotationMirror anno1,
                QualifierKind qual1,
                AnnotationMirror anno2,
                QualifierKind qual2) {
            if (qual1.isBottom()) {
                return anno2;
            } else if (qual2.isBottom()) {
                return anno1;
            } else if (qual1 == FORMAT_KIND && qual2 == FORMAT_KIND) {
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
            } else if (qual1 == INVALIDFORMAT_KIND && qual2 == INVALIDFORMAT_KIND) {

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
        protected AnnotationMirror greatestLowerBound(
                AnnotationMirror anno1,
                QualifierKind qual1,
                AnnotationMirror anno2,
                QualifierKind qual2) {
            if (qual1.isTop()) {
                return anno2;
            } else if (qual2.isTop()) {
                return anno1;
            } else if (qual1 == FORMAT_KIND && qual2 == FORMAT_KIND) {
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
            } else if (qual1 == INVALIDFORMAT_KIND && qual2 == INVALIDFORMAT_KIND) {

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
