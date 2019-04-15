package typedecldefault;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;

public class TypeDeclDefaultVisitor extends BaseTypeVisitor<TypeDeclDefaultAnnotatedTypeFactory> {

    public TypeDeclDefaultVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // skip
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
        set.add(atypeFactory.BOTTOM);
        return set;
    }
}
