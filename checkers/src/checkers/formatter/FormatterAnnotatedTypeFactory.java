package checkers.formatter;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.formatter.quals.ConversionCategory;
import checkers.formatter.quals.Format;
import checkers.formatter.quals.FormatBottom;
import checkers.formatter.quals.InvalidFormat;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.types.TreeAnnotator;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;

import java.util.IllegalFormatException;

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
        AbstractBasicAnnotatedTypeFactory<CFValue, CFStore, FormatterTransfer, FormatterAnalysis> {

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
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FormatterQualifierHierarchy(factory);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new FormatterTreeAnnotator(this);
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
