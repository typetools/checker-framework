package org.checkerframework.framework.type;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.element.ClassTypeParamApplier;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.ErrorTypeKindException;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;
import org.checkerframework.framework.util.element.MethodApplier;
import org.checkerframework.framework.util.element.MethodTypeParamApplier;
import org.checkerframework.framework.util.element.ParamApplier;
import org.checkerframework.framework.util.element.SuperTypeApplier;
import org.checkerframework.framework.util.element.TypeDeclarationApplier;
import org.checkerframework.framework.util.element.TypeVarUseApplier;
import org.checkerframework.framework.util.element.VariableApplier;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * Utility methods for adding the annotations that are stored in an Element to the type that
 * represents that element (or a use of that Element).
 *
 * <p>In a way, this class is a hack: the Type representation for the Elements should contain all
 * annotations that we want. However, due to <a
 * href="http://mail.openjdk.java.net/pipermail/type-annotations-dev/2013-December/001449.html">javac
 * bugs</a> decoding the type annotations from the Element is necessary.
 *
 * <p>Even once these bugs are fixed, this class might be useful: in TypesIntoElements it is easy to
 * add additional annotations to the element and have them stored in the bytecode by the compiler.
 * It would be more work (and might not work in the end) to instead modify the Type directly. The
 * interaction between TypeFromElement and TypesIntoElements allows us to write the defaulted
 * annotations into the Element and have them read later by other parts.
 */
public final class ElementAnnotationApplier {

    // Class cannot be instantiated.
    private ElementAnnotationApplier() {
        throw new AssertionError("Class ElementAnnotationApplier cannot be instantiated.");
    }

    /**
     * Add all of the relevant annotations stored in Element to type. This includes both top-level
     * primary annotations and nested annotations. For the most part the TypeAnnotationPosition of
     * the element annotations are used to locate the annotation in the right AnnotatedTypeMirror
     * location though the individual applier classes may have special rules (such as those for
     * upper and lower bounds and intersections).
     *
     * <p>Note: Element annotations come from two sources.
     *
     * <ol>
     *   <li>Annotations found on elements may represent those in source code or bytecode; these are
     *       added to the element by the compiler.
     *   <li>The annotations may also represent those that were inferred or defaulted by the Checker
     *       Framework after a previous call to this method. The Checker Framework will store any
     *       annotations on declarations back into the elements that represent them (see {@link
     *       org.checkerframework.framework.type.TypesIntoElements}). Subsequent, calls to apply
     *       will encounter these annotations on the provided element.
     * </ol>
     *
     * Note: This is not the ONLY place that annotations are explicitly added to types. See {@link
     * org.checkerframework.framework.type.TypeFromTree}.
     *
     * @param type the type to which we wish to apply the element's annotations
     * @param element an element that possibly contains annotations
     * @param typeFactory the typeFactory used to create the given type
     */
    public static void apply(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory) {
        try {
            try {
                applyInternal(type, element, typeFactory);
            } catch (UnexpectedAnnotationLocationException e) {
                reportInvalidLocation(element, typeFactory);
            }
            // Also copy annotations from type parameters to their uses.
            new TypeVarAnnotator().visit(type, typeFactory);
        } catch (ErrorTypeKindException e) {
            // Do nothing if an ERROR TypeKind was found.
            // This is triggered by Issue #244.
        }
    }

    /** Issues an "invalid.annotation.location.bytecode warning. */
    private static void reportInvalidLocation(Element element, AnnotatedTypeFactory typeFactory) {
        Element report = element;
        if (element.getEnclosingElement().getKind() == ElementKind.METHOD) {
            report = element.getEnclosingElement();
        }
        // There's a bug in Java 8 compiler that creates bad bytecode such that an
        // annotation on a lambda parameter is applied to a method parameter. (This bug has
        // been fixed in Java 9.) If this happens, then the location could refer to a
        // location, such as a type argument, that doesn't exist. Since Java 8 bytecode
        // might be on the classpath, catch this exception and ignore the type.
        // TODO: Issue an error if this annotation is from Java 9+ bytecode.
        if (!typeFactory.checker.hasOption("ignoreInvalidAnnotationLocations")) {
            typeFactory.checker.reportWarning(
                    element,
                    "invalid.annotation.location.bytecode",
                    ElementUtils.getQualifiedName(report));
        }
    }

