package h1h2checker;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import h1h2checker.quals.H1Bot;
import h1h2checker.quals.H1Invalid;
import h1h2checker.quals.H1Poly;
import h1h2checker.quals.H1S1;
import h1h2checker.quals.H1S2;
import h1h2checker.quals.H1Top;
import h1h2checker.quals.H2Bot;
import h1h2checker.quals.H2Poly;
import h1h2checker.quals.H2S1;
import h1h2checker.quals.H2S2;
import h1h2checker.quals.H2Top;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;

public class H1H2AnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    AnnotationMirror H1S2;

    public H1H2AnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
        H1S2 = AnnotationBuilder.fromClass(elements, h1h2checker.quals.H1S2.class);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(
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
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
        if (tree.getKind() == Kind.VARIABLE
                && ((VariableTree) tree).getName().toString().contains("addH1S2")) {
            type.replaceAnnotation(H1S2);
        }
    }
}
