package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Some Elements are members of a list (formal method parameters and type parameters). This class
 * ensures that the targeted annotations passed by handleTargeted -- see {@link
 * TargetedElementAnnotationApplier} -- only include those with a position that matches the index
 * returned by getElementIndex.
 */
abstract class IndexedElementAnnotationApplier extends TargetedElementAnnotationApplier {

    public IndexedElementAnnotationApplier(AnnotatedTypeMirror type, Element element) {
        super(type, element);
    }

    /** The index of element in the list of elements that contains it. */
    public abstract int getElementIndex();

    /**
     * A TypeAnnotationPosition has a number of different indexes (type_index, bound_index,
     * param_index) Return the index we are interested in. If offsetting needs to be done it should
     * be done in getElementIndex not here. (see ElementAnnotationUtils.getBoundIndexOffset )
     *
     * @param anno an annotation we might wish to apply
     * @return the index value this applier compares against the getElementIndex
     */
    public abstract int getTypeCompoundIndex(final Attribute.TypeCompound anno);

    @Override
    protected Map<TargetClass, List<Attribute.TypeCompound>> sift(
            Iterable<Attribute.TypeCompound> typeCompounds) {
        final Map<TargetClass, List<Attribute.TypeCompound>> targetClassToAnnos =
                super.sift(typeCompounds);

        final List<Attribute.TypeCompound> targeted = targetClassToAnnos.get(TargetClass.TARGETED);
        final List<Attribute.TypeCompound> valid = targetClassToAnnos.get(TargetClass.VALID);

        final int paramIndex = getElementIndex();

        // filter out annotations in targeted that don't have the correct parameter index. (i.e the
        // one's that are on the same method but don't pertain to the parameter element being
        // processed, see class comments ).  Place these annotations into the valid list.
        int i = 0;
        while (i < targeted.size()) {
            if (getTypeCompoundIndex(targeted.get(i)) != paramIndex) {
                valid.add(targeted.remove(i));
            } else {
                ++i;
            }
        }

        return targetClassToAnnos;
    }
}
