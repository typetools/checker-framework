package org.checkerframework.framework.flow;

import org.checkerframework.framework.type.AnnotatedTypeMirror;


/**
 * The default abstract value used in the Checker Framework.
 *
 * @author Stefan Heule
 *
 */
public class CFValue extends CFAbstractValue<CFValue> {

    public CFValue(CFAbstractAnalysis<CFValue, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        super(analysis, type);
    }

}
