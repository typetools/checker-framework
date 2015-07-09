package tests.fieldtypeinference;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.fieldtypeinference.FieldTypeInferenceAnnotatedTypeFactory;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import tests.fieldtypeinference.qual.*;
/**
 * AnnotatedTypeFactory with private field type inference enabled for testing.
 * <p>
 * The used qualifier hierarchy is straightforward and only intended for test
 * purposes.
 *
 * @author pbsf
 */
@TypeQualifiers({ Parent.class, Top.class, Sibling1.class, Sibling2.class,
        FieldTypeInferenceBottom.class })
public class FieldTypeInferenceTestAnnotatedTypeFactory extends FieldTypeInferenceAnnotatedTypeFactory {
    public FieldTypeInferenceTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                FieldTypeInferenceBottom.class);
        addTypeNameImplicit(java.lang.Void.class, bottom);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(
                this);
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                FieldTypeInferenceBottom.class);
        implicitsTreeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.NULL_LITERAL, bottom);
        implicitsTreeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.INT_LITERAL, bottom);

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this),
                implicitsTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                FieldTypeInferenceBottom.class);
        return new GraphQualifierHierarchy(factory, bottom);
    }
}
