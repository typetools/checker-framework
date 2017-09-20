package polyall;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import polyall.quals.H1Bot;
import polyall.quals.H1Invalid;
import polyall.quals.H1Poly;
import polyall.quals.H1S1;
import polyall.quals.H1S2;
import polyall.quals.H1Top;
import polyall.quals.H2Bot;
import polyall.quals.H2Poly;
import polyall.quals.H2S1;
import polyall.quals.H2S2;
import polyall.quals.H2Top;

public class PolyAllAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    AnnotationMirror H1S2;

    public PolyAllAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
        H1S2 = AnnotationBuilder.fromClass(elements, polyall.quals.H1S2.class);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(
                H1Top.class,
                H1S1.class,
                H1S2.class,
                H1Bot.class,
                H2Top.class,
                H2S1.class,
                H2S2.class,
                H2Bot.class,
                H1Poly.class,
                H2Poly.class,
                H1Invalid.class);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MultiGraphQualifierHierarchy(factory);
    }

    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
        if (tree.getKind() == Kind.VARIABLE
                && ((VariableTree) tree).getName().toString().contains("addH1S2")) {
            type.replaceAnnotation(H1S2);
        }
    }
}