    /** Same as apply except that annotations aren't copied from type parameter declarations. */
    private static void applyInternal(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory)
            throws UnexpectedAnnotationLocationException {

        if (element == null) {
            throw new BugInCF("ElementAnnotationUtil.apply: element cannot be null");

        } else if (TypeVarUseApplier.accepts(type, element)) {
            TypeVarUseApplier.apply(type, element, typeFactory);

        } else if (VariableApplier.accepts(type, element)) {
            if (element.getKind() != ElementKind.LOCAL_VARIABLE) {
                // For local variables we have the source code,
                // so there is no need to look at the Element.
                // This is needed to avoid a bug in the JDK:
                // https://github.com/eisop/checker-framework/issues/14
                VariableApplier.apply(type, element);
            }

        } else if (MethodApplier.accepts(type, element)) {
            MethodApplier.apply(type, element, typeFactory);

        } else if (TypeDeclarationApplier.accepts(type, element)) {
            TypeDeclarationApplier.apply(type, element, typeFactory);

        } else if (ClassTypeParamApplier.accepts(type, element)) {
            ClassTypeParamApplier.apply((AnnotatedTypeVariable) type, element, typeFactory);

        } else if (MethodTypeParamApplier.accepts(type, element)) {
            MethodTypeParamApplier.apply((AnnotatedTypeVariable) type, element, typeFactory);

        } else if (ParamApplier.accepts(type, element)) {
            ParamApplier.apply(type, element, typeFactory);

        } else if (isCaptureConvertedTypeVar(element)) {
            // Types resulting from capture conversion cannot have explicit annotations

        } else if (ElementUtils.isBindingVariable(element)) {
            // TODO: verify that there are no type use annotations that would need decoding

        } else {
            throw new BugInCF(
                    "ElementAnnotationUtil.apply: illegal argument: "
                            + element
                            + " ["
                            + element.getKind()
                            + "]"
                            + " with type "
                            + type);
        }
    }

    /**
     * Annotate the list of supertypes using the annotations on the TypeElement representing a class
     * or interface.
     *
     * @param supertypes types representing supertype declarations of TypeElement
     * @param subtypeElement an element representing the declaration of the class which is a subtype
     *     of supertypes
     */
    public static void annotateSupers(
            List<AnnotatedDeclaredType> supertypes, TypeElement subtypeElement) {
        try {
            SuperTypeApplier.annotateSupers(supertypes, subtypeElement);
        } catch (UnexpectedAnnotationLocationException e) {
            reportInvalidLocation(subtypeElement, supertypes.get(0).atypeFactory);
        }
    }

    /**
     * Helper method to get the lambda tree for ParamApplier. Ideally, this method would be located
     * in ElementAnnotationUtil but since AnnotatedTypeFactory.declarationFromElement is protected,
     * it has been placed here.
     *
     * @param varEle the element that may represent a lambda's parameter
     * @return a LambdaExpressionTree if the varEle represents a parameter in a lambda expression,
     *     otherwise null
     */
    public static Pair<VariableTree, LambdaExpressionTree> getParamAndLambdaTree(
            VariableElement varEle, AnnotatedTypeFactory typeFactory) {
        VariableTree paramDecl = (VariableTree) typeFactory.declarationFromElement(varEle);

        if (paramDecl != null) {
            final Tree parentTree = typeFactory.getPath(paramDecl).getParentPath().getLeaf();
            if (parentTree != null && parentTree.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
                return Pair.of(paramDecl, (LambdaExpressionTree) parentTree);
            }
        }

        return null;
    }

    /**
     * Was the type passed in generated by capture conversion.
     *
     * @param element the element which type represents
     * @return true if type was generated via capture conversion false otherwise
     */
    private static boolean isCaptureConvertedTypeVar(final Element element) {
        final Element enclosure = element.getEnclosingElement();
        return (((Symbol) enclosure).kind == com.sun.tools.javac.code.Kinds.Kind.NIL);
    }

    /**
     * Annotates uses of type variables with annotation written explicitly on the type parameter
     * declaration and/or its upper bound.
     */
    private static class TypeVarAnnotator extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {
        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeFactory factory) {
            TypeParameterElement tpelt =
                    (TypeParameterElement) type.getUnderlyingType().asElement();

            if (type.getAnnotations().isEmpty()
                    && type.getUpperBound().getAnnotations().isEmpty()
                    && tpelt.getEnclosingElement().getKind() != ElementKind.TYPE_PARAMETER) {
                try {
                    ElementAnnotationApplier.applyInternal(type, tpelt, factory);
                } catch (UnexpectedAnnotationLocationException e) {
                    // The above is the second call to applyInternal on this type and element, so
                    // any errors were already reported by the first call. (See the only use of this
                    // class.)
                }
            }
            return super.visitTypeVariable(type, factory);
        }
    }
}
