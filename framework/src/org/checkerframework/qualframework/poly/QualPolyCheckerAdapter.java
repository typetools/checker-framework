package org.checkerframework.qualframework.poly;

import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.base.CheckerAdapter;

public class QualPolyCheckerAdapter<Q extends QualParams<?>> extends CheckerAdapter<Q> {

    /**
     * Constructs a {@link CheckerAdapter} from an underlying qualifier-based
     * {@link Checker}.
     *
     * @param underlying
     */
    public QualPolyCheckerAdapter(Checker<Q> underlying) {
        super(underlying);
    }

    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter(boolean printInvisibleQualifiers) {
        return new QualifierParameterAnnotatedTypeFormatter(getTypeMirrorConverter(), printInvisibleQualifiers);
    }

}
