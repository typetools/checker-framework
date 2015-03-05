package org.checkerframework.checker.interning;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.PolyInterned;
import org.checkerframework.checker.interning.qual.UnknownInterned;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;

/**
 * An {@link AnnotatedTypeFactory} that accounts for the properties of the
 * Interned type system. This type factory will add the {@link Interned}
 * annotation to a type if the input:
 *
 * <ol>
 * <li value="1">is a String literal
 * <li value="2">is a class literal
 * <li value="3">has an enum type
 * <li value="4">has a primitive type
 * <li value="5">has the type java.lang.Class
 * </ol>
 *
 * This factory extends {@link BaseAnnotatedTypeFactory} and inherits its
 * functionality, including: flow-sensitive qualifier inference, qualifier
 * polymorphism (of {@link PolyInterned}), implicit annotations via
 * {@link ImplicitFor} on {@link Interned} (to handle cases 1, 2, 4), and
 * user-specified defaults via {@link DefaultQualifier}.
 * Case 5 is handled by the stub library.
 */
public class InterningAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The {@link Interned} annotation. */
    final AnnotationMirror INTERNED, TOP;

    /**
     * Creates a new {@link InterningAnnotatedTypeFactory} that operates on a
     * particular AST.
     *
     * @param checker the checker to use
     */
    public InterningAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.INTERNED = AnnotationUtils.fromClass(elements, Interned.class);
        this.TOP = AnnotationUtils.fromClass(elements, UnknownInterned.class);

        // If you update the following, also update ../../../manual/interning-checker.tex .
        addAliasedAnnotation(com.sun.istack.internal.Interned.class, INTERNED);

        this.postInit();

        // The null literal is interned -> make Void interned also.
        addTypeNameImplicit(java.lang.Void.class, INTERNED);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new InterningTreeAnnotator(this)
        );
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                new InterningTypeAnnotator(this),
                super.createTypeAnnotator()
        );
    }

    @Override
    public void annotateImplicit(Element element, AnnotatedTypeMirror type) {
        if (!type.isAnnotatedInHierarchy(INTERNED) && ElementUtils.isCompileTimeConstant(element))
            type.addAnnotation(INTERNED);
        super.annotateImplicit(element, type);
    }

    /**
     * A class for adding annotations based on tree
     */
    private class InterningTreeAnnotator  extends TreeAnnotator {

        InterningTreeAnnotator(InterningAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isCompileTimeString(node)) {
                type.replaceAnnotation(INTERNED);
            } else if (TreeUtils.isStringConcatenation(node)) {
                type.replaceAnnotation(TOP);
            } else if ((type.getKind().isPrimitive()) ||
                    node.getKind() == Tree.Kind.EQUAL_TO ||
                    node.getKind() == Tree.Kind.NOT_EQUAL_TO) {
                type.replaceAnnotation(INTERNED);
            } else {
                type.replaceAnnotation(TOP);
            }
            return super.visitBinary(node, type);
        }

        /* Compound assignments never result in an interned result.
         */
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
          type.replaceAnnotation(TOP);
          return super.visitCompoundAssignment(node, type);
        }
    }

    /**
     * A class for adding annotations to a type after initial type resolution.
     */
    private class InterningTypeAnnotator extends TypeAnnotator {

        /** Creates an {@link InterningTypeAnnotator} for the given checker. */
        InterningTypeAnnotator(InterningAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType t, Void p) {

            // case 3: Enum types, and the Enum class itself, are interned
            Element elt = t.getUnderlyingType().asElement();
            assert elt != null;
            if (elt.getKind() == ElementKind.ENUM) {
                t.replaceAnnotation(INTERNED);
            } else if (InterningAnnotatedTypeFactory.this.fromElement(elt).hasAnnotation(INTERNED)) {
                // If the class/interface has an @Interned annotation, use it.
                t.replaceAnnotation(INTERNED);
            }

            return super.visitDeclared(t, p);
        }
    }

    @Override
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type) {
        AnnotatedPrimitiveType primitive = super.getUnboxedType(type);
        primitive.replaceAnnotation(INTERNED);
        return primitive;
    }
}
