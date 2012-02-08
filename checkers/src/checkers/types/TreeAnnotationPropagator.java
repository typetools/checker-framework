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
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.util.AnnotationUtils;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * Propagates annotations from operands of an AST tree to the tree
 * itself.  Propagation only considers one ply of the tree and does
 * not recurse, however the TypeProvider that gives types of operands
 * may internally recurse over the tree.  By default, this class
 * honors the {@link ImplicitFor} annotation and applies implicit
 * annotations specified by {@link ImplicitFor} for any tree whose
 * visitor is not overridden or does not call {@code super}.
 */
public class TreeAnnotationPropagator
    extends SimpleTreeVisitor<Set<AnnotationMirror>, TypeAnnotationProvider> {

    private final Map<Tree.Kind, AnnotationMirror> treeKinds;
    private final Map<Class<?>, AnnotationMirror> treeClasses;
    private final Map<Pattern, AnnotationMirror> stringPatterns;
    private final QualifierHierarchy qualHierarchy;

    /**
     * Creates a {@link TypeAnnotator} from the given checker, using that checker's
     * {@link TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     *
     * @param checker the type checker to which this annotator belongs
     */
    public TreeAnnotationPropagator(BaseTypeChecker checker) {

        this.treeKinds = new EnumMap<Kind, AnnotationMirror>(Kind.class);
        this.treeClasses = new HashMap<Class<?>, AnnotationMirror>();
        this.stringPatterns = new IdentityHashMap<Pattern, AnnotationMirror>();
        this.qualHierarchy = checker.getQualifierHierarchy();

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

            AnnotationMirror theQual = annoFactory.fromClass(qual);
            for (Class<? extends Tree> treeClass : implicit.treeClasses())
                treeClasses.put(treeClass, theQual);

            for (Tree.Kind treeKind : implicit.trees())
                treeKinds.put(treeKind, theQual);

            for (String pattern : implicit.stringPatterns())
                stringPatterns.put(Pattern.compile(pattern), theQual);
        }
    }

    public void addTreeClass(Class<? extends Tree> treeClass, AnnotationMirror theQual) {
        treeClasses.put(treeClass, theQual);
    }

    public void addTreeKind(Tree.Kind treeKind, AnnotationMirror theQual) {
        treeKinds.put(treeKind, theQual);
    }

    public void addStringPattern(String pattern, AnnotationMirror theQual) {
        stringPatterns.put(Pattern.compile(pattern), theQual);
    }

    public Set<AnnotationMirror> defaultAction(Tree tree, TypeAnnotationProvider provider) {
        // If this tree's kind is in treeKinds, annotate the type.
        // If this tree's class or any of its interfaces are in treeClasses,
        // annotate the type, and if it was an interface add a mapping for it to
        // treeClasses.

        Set<AnnotationMirror> results = new HashSet<AnnotationMirror>();
        if (treeKinds.containsKey(tree.getKind()))
            results.add(treeKinds.get(tree.getKind()));
        else if (!treeClasses.isEmpty()) {
            Class<? extends Tree> t = tree.getClass();
            if (treeClasses.containsKey(t))
                results.add(treeClasses.get(t));
            for (Class<?> c : t.getInterfaces()) {
                if (treeClasses.containsKey(c)) {
                    results.add(treeClasses.get(c));
                    treeClasses.put(t, treeClasses.get(c));
                }
            }
        }
        return results;
    }

    /**
     * Go through the string patterns and add the greatest lower bound of all matching patterns.
     */
    public Set<AnnotationMirror> visitLiteral(LiteralTree tree,
                                              TypeAnnotationProvider provider) {
        if (!stringPatterns.isEmpty() && tree.getKind() == Tree.Kind.STRING_LITERAL) {
            AnnotationMirror res = null;
            String string = (String) tree.getValue();
            for (Pattern pattern : stringPatterns.keySet()) {
                if (pattern.matcher(string).matches()) {
                    if (res==null) {
                        res = stringPatterns.get(pattern);
                    } else {
                        AnnotationMirror newres = stringPatterns.get(pattern);
                        res = qualHierarchy.greatestLowerBound(res, newres);
                    }
                }
            }
            if (res!=null) {
                return Collections.singleton(res);
            }
        }
        return Collections.<AnnotationMirror>emptySet();
    }

    public Set<AnnotationMirror> visitNewArray(NewArrayTree tree,
                                                   TypeAnnotationProvider provider) {
        if (tree.getType() == null) {
            Set<AnnotationMirror> lub = null;

            for (ExpressionTree init: tree.getInitializers()) {
                Set<AnnotationMirror> annos = provider.getAnnotations(tree);

                lub = (lub == null) ? annos : qualHierarchy.leastUpperBound(lub, annos);
            }

            if (lub != null) {
                // TODO: The annotations belong to the element type of the array.
                // How do we get them there?
                // ((AnnotatedArrayType)type).getComponentType().addAnnotations(lub);
                return lub;
            }
        }
        return Collections.<AnnotationMirror>emptySet();
    }
}
