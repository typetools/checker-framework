package org.checkerframework.common.returnsreceiver.framework;

import java.util.Arrays;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Enum of supported frameworks. */
public enum FrameworkSupport {
    /** AutoValue framework. */
    AUTO_VALUE {
        @Override
        public boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t) {
            ExecutableElement element = t.getElement();
            Element enclosingElement = element.getEnclosingElement();
            boolean inAutoValueBuilder =
                    AnnotationUtils.getAnnotationByName(
                                    enclosingElement.getAnnotationMirrors(),
                                    "com.google.auto.value.AutoValue.Builder")
                            != null;

            System.out.println("calling the rr checker's returns this method on: " + element);
            System.out.println("in AutoValue builder? " + inAutoValueBuilder);

            System.out.println("enclosing element: " + enclosingElement);
            for (AnnotationMirror anm : enclosingElement.getAnnotationMirrors()) {
                System.out.println("has this annotation: " + AnnotationUtils.annotationName(anm));
                if (AnnotationUtils.annotationName(anm)
                        .equals("com.google.auto.value.AutoValue.Builder")) {
                    System.out.println("did change");
                    inAutoValueBuilder = true;
                } else {
                    System.out.println("didn't change");
                    System.out.println(
                            "AnnotationUtils.annotationName(anm): "
                                    + AnnotationUtils.annotationName(anm));
                    System.out.println(AnnotationUtils.annotationName(anm).getClass());
                    System.out.println(
                            "com.google.auto.value.AutoValue.Builder"
                                    .compareTo(AnnotationUtils.annotationName(anm)));
                    char[] actualBytes = AnnotationUtils.annotationName(anm).toCharArray();
                    char[] myBytes = "com.google.auto.value.AutoValue.Builder".toCharArray();
                    System.out.println("actual bytes: " + Arrays.toString(actualBytes));
                    System.out.println("expected bytes: " + Arrays.toString(myBytes));
                }
            }

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
                                            "com.google.auto.value.AutoValue.Builder")
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
    },
    /** Lombok framework. */
    LOMBOK {
        @Override
        public boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t) {
            ExecutableElement element = t.getElement();
            Element enclosingElement = element.getEnclosingElement();
            // AnnotationUtils.containsSameByName(element.getAnnotationMirrors(), annotClassName);
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
     * @return {@code true} if the method is generated by framework and returns {@code this}
     */
    public abstract boolean returnsThis(AnnotatedTypeMirror.AnnotatedExecutableType t);
}
