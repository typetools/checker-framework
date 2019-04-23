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
        // The constructor result is defaulted to bottom, since that isn't top an error would always
        // be issued if super is called.  Don't issue the error so that this checker can be run on
        // all-systems tests.
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        // Use bottom since that's the default.  This avoids errors when exception parameters are
        // used in all-systems tests.
        Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
        set.add(atypeFactory.BOTTOM);
        return set;
    }
}
