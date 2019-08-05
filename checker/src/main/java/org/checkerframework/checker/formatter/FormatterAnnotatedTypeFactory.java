package org.checkerframework.checker.formatter;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.IllegalFormatException;
import java.util.Set;
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
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

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
    /** The @{@link Format} annotation. */
    protected final AnnotationMirror FORMAT = AnnotationBuilder.fromClass(elements, Format.class);
    /** The @{@link InvalidFormat} annotation. */
    protected final AnnotationMirror INVALIDFORMAT =
            AnnotationBuilder.fromClass(elements, InvalidFormat.class);
    /** The @{@link FormatBottom} annotation. */
    protected final AnnotationMirror FORMATBOTTOM =
            AnnotationBuilder.fromClass(elements, FormatBottom.class);

    /** Syntax tree utilities. */
    protected final FormatterTreeUtil treeUtil = new FormatterTreeUtil(checker);

    /** Creates a FormatterAnnotatedTypeFactory. */
    public FormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll(UnknownFormat.class, FormatBottom.class);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FormatterQualifierHierarchy(factory);
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
            if (!type.isAnnotatedInHierarchy(FORMAT)) {
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

    class FormatterQualifierHierarchy extends GraphQualifierHierarchy {

        public FormatterQualifierHierarchy(MultiGraphFactory f) {
            super(f, FORMATBOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByName(subAnno, FORMAT)
                    && AnnotationUtils.areSameByName(superAnno, FORMAT)) {
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
            }
            if (AnnotationUtils.areSameByName(superAnno, FORMAT)) {
                superAnno = FORMAT;
            }
            if (AnnotationUtils.areSameByName(subAnno, FORMAT)) {
                subAnno = FORMAT;
            }
            if (AnnotationUtils.areSameByName(superAnno, INVALIDFORMAT)) {
                superAnno = INVALIDFORMAT;
            }
            if (AnnotationUtils.areSameByName(subAnno, INVALIDFORMAT)) {
                subAnno = INVALIDFORMAT;
            }

            return super.isSubtype(subAnno, superAnno);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror anno1, AnnotationMirror anno2) {
            if (AnnotationUtils.areSameByName(anno1, FORMATBOTTOM)) {
                return anno2;
            }
            if (AnnotationUtils.areSameByName(anno2, FORMATBOTTOM)) {
                return anno1;
            }
            if (AnnotationUtils.areSameByName(anno1, FORMAT)
                    && AnnotationUtils.areSameByName(anno2, FORMAT)) {
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
            }
            if (AnnotationUtils.areSameByName(anno1, INVALIDFORMAT)
                    && AnnotationUtils.areSameByName(anno2, INVALIDFORMAT)) {
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
        public AnnotationMirror greatestLowerBound(AnnotationMirror anno1, AnnotationMirror anno2) {
            if (AnnotationUtils.areSameByName(anno1, UNKNOWNFORMAT)) {
                return anno2;
            }
            if (AnnotationUtils.areSameByName(anno2, UNKNOWNFORMAT)) {
                return anno1;
            }
            if (AnnotationUtils.areSameByName(anno1, FORMAT)
                    && AnnotationUtils.areSameByName(anno2, FORMAT)) {
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
            }
            if (AnnotationUtils.areSameByName(anno1, INVALIDFORMAT)
                    && AnnotationUtils.areSameByName(anno2, INVALIDFORMAT)) {
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
