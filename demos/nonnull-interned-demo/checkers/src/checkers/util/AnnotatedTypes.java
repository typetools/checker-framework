package checkers.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.SimpleAnnotatedTypeVisitor;
import checkers.types.AnnotatedTypeMirror.*;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This
 * class mimics the class {@see javax.lang.model.util.Types}.
 */
public class AnnotatedTypes {

    private ProcessingEnvironment env;

    /**
     * Constructor for {@code AnnotatedTypeUtils}
     * 
     * @param env
     *            the processing environment for this round
     */
    public AnnotatedTypes(ProcessingEnvironment env) {
        this.env = env;
    }

    /**
     * Returns the most specific base type of {@code t} that start
     * with the given {@code Element}
     * 
     * @param t         a type
     * @param element   an element
     * @return the base type of t of the given element
     */
    public AnnotatedTypeMirror asSuper(AnnotatedTypeMirror type,
            Element element) {
        return asSuper.visit(type, element);
    }

    private SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, Element> asSuper =
        new SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, Element>() {
        
        @Override
        protected AnnotatedTypeMirror defaultAction(AnnotatedTypeMirror type, Element p) {
            return type;
        }
        
        public AnnotatedTypeMirror visitTypeVariable(AnnotatedTypeVariable type, Element p) {
            // Operate on the upper bound
            return asSuper(((AnnotatedTypeVariable) type).getUpperBound(),
                    p);
        }
        
        public AnnotatedTypeMirror visitArray(AnnotatedArrayType type, Element p) {
            // Check if array component is subtype of the element
            // first
            return isSubtype(type, getAnnotatedType(p))
                    ? getAnnotatedType(p) : null;
        }
        
        public AnnotatedTypeMirror visitDeclared(AnnotatedDeclaredType type, Element p) {
            AnnotatedDeclaredType t = (AnnotatedDeclaredType) type;
            // If visited Element is the desired one, we are done
            if (t.getElement().equals(p))
                return t;

            // Visit the superclass first!
            AnnotatedTypeMirror st = t.getSupertype();
            if (st.getKind() == TypeKind.DECLARED) {
                AnnotatedTypeMirror x = asSuper(st, p);
                if (x != null)
                    return x;
            }

            // Check if defined by interfaces
            if (p.getKind().isInterface()) {
                for (AnnotatedTypeMirror si : t.getInterfaces()) {
                    AnnotatedTypeMirror x = asSuper(si, p);
                    if (x != null)
                        return x;
                }
            }

            // Couldn't find the desired base type
            return null;
        }
    };
    
    /**
     * Return the base type of t or any of its outer types that starts
     * with the given Element. If none exists, return null.
     * 
     * @param t     a type
     * @param elem   a symbol
     */
    public AnnotatedTypeMirror asOuterSuper(AnnotatedTypeMirror t,
            Element elem) {
        switch (t.getKind()) {
        case DECLARED:
            do {
                // Search among supers for a desired supertype
                AnnotatedTypeMirror s = asSuper(t, elem);
                if (s != null)
                    return s;
                // if not found immediately, try enclosing type
                // like A in A.B
                t = t.getEnclosingType();
            } while (t.getKind() == TypeKind.DECLARED);
            return null;
        case ARRAY:
            return isSubtype(t, getAnnotatedType(elem))
                    ? getAnnotatedType(elem) : null;
        case TYPEVAR:
            return asSuper(t, elem);
        default:
            return null;
        }
    }

    /**
     * This method checks for subtyping according to Java rules not
     * with respect annotated types.
     * 
     * @param t1    a type
     * @param t2    a type
     * @return true iff t1 is a subtype of t2
     */
    private boolean isSubtype(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        // FOR NOW: We just want to test for regular testing
        return env.getTypeUtils().isSubtype(t1.getUnderlyingType(),
                t2.getUnderlyingType());
    }

    /**
     * A Place-holder for a method that returns the appropriate
     * {@code AnnotatedType} for the given element
     * 
     * @param elem
     *            an element
     * @return the type associated with the element
     */
    public AnnotatedTypeMirror getAnnotatedType(Element elem) {
        throw new RuntimeException("Not Implemented yet!");
    }

