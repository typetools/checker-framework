package typedecldefault;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import typedecldefault.quals.*;

public class TypeDeclDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    AnnotationMirror BOTTOM = AnnotationBuilder.fromClass(elements, TypeDeclDefaultBottom.class);

    public TypeDeclDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(
                Arrays.asList(
                        TypeDeclDefaultTop.class,
                        TypeDeclDefaultBottom.class,
                        PolyTypeDeclDefault.class));
    }
}
