package org.checkerframework.framework.type;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.element.ClassTypeParamApplier;
import org.checkerframework.framework.util.element.MethodApplier;
import org.checkerframework.framework.util.element.MethodTypeParamApplier;
import org.checkerframework.framework.util.element.ParamApplier;
import org.checkerframework.framework.util.element.SuperTypeApplier;
import org.checkerframework.framework.util.element.TypeDeclarationApplier;
import org.checkerframework.framework.util.element.TypeVarUseApplier;
import org.checkerframework.framework.util.element.VariableApplier;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;

/**
 * Utility methods for adding the annotations that are stored in an Element to the type that
 * represents that element (or a use of that Element).
 *
 * <p>In a way, this class is a hack: the Type representation for the Elements should contain all
 * annotations that we want. However, due to javac bugs
 * http://mail.openjdk.java.net/pipermail/type-annotations-dev/2013-December/001449.html decoding
 * the type annotations from the Element is necessary.
 *
 * <p>Even once these bugs are fixed, this class might be useful: in TypesIntoElements it is easy to
 * add additional annotations to the element and have them stored in the bytecode by the compiler.
 * It would be more work (and might not work in the end) to instead modify the Type directly. The
 * interaction between TypeFromElement and TypesIntoElements allows us to write the defaulted
 * annotations into the Element and have them read later by other parts.
 */
public class ElementAnnotationApplier {

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
        if (element == null) {
            throw new BugInCF("ElementAnnotationUtil.apply: element cannot be null");

        } else if (TypeVarUseApplier.accepts(type, element)) {
            TypeVarUseApplier.apply(type, element, typeFactory);

        } else if (VariableApplier.accepts(type, element)) {
            VariableApplier.apply(type, element);

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
        SuperTypeApplier.annotateSupers(supertypes, subtypeElement);
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
            if (parentTree != null && parentTree.getKind() == Kind.LAMBDA_EXPRESSION) {
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
        return (((Symbol) enclosure).kind == com.sun.tools.javac.code.Kinds.NIL);
    }
}
