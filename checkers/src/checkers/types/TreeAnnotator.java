package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.util.AnnotationUtils;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;


/**
 * Adds annotations to a type based on the contents of a tree. By default, this
 * class honors the {@link ImplicitFor} annotation and applies implicit
 * annotations specified by {@link ImplicitFor} for any tree whose visitor is
 * not overridden or does not call {@code super}; it is designed to be invoked
 * from
 * {@link AnnotatedTypeFactory#annotateImplicit(Element, AnnotatedTypeMirror)}
 * and {@link AnnotatedTypeFactory#annotateImplicit(Tree, AnnotatedTypeMirror)}.
 *
 * <p>
 *
 * {@link TreeAnnotator} does not traverse trees deeply by default.
 *
 * This class takes care of three of the attributes of {@link ImplicitFor};
 * the others are handled in {@link TypeAnnotator}.
 * TODO: we currently don't check that any attribute is set, that is, a qualifier
 * could be annotated as @ImplicitFor(), which might be misleading.
 *
 * @see TypeAnnotator
 */
public class TreeAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

    /* The following three fields are mappings from a particular AST kind,
     * AST Class, or String literal pattern to the set of AnnotationMirrors
     * that should be defaulted.
     * There can be at most one qualifier per qualifier hierarchy.
     * For type systems with single top qualifiers, the sets will always contain
     * at most one element.
     */
    private final Map<Tree.Kind, Set<AnnotationMirror>> treeKinds;
    private final Map<Class<?>, Set<AnnotationMirror>> treeClasses;
    private final Map<Pattern, Set<AnnotationMirror>> stringPatterns;

    private final QualifierHierarchy qualHierarchy;
    private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a {@link TypeAnnotator} from the given checker, using that checker's
     * {@link TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     *
     * @param checker the type checker to which this annotator belongs
     */
    public TreeAnnotator(BaseTypeChecker checker, AnnotatedTypeFactory atypeFactory) {

        this.treeKinds = new EnumMap<Kind, Set<AnnotationMirror>>(Kind.class);
        this.treeClasses = new HashMap<Class<?>, Set<AnnotationMirror>>();
        this.stringPatterns = new IdentityHashMap<Pattern, Set<AnnotationMirror>>();

        this.qualHierarchy = checker.getQualifierHierarchy();
        this.atypeFactory = atypeFactory;

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = checker.getSupportedTypeQualifiers();

        // For each qualifier, read the @ImplicitFor annotation and put its tree
        // classes and kinds into maps.
        for (Class<? extends Annotation> qual : quals) {
            ImplicitFor implicit = qual.getAnnotation(ImplicitFor.class);
            if (implicit == null)
                continue;

            AnnotationMirror theQual = AnnotationUtils.fromClass(atypeFactory.elements, qual);
            for (Class<? extends Tree> treeClass : implicit.treeClasses()) {
                addTreeClass(treeClass, theQual);
            }

            for (Tree.Kind treeKind : implicit.trees()) {
                addTreeKind(treeKind, theQual);
            }

            for (String pattern : implicit.stringPatterns()) {
                addStringPattern(pattern, theQual);
            }
        }
    }

    public void addTreeClass(Class<? extends Tree> treeClass, AnnotationMirror theQual) {
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, treeClasses, treeClass, theQual);
        if (!res) {
            SourceChecker.errorAbort("TreeAnnotator: invalid update of map " +
                    treeClasses + " at " + treeClass + " with " +theQual);
        }
    }

    public void addTreeKind(Tree.Kind treeKind, AnnotationMirror theQual) {
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, treeKinds, treeKind, theQual);
        if (!res) {
            SourceChecker.errorAbort("TreeAnnotator: invalid update of treeKinds " +
                    treeKinds + " at " + treeKind + " with " + theQual);
        }
    }

    public void addStringPattern(String pattern, AnnotationMirror theQual) {
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, stringPatterns, Pattern.compile(pattern), theQual);
        if (!res) {
            SourceChecker.errorAbort("TreeAnnotator: invalid update of stringPatterns " +
                    stringPatterns + " at " + pattern + " with " + theQual);
        }
    }

    @Override
    public Void defaultAction(Tree tree, AnnotatedTypeMirror type) {
        if (tree == null || type == null) return null;

        // If this tree's kind is in treeKinds, annotate the type.
        // If this tree's class or any of its interfaces are in treeClasses,
        // annotate the type, and if it was an interface add a mapping for it to
        // treeClasses.

        if (treeKinds.containsKey(tree.getKind())) {
            Set<AnnotationMirror> fnd = treeKinds.get(tree.getKind());
            for (AnnotationMirror f : fnd) {
                if (!type.isAnnotatedInHierarchy(f)) {
                    type.addAnnotation(f);
                }
            }
        } else if (!treeClasses.isEmpty()) {
            Class<? extends Tree> t = tree.getClass();
            if (treeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = treeClasses.get(t);
                for (AnnotationMirror f : fnd) {
                    if (!type.isAnnotatedInHierarchy(f)) {
                        type.addAnnotation(f);
                    }
                }
            }
            for (Class<?> c : t.getInterfaces()) {
                if (treeClasses.containsKey(c)) {
                    Set<AnnotationMirror> fnd = treeClasses.get(c);
                    for (AnnotationMirror f : fnd) {
                        if (!type.isAnnotatedInHierarchy(f)) {
                            type.addAnnotation(f);
                        }
                    }
                    treeClasses.put(t, treeClasses.get(c));
                }
            }
        }
        return null;
    }

    /**
     * Go through the string patterns and add the greatest lower bound of all matching patterns.
     */
    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
        if (!stringPatterns.isEmpty() && tree.getKind() == Tree.Kind.STRING_LITERAL) {
            Set<AnnotationMirror> res = null;
            String string = (String) tree.getValue();
            for (Pattern pattern : stringPatterns.keySet()) {
                if (pattern.matcher(string).matches()) {
                    if (res == null) {
                        res = stringPatterns.get(pattern);
                    } else {
                        Set<AnnotationMirror> newres = stringPatterns.get(pattern);
                        res = qualHierarchy.greatestLowerBounds(res, newres);
                    }
                }
            }
            if (res != null) {
                type.addAnnotations(res);
            }
        }
        return super.visitLiteral(tree, type);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
        // TODO: should we perform default action to get trees/tree kinds/...?
        // defaultAction(tree, type);

        // System.out.printf("TreeAnnotator.visitNewArray(%s, %s)%n", tree, type);
        assert type.getKind() == TypeKind.ARRAY : "TreeAnnotator.visitNewArray: should be an array type";
        AnnotatedTypeMirror componentType = ((AnnotatedArrayType)type).getComponentType();

        Collection<AnnotationMirror> prev = null;
        if (tree.getInitializers() != null &&
                tree.getInitializers().size() != 0) {
            // We have initializers, either with or without an array type.

            for (ExpressionTree init: tree.getInitializers()) {
                AnnotatedTypeMirror iniType = atypeFactory.getAnnotatedType(init);
                Collection<AnnotationMirror> annos = iniType.getAnnotations();

                prev = (prev == null) ? annos : qualHierarchy.leastUpperBounds(prev, annos);
            }
        } else {
            prev = componentType.getAnnotations();
        }

        assert prev != null : "TreeAnnotator.visitNewArray: violated assumption about qualifiers";

        AnnotatedTypeMirror context = atypeFactory.getVisitorState().getAssignmentContext();
        Collection<AnnotationMirror> post;

        if (context != null && context instanceof AnnotatedArrayType) {
            AnnotatedTypeMirror contextComponentType = ((AnnotatedArrayType) context).getComponentType();
            // Only compare the qualifiers that existed in the array type
            // Defaulting wasn't performed yet, so prev might have fewer qualifiers than
            // contextComponentType, which would cause a failure.
            // TODO: better solution?
            boolean prevIsSubtype = true;
            for (AnnotationMirror am : prev) {
                if (contextComponentType.isAnnotatedInHierarchy(am) &&
                        !this.qualHierarchy.isSubtype(am, contextComponentType.getAnnotationInHierarchy(am))) {
                    prevIsSubtype = false;
                }
            }
            // TODO: checking conformance of component kinds is a basic sanity check
            // It fails for array initializer expressions. Those should be handled nicer.
            if (contextComponentType.getKind() == componentType.getKind() &&
                    (prev.isEmpty() ||
                    (!contextComponentType.getAnnotations().isEmpty() &&
                            prevIsSubtype))) {
                post = contextComponentType.getAnnotations();
            } else {
                // The type of the array initializers is incompatible with the
                // context type!
                // Somebody else will complain.
                post = prev;
            }
        } else {
            // No context is available - simply use what we have.
            post = prev;
        }
        for (AnnotationMirror an : post) {
            if (!componentType.isAnnotatedInHierarchy(an)) {
                componentType.addAnnotation(an);
            }
        }

        return super.visitNewArray(tree, type);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
        // TODO: should we perform default action to get trees/tree kinds/...?
        // defaultAction(node, type);
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getExpression());
            AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getVariable());
            Set<AnnotationMirror> lubs = qualHierarchy.leastUpperBounds(rhs.getAnnotations(), lhs.getAnnotations());
            type.replaceAnnotations(lubs);
        }
        return super.visitCompoundAssignment(node, type);
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        // TODO: should we perform default action to get trees/tree kinds/...?
        // defaultAction(node, type);
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror a = atypeFactory.getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror b = atypeFactory.getAnnotatedType(node.getRightOperand());
            Set<AnnotationMirror> lubs = qualHierarchy.leastUpperBounds(a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
            type.replaceAnnotations(lubs);
        }
        return super.visitBinary(node, type);
    }

    @Override
    public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
        // TODO: should we perform default action to get trees/tree kinds/...?
        // defaultAction(node, type);
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror exp = atypeFactory.getAnnotatedType(node.getExpression());
            type.replaceAnnotations(exp.getAnnotations());
        }
        return super.visitUnary(node, type);
    }

    /*
     * TODO: would this make sense in general?
    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, AnnotatedTypeMirror type) {
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror a = typeFactory.getAnnotatedType(node.getTrueExpression());
            AnnotatedTypeMirror b = typeFactory.getAnnotatedType(node.getFalseExpression());
            Set<AnnotationMirror> lubs = qualHierarchy.leastUpperBounds(a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
            type.replaceAnnotations(lubs);
        }
        return super.visitConditionalExpression(node, type);
    }*/

    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
        // TODO: should we perform default action to get trees/tree kinds/...?
        // defaultAction(node, type);
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());
            if (type.getKind() == TypeKind.TYPEVAR ) {
                if (exprType.getKind() == TypeKind.TYPEVAR) {
                    // If both types are type variables, take the direct annotations.
                    type.addAnnotations(exprType.getAnnotations());
                }
                // else do nothing
                // TODO: What should we do if the type is a type variable, but the expression
                // is not?
            } else {
                // Use effective annotations from the expression, to get upper bound
                // of type variables.
                type.addAnnotations(exprType.getEffectiveAnnotations());
            }
        }
        return super.visitTypeCast(node, type);
    }
}
