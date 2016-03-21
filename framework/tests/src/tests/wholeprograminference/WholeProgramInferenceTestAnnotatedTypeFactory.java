package tests.wholeprograminference;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.UnknownClass;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import tests.wholeprograminference.qual.DefaultType;
import tests.wholeprograminference.qual.ImplicitAnno;
import tests.wholeprograminference.qual.Parent;
import tests.wholeprograminference.qual.Sibling1;
import tests.wholeprograminference.qual.Sibling2;
import tests.wholeprograminference.qual.SiblingWithFields;
import tests.wholeprograminference.qual.WholeProgramInferenceBottom;
import tests.wholeprograminference.qual.Top;
/**
 * AnnotatedTypeFactory to test whole-program inference using .jaif
 * files.
 * <p>
 * The used qualifier hierarchy is straightforward and only intended for test
 * purposes.
 * 
 * @author pbsf
 */
public class WholeProgramInferenceTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror PARENT = new AnnotationBuilder(
            processingEnv, Parent.class).build();
    private final AnnotationMirror BOTTOM = new AnnotationBuilder(
            processingEnv, WholeProgramInferenceBottom.class).build();

    public WholeProgramInferenceTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        addTypeNameImplicit(java.lang.Void.class, BOTTOM);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new HashSet<Class<? extends Annotation>>(Arrays.asList(
                    Parent.class, DefaultType.class, Top.class, Sibling1.class,
                    Sibling2.class, WholeProgramInferenceBottom.class,
                    SiblingWithFields.class, ImplicitAnno.class)));
    }
    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(
                this);
        implicitsTreeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.NULL_LITERAL, BOTTOM);
        implicitsTreeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.INT_LITERAL, BOTTOM);

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this),
                implicitsTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new WholeProgramInferenceTestQualifierHierarchy(factory);
    }

    /**
     * Using a MultiGraphQualifierHierarchy to enable tests with Annotations
     * that contain fields. @see SiblingWithFields.
     * @author pbsf
     *
     */
    protected class WholeProgramInferenceTestQualifierHierarchy extends
                MultiGraphQualifierHierarchy {

        public WholeProgramInferenceTestQualifierHierarchy(MultiGraphFactory f) {
            super(f);
        }

        @Override
        public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
            return BOTTOM;
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if ((AnnotationUtils.areSameByClass(a1, Sibling1.class)
                 && AnnotationUtils.areSameByClass(a2, Sibling2.class)) 
                || (AnnotationUtils.areSameByClass(a1, Sibling2.class)
                 && AnnotationUtils.areSameByClass(a2, Sibling1.class))
                || (AnnotationUtils.areSameByClass(a1, Sibling1.class)
                 && AnnotationUtils.areSameByClass(a2, SiblingWithFields.class))
                || (AnnotationUtils.areSameByClass(a1, SiblingWithFields.class)
                 && AnnotationUtils.areSameByClass(a2, Sibling2.class))
                || (AnnotationUtils.areSameByClass(a1, Sibling2.class)
                 && AnnotationUtils.areSameByClass(a2, SiblingWithFields.class))) {
                return PARENT;
            }
            return super.leastUpperBound(a1, a2);
        }
        @Override
        public boolean isSubtype(AnnotationMirror sub, AnnotationMirror sup) {
            if (AnnotationUtils.areSame(sub, sup)
                || AnnotationUtils.areSameByClass(sup, UnknownClass.class)
                || AnnotationUtils.areSameByClass(sub, WholeProgramInferenceBottom.class)
                || AnnotationUtils.areSameByClass(sup, Top.class)) {
                return true;
            }

            if (AnnotationUtils.areSameByClass(sub, UnknownClass.class)
                || AnnotationUtils.areSameByClass(sup, WholeProgramInferenceBottom.class)) {
                return false;
            }

            if (AnnotationUtils.areSameByClass(sub, Top.class)) {
                return false;
            }

            if ((AnnotationUtils.areSameByClass(sub, ImplicitAnno.class))
                && (AnnotationUtils.areSameByClass(sup, Sibling1.class)
                    || AnnotationUtils.areSameByClass(sup, Sibling2.class)
                    || AnnotationUtils.areSameByClass(sup, SiblingWithFields.class))) {
                return true;
            }

            if ((AnnotationUtils.areSameByClass(sub, Sibling1.class)
                 || AnnotationUtils.areSameByClass(sub, Sibling2.class)
                 || AnnotationUtils.areSameByClass(sub, ImplicitAnno.class)
                 || AnnotationUtils.areSameByClass(sub, SiblingWithFields.class))
                && AnnotationUtils.areSameByClass(sup, Parent.class)) {
                return true;
            }

            if ((AnnotationUtils.areSameByClass(sub, Sibling1.class)
                 || AnnotationUtils.areSameByClass(sub, Sibling2.class)
                 || AnnotationUtils.areSameByClass(sub, ImplicitAnno.class)
                 || AnnotationUtils.areSameByClass(sub, SiblingWithFields.class)
                 || AnnotationUtils.areSameByClass(sub, Parent.class)) 
                && AnnotationUtils.areSameByClass(sup, DefaultType.class)){
                return true;
            }

            if (AnnotationUtils.areSameByClass(sub, SiblingWithFields.class)
                && AnnotationUtils.areSameByClass(sup, SiblingWithFields.class)) {
                List<String> subVal1 = AnnotationUtils.getElementValueArray(sub,
                        "value", String.class, true);
                List<String> supVal1 = AnnotationUtils.getElementValueArray(sup,
                        "value", String.class, true);
                String subVal2 = AnnotationUtils.getElementValue(sub, "value2",
                        String.class, true);
                String supVal2 = AnnotationUtils.getElementValue(sup, "value2",
                        String.class, true);
                return subVal1.equals(supVal1) && subVal2.equals(supVal2);
            }
            return false;
        }
    }

}
