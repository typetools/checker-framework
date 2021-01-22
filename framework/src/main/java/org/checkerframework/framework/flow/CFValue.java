package org.checkerframework.framework.flow;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.plumelib.util.UniqueId;

/**
 * The default abstract value used in the Checker Framework: a set of annotations and a TypeMirror.
 */
public class CFValue extends CFAbstractValue<CFValue> implements UniqueId {

    public CFValue(
            CFAbstractAnalysis<CFValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
        //        try {
        //            throw new RuntimeException();
        //        } catch (Exception e) {
        //            System.out.println("creating a new CFValue with uid: " + uid);
        //            //e.printStackTrace();
        //        }
    }

    /** The unique ID for the next-created object. */
    static final AtomicLong nextUid = new AtomicLong(0);
    /** The unique ID of this object. */
    final long uid = nextUid.getAndIncrement();

    /**
     * Returns the unique ID of this object.
     *
     * @return the unique ID of this object.
     */
    @Override
    public long getUid() {
        return uid;
    }

    /**
     * Returns the string representation.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return super.toString() + "#" + uid;
    }
}
