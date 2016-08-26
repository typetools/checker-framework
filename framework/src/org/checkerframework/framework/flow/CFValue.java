package org.checkerframework.framework.flow;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

/**
 * The default abstract value used in the Checker Framework.
 *
 * @author Stefan Heule
 *
 */
public class CFValue extends CFAbstractValue<CFValue> {

    public CFValue(
            CFAbstractAnalysis<CFValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }

    public CFValue(CFAbstractAnalysis<CFValue, ?, ?> analysis, AnnotatedTypeMirror type) {
        super(analysis, type);
    }
}