    /**
     * The type of given symbol, seen as a member of t.
     * 
     * @param t    a type
     * @param sym  a symbol
     */
    public AnnotatedTypeMirror asMemberOf(AnnotatedTypeMirror t, Element elem) {
        if (ElementUtils.isStatic(elem))
            return getAnnotatedType(elem);

        // For Type Variable, operate on the upper
        if (t.getKind() == TypeKind.TYPEVAR)
            return asMemberOf(((AnnotatedTypeVariable) t).getUpperBound(),
                    elem);

        // I cannot think of why it wouldn't be a declared type!
        // Defensive Programming
        if (t.getKind() != TypeKind.DECLARED) {
            return getAnnotatedType(elem);
        }

        //
        // Basic Algorithm:
        // 1. Find the owner of the element
        // 2. Find the base type of owner (e.g. type of owner as supertype 
        //      of passed type)
        // 3. Subsitute for type variables if any exist
        TypeElement owner = ElementUtils.enclosingClass(elem);
        AnnotatedDeclaredType ownerType =
            (AnnotatedDeclaredType) getAnnotatedType(owner);

        // TODO: Potential bug if Raw type is used
        if (ElementUtils.isStatic(elem) || owner.getTypeParameters().isEmpty())
            return getAnnotatedType(elem);

        AnnotatedDeclaredType base =
            (AnnotatedDeclaredType) asOuterSuper(t, owner);

        if (base == null)
            return getAnnotatedType(elem);

        List<? extends AnnotatedTypeMirror> ownerParams = 
            ownerType.getTypeArguments();
        List<? extends AnnotatedTypeMirror> baseParams =
            base.getTypeArguments();
        if (!ownerParams.isEmpty()) {
            if (baseParams.isEmpty())
                return getAnnotatedType(elem).getErasured();
            else
                return subst(getAnnotatedType(elem), ownerParams, baseParams);
        }

        return getAnnotatedType(elem);

    }

    /**
     * Returns a new type, a copy of the passed {@code t}, with all
     * instances of {@code from} type substituted with their correspondents
     * in {@code to} and return the substituted in type.
     * 
     * @param t     the type
     * @param from  the from types
     * @param to    the to types
     * @return  the new type after substitutions
     */
    public AnnotatedTypeMirror subst(AnnotatedTypeMirror t,
            List<? extends AnnotatedTypeMirror> from,
            List<? extends AnnotatedTypeMirror> to) {
        assert from.size() == to.size();
        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings =
            new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

        for (int i = 0; i < from.size(); ++i) {
            mappings.put(from.get(i), to.get(i));
        }
        return t.subsitute(mappings);
    }


    /**
     * Returns a new type, a copy of the passed {@code t}, with all
     * instances of {@code mappings.keySet()} type substituted with 
     * their correspondents in {@code to} and return the substituted 
     * in type.
     * 
     * @param t     the type
     * @param mappings  the mapping for the types
     * @return  the new type after substitutions
     */
    public List<? extends AnnotatedTypeMirror> subst(
            List<? extends AnnotatedTypeMirror> ts,
            Map<? extends AnnotatedTypeMirror, ? extends AnnotatedTypeMirror> mappings) {
        List<AnnotatedTypeMirror> rTypes = new ArrayList<AnnotatedTypeMirror>();
        for (AnnotatedTypeMirror type : ts) 
            rTypes.add(type.subsitute(mappings));
        return rTypes;
    }

    /**
     * Returns a deep copy of the passed type
     * 
     * @param type  the annotated type to be copied
     * @return a deep copy of the passed type
     */
    public AnnotatedTypeMirror deepCopy(AnnotatedTypeMirror type) {
        // TODO: Test this, specify behaviour
        return type.subsitute(Collections.<AnnotatedTypeMirror, 
                AnnotatedTypeMirror>emptyMap());
    }
    
    /**
     * 
     */
    public AnnotatedTypeMirror getIteratedType(AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return ((AnnotatedArrayType) type).getComponentType();
        } else if (type.getKind() == TypeKind.DECLARED) {
            Element iterableElement = env.getElementUtils().getTypeElement("java.lang.Iterable");
            AnnotatedDeclaredType dt = (AnnotatedDeclaredType) asSuper(type, iterableElement);
            if (dt == null)
                return null;
            else if (dt.getTypeArguments().isEmpty()) {
                // was erased
                return this.getAnnotatedType(env.getElementUtils().getTypeElement("java.lang.Object"));
            } else {
                return dt.getTypeArguments().get(0);
            }
        } else {
            // Type is not iterable
            return null;
        }
    }

}
