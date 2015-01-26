package org.checkerframework.framework.util;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;
import java.util.Set;

/**
 * Class to hold a utility method needed by TypeFromExpression and BaseTypeVisitor.
 */
public class ConstructorReturnUtil {

    /**
     * keepOnlyExplicitConstructorAnnotations modifies returnType to
     * keep only annotations explicitly on the constructor
     * and annotations resulting from resolution of polymorphic qualifiers.
     *
     * @param atypeFactory type factory
     * @param returnType The return type for the constructor. No polymorphic qualifiers should have been substituted.
     * @param constructor The ATM for the constructor.
     */
    public static void keepOnlyExplicitConstructorAnnotations(AnnotatedTypeFactory atypeFactory,
            AnnotatedDeclaredType returnType,
            AnnotatedTypeMirror.AnnotatedExecutableType constructor) {

        // TODO: There will be a nicer way to access this in 308 soon.
        List<Attribute.TypeCompound> decall = ((Symbol)constructor.getElement()).getRawTypeAttributes();
        Set<AnnotationMirror> decret = AnnotationUtils.createAnnotationSet();
        for (Attribute.TypeCompound da : decall) {
            if (da.position.type == com.sun.tools.javac.code.TargetType.METHOD_RETURN) {
                decret.add(da);
            }
        }

        // Collect all polymorphic qualifiers; we should substitute them.
        Set<AnnotationMirror> polys = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : returnType.getAnnotations()) {
            if (QualifierPolymorphism.isPolymorphicQualified(anno)) {
                polys.add(anno);
            }
        }

        for (AnnotationMirror cta : constructor.getReturnType().getAnnotations()) {
            AnnotationMirror ctatop = atypeFactory.getQualifierHierarchy().getTopAnnotation(cta);
            if (atypeFactory.isSupportedQualifier(cta) &&
                    !returnType.isAnnotatedInHierarchy(cta)) {
                for (AnnotationMirror fromDecl : decret) {
                    if (atypeFactory.isSupportedQualifier(fromDecl) &&
                            AnnotationUtils.areSame(ctatop,
                                    atypeFactory.getQualifierHierarchy().getTopAnnotation(fromDecl))) {
                        returnType.addAnnotation(cta);
                        break;
                    }
                }
            }

            // Go through the polymorphic qualifiers and see whether
            // there is anything left to replace.
            for (AnnotationMirror pa : polys) {
                if (AnnotationUtils.areSame(ctatop,
                        atypeFactory.getQualifierHierarchy().getTopAnnotation(pa))) {
                    returnType.replaceAnnotation(cta);
                    break;
                }
            }
        }

    }
}
