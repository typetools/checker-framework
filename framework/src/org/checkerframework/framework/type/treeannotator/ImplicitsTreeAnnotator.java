package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Adds annotations to a type based on the contents of a tree. By default, this
 * class honors the {@link org.checkerframework.framework.qual.ImplicitFor} annotation and applies implicit
 * annotations specified by {@link org.checkerframework.framework.qual.ImplicitFor} for any tree whose visitor is
 * not overridden or does not call {@code super}; it is designed to be invoked
 * from
 * {@link org.checkerframework.framework.type.AnnotatedTypeFactory#annotateImplicit(javax.lang.model.element.Element, org.checkerframework.framework.type.AnnotatedTypeMirror)}
 * and {@link org.checkerframework.framework.type.AnnotatedTypeFactory#annotateImplicit(com.sun.source.tree.Tree, org.checkerframework.framework.type.AnnotatedTypeMirror)}.
 *
 * <p>
 *
 * {@link ImplicitsTreeAnnotator} does not traverse trees deeply by default.
 *
 * This class takes care of three of the attributes of {@link org.checkerframework.framework.qual.ImplicitFor};
 * the others are handled in {@link org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator}.
 * TODO: we currently don't check that any attribute is set, that is, a qualifier
 * could be annotated as @ImplicitFor(), which might be misleading.
 *
 * @see org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator
 * @see TreeAnnotator
 */
public class ImplicitsTreeAnnotator extends TreeAnnotator {

    /* The following three fields are mappings from a particular AST kind,
     * AST Class, or String literal pattern to the set of AnnotationMirrors
     * that should be defaulted.
     * There can be at most one qualifier per qualifier hierarchy.
     * For type systems with single top qualifiers, the sets will always contain
     * at most one element.
     */
    private final Map<Kind, Set<AnnotationMirror>> treeKinds;
    private final Map<Class<?>, Set<AnnotationMirror>> treeClasses;
    private final Map<Pattern, Set<AnnotationMirror>> stringPatterns;

    protected final QualifierHierarchy qualHierarchy;

    /**
     * Creates a {@link org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator} from the given checker, using that checker's
     * {@link org.checkerframework.framework.qual.TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     */
    public ImplicitsTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        this.treeKinds = new EnumMap<Kind, Set<AnnotationMirror>>(Kind.class);
        this.treeClasses = new HashMap<Class<?>, Set<AnnotationMirror>>();
        this.stringPatterns = new IdentityHashMap<Pattern, Set<AnnotationMirror>>();

        this.qualHierarchy = atypeFactory.getQualifierHierarchy();

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = atypeFactory.getSupportedTypeQualifiers();

        // For each qualifier, read the @ImplicitFor annotation and put its tree
        // classes and kinds into maps.
        for (Class<? extends Annotation> qual : quals) {
            ImplicitFor implicit = qual.getAnnotation(ImplicitFor.class);
            if (implicit == null)
                continue;

            AnnotationMirror theQual = AnnotationUtils.fromClass(atypeFactory.getElementUtils(), qual);
            for (Class<? extends Tree> treeClass : implicit.treeClasses()) {
                addTreeClass(treeClass, theQual);
            }

            for (Kind treeKind : implicit.trees()) {
                addTreeKind(treeKind, theQual);
            }

            for (String pattern : implicit.stringPatterns()) {
                addStringPattern(pattern, theQual);
            }
        }
    }

    public void addTreeClass(Class<? extends Tree> treeClass, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(treeClasses, treeClass, theQual);
        if (!res) {
            ErrorReporter.errorAbort("PropagationTreeAnnotator: invalid update of map " +
                    treeClasses + " at " + treeClass + " with " +theQual);
        }
    }

    public void addTreeKind(Kind treeKind, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(treeKinds, treeKind, theQual);
        if (!res) {
            ErrorReporter.errorAbort("PropagationTreeAnnotator: invalid update of treeKinds " +
                    treeKinds + " at " + treeKind + " with " + theQual);
        }
    }

    public void addStringPattern(String pattern, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(stringPatterns, Pattern.compile(pattern), theQual);
        if (!res) {
            ErrorReporter.errorAbort("PropagationTreeAnnotator: invalid update of stringPatterns " +
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
            type.addMissingAnnotations(fnd);
        } else if (!treeClasses.isEmpty()) {
            Class<? extends Tree> t = tree.getClass();
            if (treeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = treeClasses.get(t);
                type.addMissingAnnotations(fnd);
            }
            for (Class<?> c : t.getInterfaces()) {
                if (treeClasses.containsKey(c)) {
                    Set<AnnotationMirror> fnd = treeClasses.get(c);
                    type.addMissingAnnotations(fnd);
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
        if (!stringPatterns.isEmpty() && tree.getKind() == Kind.STRING_LITERAL) {
            Set<? extends AnnotationMirror> res = null;
            String string = (String) tree.getValue();
            for (Pattern pattern : stringPatterns.keySet()) {
                if (pattern.matcher(string).matches()) {
                    if (res == null) {
                        res = stringPatterns.get(pattern);
                    } else {
                        Set<? extends AnnotationMirror> newres = stringPatterns.get(pattern);
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
}
