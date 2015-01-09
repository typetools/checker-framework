package org.checkerframework.qualframework.poly;

/**
 * // TODO:: Delete this
 */
public interface QualParamsVisitor<Q, R, P> {

    R visit(Q qual, P p);
    R visit(QualParams<Q> params, P p);
    R visit(PolyQual<Q> params, P p);
    R visit(Wildcard<Q> params, P p);
}
