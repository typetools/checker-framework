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
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
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
    private final AnnotatedTypeFactory typeFactory;

    /**
     * Creates a {@link TypeAnnotator} from the given checker, using that checker's
     * {@link TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     *
     * @param checker the type checker to which this annotator belongs
     */
    public TreeAnnotator(BaseTypeChecker checker, AnnotatedTypeFactory typeFactory) {

        this.treeKinds = new EnumMap<Kind, Set<AnnotationMirror>>(Kind.class);
        this.treeClasses = new HashMap<Class<?>, Set<AnnotationMirror>>();
        this.stringPatterns = new IdentityHashMap<Pattern, Set<AnnotationMirror>>();
        this.qualHierarchy = checker.getQualifierHierarchy();
        this.typeFactory = typeFactory;

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(checker.getProcessingEnvironment());

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals
            = checker.getSupportedTypeQualifiers();

        // For each qualifier, read the @ImplicitFor annotation and put its tree
        // classes and kinds into maps.
        for (Class<? extends Annotation> qual : quals) {
            ImplicitFor implicit = qual.getAnnotation(ImplicitFor.class);
            if (implicit == null)
                continue;

            boolean res;
            AnnotationMirror theQual = annoFactory.fromClass(qual);
            for (Class<? extends Tree> treeClass : implicit.treeClasses()) {
                res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, treeClasses, treeClass, theQual);
                if (!res) {
                    SourceChecker.errorAbort("TreeAnnotator: invalid update of treeClasses " +
                            treeClasses + " at " + treeClass + " with " + theQual);
                }
            }

            for (Tree.Kind treeKind : implicit.trees()) {
                res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, treeKinds, treeKind, theQual);
                if (!res) {
                    SourceChecker.errorAbort("TreeAnnotator: invalid update of treeKinds " +
                            treeKinds + " at " + treeKind + " with " + theQual);
                }
            }

            for (String pattern : implicit.stringPatterns()) {
                res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, stringPatterns, Pattern.compile(pattern), theQual);
                if (!res) {
                    SourceChecker.errorAbort("TreeAnnotator: invalid update of stringPatterns " +
                            stringPatterns + " at " + pattern + " with " + theQual);
                }
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
        if (tree==null || type==null) return null;

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
                    if (res==null) {
                        res = stringPatterns.get(pattern);
                    } else {
                        Set<AnnotationMirror> newres = stringPatterns.get(pattern);
                        res = qualHierarchy.greatestLowerBounds(res, newres);
                    }
                }
            }
            if (res!=null) {
                type.addAnnotations(res);
            }
        }
        return super.visitLiteral(tree, type);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
        if (tree.getType() == null) {
            Collection<AnnotationMirror> lubs = null;

            for (ExpressionTree init: tree.getInitializers()) {
                AnnotatedTypeMirror iniType = typeFactory.getAnnotatedType(init);
                Collection<AnnotationMirror> annos = iniType.getAnnotations();

                lubs = (lubs == null) ? annos : qualHierarchy.leastUpperBounds(lubs, annos);
            }

            assert type.getKind() == TypeKind.ARRAY;
            if (lubs != null) {
                AnnotatedTypeMirror componentType = ((AnnotatedArrayType)type).getComponentType();
                Tree context = typeFactory.getVisitorState().getAssignmentContextTree();

                if (context!=null) {
                    AnnotatedTypeMirror contextType = null;
                    if (context instanceof VariableTree) {
                        contextType = typeFactory.getDefaultedAnnotatedType((VariableTree)context);
                    }
                    if (context instanceof IdentifierTree) {
                        // Within annotations
                        contextType = typeFactory.getAnnotatedType(context);
                        if (contextType instanceof AnnotatedExecutableType) {
                            contextType = ((AnnotatedExecutableType)contextType).getReturnType();
                        }
                    }
                    if (contextType!=null && contextType instanceof AnnotatedArrayType) {
                        AnnotatedTypeMirror contextComponentType = ((AnnotatedArrayType) contextType).getComponentType();
                        if (this.qualHierarchy.isSubtype(lubs, contextComponentType.getAnnotations())) {
                            for (AnnotationMirror cct : contextComponentType.getAnnotations()) {
                                if (!componentType.isAnnotatedInHierarchy(cct)) {
                                    componentType.addAnnotation(cct);
                                }
                            }

                            if (!type.isAnnotated()) {
                                type.addAnnotations(qualHierarchy.getBottomAnnotations());
                            }
                        } else {
                            // The type of the array initializers is incompatible with the
                            // context type!
                            // Somebody else will complain.
                            for (AnnotationMirror lub : lubs) {
                                if (!componentType.isAnnotatedInHierarchy(lub)) {
                                    componentType.addAnnotation(lub);
                                }
                            }
                        }
                    }
                } else {
                    for (AnnotationMirror lub : lubs) {
                        if (!componentType.isAnnotatedInHierarchy(lub)) {
                            componentType.addAnnotation(lub);
                        }
                    }
                    if (!type.isAnnotated()) {
                        type.addAnnotations(qualHierarchy.getBottomAnnotations());
                    }
                }
            }
        }
        return super.visitNewArray(tree, type);
    }
}
