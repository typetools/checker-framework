package testlib.wholeprograminference;

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
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import testlib.wholeprograminference.qual.DefaultType;
import testlib.wholeprograminference.qual.ImplicitAnno;
import testlib.wholeprograminference.qual.Parent;
import testlib.wholeprograminference.qual.Sibling1;
import testlib.wholeprograminference.qual.Sibling2;
import testlib.wholeprograminference.qual.SiblingWithFields;
import testlib.wholeprograminference.qual.Top;
import testlib.wholeprograminference.qual.WholeProgramInferenceBottom;

/**
 * AnnotatedTypeFactory to test whole-program inference using .jaif files.
 *
 * <p>The used qualifier hierarchy is straightforward and only intended for test purposes.
 */
public class WholeProgramInferenceTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror PARENT =
            new AnnotationBuilder(processingEnv, Parent.class).build();
    private final AnnotationMirror BOTTOM =
            new AnnotationBuilder(processingEnv, WholeProgramInferenceBottom.class).build();

    public WholeProgramInferenceTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        Parent.class,
                        DefaultType.class,
                        Top.class,
                        Sibling1.class,
                        Sibling2.class,
                        WholeProgramInferenceBottom.class,
                        SiblingWithFields.class,
                        ImplicitAnno.class));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        LiteralTreeAnnotator literalTreeAnnotator = new LiteralTreeAnnotator(this);
        literalTreeAnnotator.addLiteralKind(LiteralKind.INT, BOTTOM);
        literalTreeAnnotator.addStandardLiteralQualifiers();

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this), literalTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new WholeProgramInferenceTestQualifierHierarchy(factory);
    }

    /**
     * Using a MultiGraphQualifierHierarchy to enable tests with Annotations that contain
     * fields. @see SiblingWithFields.
     */
    protected class WholeProgramInferenceTestQualifierHierarchy
            extends MultiGraphQualifierHierarchy {

        public WholeProgramInferenceTestQualifierHierarchy(MultiGraphFactory f) {
            super(f);
        }

        @Override
        public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
            return BOTTOM;
        }

        @Override
        public Set<? extends AnnotationMirror> getBottomAnnotations() {
            return Collections.singleton(BOTTOM);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
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
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, superAnno)
                    || AnnotationUtils.areSameByClass(superAnno, UnknownClass.class)
                    || AnnotationUtils.areSameByClass(subAnno, WholeProgramInferenceBottom.class)
                    || AnnotationUtils.areSameByClass(superAnno, Top.class)) {
                return true;
            }

            if (AnnotationUtils.areSameByClass(subAnno, UnknownClass.class)
                    || AnnotationUtils.areSameByClass(
                            superAnno, WholeProgramInferenceBottom.class)) {
                return false;
            }

            if (AnnotationUtils.areSameByClass(subAnno, Top.class)) {
                return false;
            }

            if (AnnotationUtils.areSameByClass(subAnno, ImplicitAnno.class)
                    && (AnnotationUtils.areSameByClass(superAnno, Sibling1.class)
                            || AnnotationUtils.areSameByClass(superAnno, Sibling2.class)
                            || AnnotationUtils.areSameByClass(
                                    superAnno, SiblingWithFields.class))) {
                return true;
            }

            if ((AnnotationUtils.areSameByClass(subAnno, Sibling1.class)
                            || AnnotationUtils.areSameByClass(subAnno, Sibling2.class)
                            || AnnotationUtils.areSameByClass(subAnno, ImplicitAnno.class)
                            || AnnotationUtils.areSameByClass(subAnno, SiblingWithFields.class))
                    && AnnotationUtils.areSameByClass(superAnno, Parent.class)) {
                return true;
            }

            if ((AnnotationUtils.areSameByClass(subAnno, Sibling1.class)
                            || AnnotationUtils.areSameByClass(subAnno, Sibling2.class)
                            || AnnotationUtils.areSameByClass(subAnno, ImplicitAnno.class)
                            || AnnotationUtils.areSameByClass(subAnno, SiblingWithFields.class)
                            || AnnotationUtils.areSameByClass(subAnno, Parent.class))
                    && AnnotationUtils.areSameByClass(superAnno, DefaultType.class)) {
                return true;
            }

            if (AnnotationUtils.areSameByClass(subAnno, SiblingWithFields.class)
                    && AnnotationUtils.areSameByClass(superAnno, SiblingWithFields.class)) {
                List<String> subVal1 =
                        AnnotationUtils.getElementValueArray(subAnno, "value", String.class, true);
                List<String> supVal1 =
                        AnnotationUtils.getElementValueArray(
                                superAnno, "value", String.class, true);
                String subVal2 =
                        AnnotationUtils.getElementValue(subAnno, "value2", String.class, true);
                String supVal2 =
                        AnnotationUtils.getElementValue(superAnno, "value2", String.class, true);
                return subVal1.equals(supVal1) && subVal2.equals(supVal2);
            }
            return false;
        }
    }
}
