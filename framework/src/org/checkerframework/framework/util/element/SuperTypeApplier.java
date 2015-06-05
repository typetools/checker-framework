package org.checkerframework.framework.util.element;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

/**
 *  When discovering supertypes of an AnnotatedTypeMirror we want to annotate each supertype with the annotations
 *  of the subtypes class declaration.  This class provides static methods to do this for a list of supertypes.
 *  An instance of this class handles ONE supertype.
 */
public class SuperTypeApplier extends IndexedElementAnnotationApplier {

    /**
     * Annotates each supertype with annotations from subtypeElement's extends/implements clauses.
     * @param supertypes Supertypes to annotate
     * @param subtypeElement Element that may have annotations to apply to supertypes
     */
    public static void annotateSupers(List<AnnotatedTypeMirror.AnnotatedDeclaredType> supertypes, TypeElement subtypeElement) {

        final boolean isInterface = subtypeElement.getSuperclass().getKind() == TypeKind.NONE;

        final int typeOffset = isInterface ? 0 : -1;

        for (int i= 0; i < supertypes.size(); i++) {
            final AnnotatedTypeMirror supertype = supertypes.get(i);
            final int typeIndex = i + typeOffset;

            (new SuperTypeApplier(supertype, subtypeElement, typeIndex)).extractAndApply();
        }
    }

    private final Symbol.ClassSymbol subclassSymbol;

    /**
     * The type_index of the supertype being annotated.
     *
     * Note: Due to the semantics of TypeAnnotationPosition, type_index/index numbering works as follows:
     *
     * If subtypeElement represents a class and not an interface:
     *    then
     *      the first member of supertypes represents the object and the relevant type_index = -1
     *      interface indices are offset by 1.
     *
     *    else
     *      all members of supertypes represent interfaces and their indices == their index in the supertypes list
     *
     */
    private final int index;

    /**
     * Note: This is not meant to be used in apply explicitly unlike all other AnnotationAppliers
     * it is intended to be used for annotate super types via the static annotateSuper method, hence the private
     * constructor
     */
    SuperTypeApplier(final AnnotatedTypeMirror supertype, final TypeElement subclassElement, final int index) {
        super(supertype, subclassElement);
        this.subclassSymbol = (Symbol.ClassSymbol) subclassElement;
        this.index = index;
    }

    /**
     * @return The type_index that should represent supertype
     */
    @Override
    public int getElementIndex() {
        return index;
    }

    /**
     * @return The type_index of anno's TypeAnnotationPosition
     */
    @Override
    public int getTypeCompoundIndex(Attribute.TypeCompound anno) {
        return anno.getPosition().type_index;
    }

    /**
     * @return TargetType.CLASS_EXTENDS
     */
    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[]{ TargetType.CLASS_EXTENDS };
    }

    /**
     * @return TargetType.CLASS_TYPE_PARAMETER, TargetType.CLASS_TYPE_PARAMETER_BOUND
     */
    @Override
    protected TargetType[] validTargets() {
        return new TargetType[]{ TargetType.CLASS_TYPE_PARAMETER,
                                 TargetType.CLASS_TYPE_PARAMETER_BOUND };
    }

    /**
     * @return The TypeCompounds (annotations) of the subclass
     */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return subclassSymbol.getRawTypeAttributes();
    }

    @Override
    protected void handleTargeted(List<Attribute.TypeCompound> targeted) {
        ElementAnnotationUtil.annotateViaTypeAnnoPosition(type, targeted);
    }

    @Override
    protected boolean isAccepted() {
        return true;
    }
}
