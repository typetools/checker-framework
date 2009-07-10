package checkers.interning;

import javax.lang.model.element.*;

import checkers.basetype.BaseTypeChecker;
import checkers.interning.quals.*;
import checkers.javari.quals.Mutable;
import checkers.quals.DefaultQualifier;
import checkers.quals.ImplicitFor;
import checkers.types.*;
import static checkers.types.AnnotatedTypeMirror.*;

import com.sun.source.tree.*;

/**
 * An {@link AnnotatedTypeFactory} that accounts for the properties of the
 * Interned type system. This type factory will add the {@link Interned}
 * annotation to a type if the input:
 *
 * 1. is a String literal
 * 2. is a class literal
 * 3. has an enum type
 * 4. has a primitive type
 * 5. has the type java.lang.Class
 * 6. is a call to the method {@link String#intern()}. The method is the only
 *    interning method in the JDK. This class hard-codes handling of it, so
 *    there is no need for an annotated JDK.
 *
 * This factory extends {@link BasicAnnotatedTypeFactory} and inherits its
 * functionalities: flow-sensitive qualifier inference, qualifier polymorphism
 * (of {@link PolyInterned}), implicit annotations via {@link ImplicitFor}
 * (to handle cases 1, 4), and user-specified defaults via {@link DefaultQualifier}.
 */
public class InterningAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<InterningChecker> {

    /** The {@link Interned} annotation. */
    final AnnotationMirror INTERNED;

    /**
     * Creates a new {@link InterningAnnotatedTypeFactory} that operates on a
     * particular AST.
     *
     * @param checker the checker to use
     * @param root the AST on which this type factory operates
     */
    public InterningAnnotatedTypeFactory(InterningChecker checker,
        CompilationUnitTree root) {
        super(checker, root);
        this.INTERNED = annotations.fromClass(Interned.class);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(InterningChecker checker) {
        return new InterningTypeAnnotator(checker);
    }

    /**
     * A class for adding annotations to a type after initial type resolution.
     */
    private class InterningTypeAnnotator extends TypeAnnotator {

        /** Creates an {@link InterningTypeAnnotator} for the given checker. */
        InterningTypeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType t, ElementKind p) {

            // cases 2,3,5: Enum types and class, interned
            Element elt = t.getUnderlyingType().asElement();
            assert elt != null;
            if (elt.getKind() == ElementKind.ENUM)
                t.addAnnotation(INTERNED);

            return super.visitDeclared(t, p);
        }
    }

    @Override
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type) {
        AnnotatedPrimitiveType primitive = super.getUnboxedType(type);
        primitive.addAnnotation(INTERNED);
        return primitive;
    }
    
    protected void annotateInheritedFromClass(@Mutable AnnotatedTypeMirror type) {
      InheritedFromClassAnnotator.INSTANCE.visit(type, this);
  }

}
