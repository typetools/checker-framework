package org.checkerframework.common.returnsrcvr.framework;

import com.google.auto.value.AutoValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;

/**
 * AutoValue support for returns receiver checker. Checks if the current class or its super class is
 * an AutoValue Builder
 */
public class AutoValueSupport implements FrameworkSupport {

    @Override
    public boolean knownToReturnThis(AnnotatedTypeMirror.AnnotatedExecutableType t) {

        ExecutableElement element = t.getElement();

        Element enclosingElement = element.getEnclosingElement();

        boolean inAutoValueBuilder =
                FrameworkSupportUtils.hasAnnotation(enclosingElement, AutoValue.Builder.class);

        if (!inAutoValueBuilder) {
            // see if superclass is an AutoValue Builder, to handle generated code
            TypeMirror superclass = ((TypeElement) enclosingElement).getSuperclass();
            // if enclosingType is an interface, the superclass has TypeKind NONE
            if (!(superclass.getKind() == TypeKind.NONE)) {
                // update enclosingElement to be for the superclass for this case
                enclosingElement = TypesUtils.getTypeElement(superclass);
                inAutoValueBuilder =
                        enclosingElement.getAnnotation(AutoValue.Builder.class) != null;
            }
        }

        if (inAutoValueBuilder) {
            AnnotatedTypeMirror returnType = t.getReturnType();
            return returnType != null
                    && enclosingElement.equals(
                            TypesUtils.getTypeElement(returnType.getUnderlyingType()));
        }

        return false;
    }
}
