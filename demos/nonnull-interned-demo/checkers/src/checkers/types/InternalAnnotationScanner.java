package checkers.types;

import checkers.quals.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.tools.javac.comp.AnnotationTarget;

/**
 * A scanner for obtaining groups of annotations from a source tree.
 */
class InternalAnnotationScanner extends SimpleTreeVisitor<InternalAnnotationGroup, Void> {

    /** The syntax tree root. */
    private final CompilationUnitTree root;

    /** The Trees instance to use when scanning. */
    private final Trees trees;

    /** The processing environment. */
    private final ProcessingEnvironment env;

    /** The factory to use to get annotation data. */
    private final AnnotationFactory factory;

    /**
     * Creates a new instance of InternalAnnotationScanner.
     *
     * @param root the source tree root
     * @param env the current processing environment
     * @param trees the {@link Trees} instance to use for scanning
     */
    public InternalAnnotationScanner(CompilationUnitTree root,
            ProcessingEnvironment env, Trees trees) {
        this.root = root;
        this.trees = trees;
        this.env = env;
        this.factory = new AnnotationFactory(env);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup defaultAction(Tree node, Void p) {
        return InternalAnnotationGroup.EMPTY;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitClass(ClassTree node, Void p) {
        @Nullable Element e = InternalUtils.symbol(node);

        return new InternalAnnotationGroup(factory.createAnnotations(e), e);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitIdentifier(IdentifierTree node, Void p) {
        @Nullable Element e = InternalUtils.symbol(node);
        assert e != null; /*nninvariant*/

        List<InternalAnnotation> annotations = new ArrayList<InternalAnnotation>();
        for (InternalAnnotation an : factory.createAnnotations(e)) {
            if (e.getKind() != ElementKind.FIELD &&
                    e.getKind() != ElementKind.LOCAL_VARIABLE)
                annotations.add(an);

            @Nullable AnnotationTarget target = an.getTarget();
            if (target == null) continue; /*nnbug*/
            switch (target.type) {
            case LOCAL_VARIABLE:
            case LOCAL_VARIABLE_GENERIC_OR_ARRAY:

            case FIELD_GENERIC_OR_ARRAY:
                // TODO: FIX ME
            case UNKNOWN:
                annotations.add(an);
            }

        }

        InternalAnnotationGroup group = new InternalAnnotationGroup(annotations, e);
        return group;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitMemberSelect(MemberSelectTree node,
        Void p) {
        @Nullable Element e = InternalUtils.symbol(node);

        return new InternalAnnotationGroup(factory.createAnnotations(e), e);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitNewClass(NewClassTree node, Void p) {
        assert root != null;

        @Nullable TreePath path = trees.getPath(root, node);
        @Nullable Element method = InternalUtils.enclosingSymbol(path);

        if (method == null) /*nnbug*/
            return InternalAnnotationGroup.EMPTY;

        // TODO *** this is a hack and needs improvement
        if (method.getKind() == ElementKind.LOCAL_VARIABLE) {
            @Nullable Element encl = method.getEnclosingElement();
            if (encl != null)
                method = encl.getEnclosingElement();
        }

        if (method != null)
            return annotationsFromElement(method, node);
        else return InternalAnnotationGroup.EMPTY;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitTypeCast(TypeCastTree node, Void p) {

        // We added annotations to this file (which had already been written)
        // and ran the type checker to see what bugs it could find.

        @Nullable TreePath path = trees.getPath(root, node);
        @Nullable Element method = InternalUtils.enclosingSymbol(path);

        if (method != null)
            return annotationsFromElement(method, node);
        else return InternalAnnotationGroup.EMPTY;
    }

    // This handles the type as a whole, not a parameterized typewithin another type
    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitParameterizedType(ParameterizedTypeTree node, Void p) {
        @Nullable TreePath path = trees.getPath(root, node);
        if (path == null) /*nnbug*/
            return InternalAnnotationGroup.EMPTY;

        // This handles the type as whole though
        for (Tree tree : path) {
            switch (tree.getKind()) {
            case TYPE_CAST:
                return visitTypeCast((TypeCastTree) tree, p);
            case VARIABLE:
                return visitVariable((VariableTree)tree, p);
            case METHOD:
            case CLASS:
                // we gone too far
                @Nullable Element source = InternalUtils.enclosingAnnotationSource(path);
                assert source != null; /*nninvariant*/
                return annotationsFromElement(source, node);
            }
        }
        return InternalAnnotationGroup.EMPTY;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitLiteral(LiteralTree node, Void p) {
        return InternalAnnotationGroup.EMPTY;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitVariable(VariableTree node, Void p) {
        @Nullable Element e = InternalUtils.symbol(node);

        List<InternalAnnotation> annotations = new ArrayList<InternalAnnotation>();
        for (InternalAnnotation an : factory.createAnnotations(e)) {
            @Nullable AnnotationTarget target = an.getTarget();
            if (target == null) continue; /*nnbug*/
            switch (target.type) {
            case LOCAL_VARIABLE:
            case LOCAL_VARIABLE_GENERIC_OR_ARRAY:

            case FIELD_GENERIC_OR_ARRAY:
                // TODO: Fix Me
            case UNKNOWN:
                annotations.add(an);
            }
        }

        InternalAnnotationGroup group = new InternalAnnotationGroup(annotations, e);
        return group;
    }

    // Returns the annotations on a tree with an associated element.
    @SuppressWarnings("nullness")
    private InternalAnnotationGroup annotationsFromElement(Element element,
            Tree tree) {

        List<InternalAnnotation> annotations =
            factory.createAnnotations(element, tree);

        return new InternalAnnotationGroup(annotations, element);
    }

    // Returns the annotations on an expression of the form "new C[]".
    @Override
    public InternalAnnotationGroup visitNewArray(@Nullable NewArrayTree node, @Nullable Void p) {
        assert root != null: "nullness";
        assert node != null: "nullness";

        TreePath path = trees.getPath(root, node);
        Element method = InternalUtils.enclosingSymbol(path);

        return annotationsFromElement(method, node);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitMethod(MethodTree node, Void p) {
        @Nullable Element e = InternalUtils.symbol(node);

        return new InternalAnnotationGroup(factory.createAnnotations(e), e);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitMethodInvocation(
        MethodInvocationTree node, Void p) {

        @Nullable Element e = InternalUtils.symbol(node.getMethodSelect());
        @Nullable TreePath path = TreePath.getPath(root, node);
        if (e == null) /*nnbug*/
            return InternalAnnotationGroup.EMPTY;
        return new InternalAnnotationGroup(factory.createAnnotations(e), e);
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitBinary(BinaryTree node, Void p) {
        return InternalAnnotationGroup.EMPTY;
    }

    @Override @SuppressWarnings("nullness")
    public InternalAnnotationGroup visitPrimitiveType(PrimitiveTypeTree node, Void p) {
        return InternalAnnotationGroup.EMPTY;
    }
}

/* Add to visitNewArray():
        if (method == null)
            return InternalAnnotationGroup.EMPTY;
*/
