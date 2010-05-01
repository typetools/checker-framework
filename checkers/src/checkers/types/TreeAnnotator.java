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

    private final Map<Tree.Kind, AnnotationMirror> treeKinds;
    private final Map<Class<?>, AnnotationMirror> treeClasses;
    private final Map<Pattern, AnnotationMirror> stringPatterns;
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

        this.treeKinds = new EnumMap<Kind, AnnotationMirror>(Kind.class);
        this.treeClasses = new HashMap<Class<?>, AnnotationMirror>();
        this.stringPatterns = new IdentityHashMap<Pattern, AnnotationMirror>();
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

            AnnotationMirror theQual = annoFactory.fromClass(qual);
            for (Class<? extends Tree> treeClass : implicit.treeClasses())
                treeClasses.put(treeClass, theQual);

            for (Tree.Kind treeKind : implicit.trees())
                treeKinds.put(treeKind, theQual);

            for (String pattern : implicit.stringPatterns())
                stringPatterns.put(Pattern.compile(pattern), theQual);
        }
    }

    @Override
    public Void defaultAction(Tree tree, AnnotatedTypeMirror type) {

        if (type.isAnnotated())
            return null;

        // If this tree's kind is in treeKinds, annotate the type.
        // If this tree's class or any of its interfaces are in treeClasses,
        // annotate the type, and if it was an interface add a mapping for it to
        // treeClasses.

        if (treeKinds.containsKey(tree.getKind()))
            type.addAnnotation(treeKinds.get(tree.getKind()));
        else if (!treeClasses.isEmpty()) {
            Class<? extends Tree> t = tree.getClass();
            if (treeClasses.containsKey(t))
                type.addAnnotation(treeClasses.get(t));
            for (Class<?> c : t.getInterfaces()) {
                if (treeClasses.containsKey(c)) {
                    type.addAnnotation(treeClasses.get(c));
                    treeClasses.put(t, treeClasses.get(c));
                }
            }
        }
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
        if (!stringPatterns.isEmpty() && tree.getKind() == Tree.Kind.STRING_LITERAL) {
            String string = (String)tree.getValue();
            for (Pattern pattern: stringPatterns.keySet()) {
                if (pattern.matcher(string).matches()) {
                    type.addAnnotation(stringPatterns.get(pattern));
                    break;
                }
            }
        }
        return super.visitLiteral(tree, type);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
        if (tree.getType() == null) {
            Collection<AnnotationMirror> lub = null;
            
            for (ExpressionTree init: tree.getInitializers()) {
                AnnotatedTypeMirror iniType = typeFactory.getAnnotatedType(init);
                Collection<AnnotationMirror> annos = iniType.getAnnotations();
                
                lub = (lub == null) ? annos : qualHierarchy.leastUpperBound(lub, annos);
            }
            
            assert type.getKind() == TypeKind.ARRAY;
            if (lub != null) {
                ((AnnotatedArrayType)type).getComponentType().addAnnotations(lub);
            }
        }
        return null;
    }
}
