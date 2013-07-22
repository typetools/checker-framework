package checkers.formatter;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.formatter.quals.ConversionCategory;
import checkers.formatter.quals.Format;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.TreeAnnotator;

import javacutils.AnnotationUtils;

import java.util.IllegalFormatException;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;
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
AbstractBasicAnnotatedTypeFactory<FormatterChecker, CFValue, CFStore, FormatterTransfer, FormatterAnalysis> {

    private final AnnotationMirror FORMAT;

    public FormatterAnnotatedTypeFactory(FormatterChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        FORMAT = AnnotationUtils.fromClass(elements, Format.class);
        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator(FormatterChecker checker) {
        return new FormatterTreeAnnotator(checker);
    }

    private class FormatterTreeAnnotator extends TreeAnnotator {
        public FormatterTreeAnnotator(BaseTypeChecker<?> checker) {
            super(checker, FormatterAnnotatedTypeFactory.this);
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
                        ConversionCategory[] cs = FormatUtil
                                .formatParameterCategories(format);
                        anno = checker.treeUtil.categoriesToFormatAnnotation(cs);
                    } catch (IllegalFormatException e) {
                        anno = checker.treeUtil.exceptionToInvalidFormatAnnotation(e);
                    }
                    type.addAnnotation(anno);
                }
            }
            return super.visitLiteral(tree, type);
        }
    }
}
