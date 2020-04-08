package org.checkerframework.common.returnsreceiver;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Enum of supported fluent API generators. For such generators, the checker can automatically
 * add @This annotations on method return types in the generated code.
 */
public enum FluentAPIGenerator {
    AUTO_VALUE {

        private final String AUTO_VALUE_BUILDER = getAutoValueBuilderCanonicalName();

        @Override
        public boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t) {
            ExecutableElement element = t.getElement();
            Element enclosingElement = element.getEnclosingElement();
            boolean inAutoValueBuilder =
                    AnnotationUtils.getAnnotationByName(
                                    enclosingElement.getAnnotationMirrors(), AUTO_VALUE_BUILDER)
                            != null;

            if (!inAutoValueBuilder) {
                // see if superclass is an AutoValue Builder, to handle generated code
                TypeMirror superclass = ((TypeElement) enclosingElement).getSuperclass();
                // if enclosingType is an interface, the superclass has TypeKind NONE
                if (superclass.getKind() != TypeKind.NONE) {
                    // update enclosingElement to be for the superclass for this case
                    enclosingElement = TypesUtils.getTypeElement(superclass);
                    inAutoValueBuilder =
                            AnnotationUtils.getAnnotationByName(
                                            enclosingElement.getAnnotationMirrors(),
                                            AUTO_VALUE_BUILDER)
                                    != null;
                }
            }

            if (inAutoValueBuilder) {
                AnnotatedTypeMirror returnType = t.getReturnType();
                if (returnType == null) throw new RuntimeException("Return type cannot be null");
                return enclosingElement.equals(
                        TypesUtils.getTypeElement(returnType.getUnderlyingType()));
            }
            return false;
        }

        private String getAutoValueBuilderCanonicalName() {
            String com = "com";
            return com + "." + "google.auto.value.AutoValue.Builder";
        }
    },
    LOMBOK {
        @Override
        public boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t) {
            ExecutableElement element = t.getElement();
            Element enclosingElement = element.getEnclosingElement();
            boolean inLombokBuilder =
                    (AnnotationUtils.containsSameByName(
                                            enclosingElement.getAnnotationMirrors(),
                                            "lombok.Generated")
                                    || AnnotationUtils.containsSameByName(
                                            element.getAnnotationMirrors(), "lombok.Generated"))
                            && enclosingElement.getSimpleName().toString().endsWith("Builder");

            if (inLombokBuilder) {
                AnnotatedTypeMirror returnType = t.getReturnType();
                if (returnType == null) throw new RuntimeException("Return type cannot be null");
                return enclosingElement.equals(
                        TypesUtils.getTypeElement(returnType.getUnderlyingType()));
            }
            return false;
        }
    };

    /**
     * @param t the method to check
     * @return {@code true} if the method was created by this generator and returns {@code this}
     */
    public abstract boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t);
}
