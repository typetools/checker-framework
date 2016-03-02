package org.checkerframework.checker.formatter;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.IllegalFormatException;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

/**
 * Adds {@link Format} to the type of tree, if it is a {@code String} or
 * {@code char} literal that represents a satisfiable format. The annotation's
 * value is set to be a list of appropriate {@link ConversionCategory} values
 * for every parameter of the format.
 *
 * @see ConversionCategory
 *
 * @author Konstantin Weitz
 */
public class FormatterAnnotatedTypeFactory extends
        GenericAnnotatedTypeFactory<CFValue, CFStore, FormatterTransfer, FormatterAnalysis> {

    private final AnnotationMirror FORMAT;
    private final AnnotationMirror INVALIDFORMAT;
    private final AnnotationMirror FORMATBOTTOM;

    protected final FormatterTreeUtil treeUtil;

    public FormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        FORMAT = AnnotationUtils.fromClass(elements, Format.class);
        INVALIDFORMAT = AnnotationUtils.fromClass(elements, InvalidFormat.class);
        FORMATBOTTOM = AnnotationUtils.fromClass(elements, FormatBottom.class);

        this.treeUtil = new FormatterTreeUtil(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll(
                UnknownFormat.class, FormatBottom.class);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FormatterQualifierHierarchy(factory);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new FormatterTreeAnnotator(this)
        );
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
                        anno = FormatterAnnotatedTypeFactory.this.treeUtil.categoriesToFormatAnnotation(cs);
                    } catch (IllegalFormatException e) {
                        anno = FormatterAnnotatedTypeFactory.this.treeUtil.exceptionToInvalidFormatAnnotation(e);
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
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, FORMAT) &&
                AnnotationUtils.areSameIgnoringValues(lhs, FORMAT))
            {
                ConversionCategory[] rhsArgTypes =
                        treeUtil.formatAnnotationToCategories(rhs);
                ConversionCategory[] lhsArgTypes =
                        treeUtil.formatAnnotationToCategories(lhs);

                if (rhsArgTypes.length != lhsArgTypes.length) {
                    return false;
                }

                for (int i = 0; i < rhsArgTypes.length; ++i) {
                    if (!ConversionCategory.isSubsetOf(lhsArgTypes[i], rhsArgTypes[i])) {
                        return false;
                    }
                }
                return true;
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, FORMAT)) {
                lhs = FORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, FORMAT)) {
                rhs = FORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, INVALIDFORMAT)) {
                lhs = INVALIDFORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, INVALIDFORMAT)) {
                rhs = INVALIDFORMAT;
            }

            return super.isSubtype(rhs, lhs);
        }
    }
}
