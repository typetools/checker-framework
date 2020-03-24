package org.checkerframework.checker.nullness;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;
import org.checkerframework.framework.util.TypeArgumentMapper;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/**
 * KeyForPropagator is used to move nested KeyFor annotations in type arguments from one side of a
 * pseudo-assignment to the other. The KeyForPropagationTreeAnnotator details the locations in which
 * this occurs.
 *
 * @see org.checkerframework.checker.nullness.KeyForPropagationTreeAnnotator
 */
public class KeyForPropagator {
    public static enum PropagationDirection {
        // transfer FROM the super type to the subtype
        TO_SUBTYPE,

        // transfer FROM the subtype to the supertype
        TO_SUPERTYPE,

        // first execute TO_SUBTYPE then TO_SUPERTYPE, if TO_SUBTYPE actually transfers
        // an annotation for a particular type T then T will not be affected by the
        // TO_SUPERTYPE transfer because it will already have a KeyFor annotation
        BOTH
    }

    // The top type of the KeyFor hierarchy, this class will replace @UnknownKeyFor annotations.
    // It will also add annotations when they are misisng for types that require primary annotation
    // (i.e. not TypeVars, Wildcards, Intersections, or Unions).
    private final AnnotationMirror UNKNOWN_KEYFOR;

    public KeyForPropagator(AnnotationMirror unknownKeyfor) {
        this.UNKNOWN_KEYFOR = unknownKeyfor;
    }

    /**
     * Propagate annotations from the type arguments of one type to another. Which type is the
     * source and destination of the annotations depends on the direction parameter. Only @KeyFor
     * annotations are propagated and only if the type to which it would be propagated contains
     * an @UnknownKeyFor or contains no key for annotations of any kind. If any of the type
     * arguments are wildcards than they are ignored.
     *
     * <p>Note the primary annotations of subtype/supertype are not used.
     *
     * <p>Simple Example:
     *
     * <pre>{@code
     * typeOf(subtype) = ArrayList<@KeyFor("a") String>
     * typeOf(supertype) = List<@UnknownKeyFor String>
     * direction = TO_SUPERTYPE
     * }</pre>
     *
     * The type of supertype after propagate would be: {@code List<@KeyFor("a") String>}
     *
     * <p>A more complex example would be:
     *
     * <pre>{@code
     * typeOf(subtype) = HashMap<@UnknownKeyFor String, @KeyFor("b") List<@KeyFor("c") String>>
     * typeOf(supertype) = Map<@KeyFor("a") String, @KeyFor("b") List<@KeyFor("c") String>>
     * direction = TO_SUBTYPE
     * }</pre>
     *
     * The type of subtype after propagate would be: {@code HashMap<@KeyFor("a")
     * String, @KeyFor("b") List<@KeyFor("c") String>>}
     */
    public void propagate(
            final AnnotatedDeclaredType subtype,
            final AnnotatedDeclaredType supertype,
            PropagationDirection direction,
            final AnnotatedTypeFactory typeFactory) {
        final TypeElement subtypeElement = (TypeElement) subtype.getUnderlyingType().asElement();
        final TypeElement supertypeElement =
                (TypeElement) supertype.getUnderlyingType().asElement();
        final Types types = typeFactory.getProcessingEnv().getTypeUtils();

        // Note: The right hand side of this or expression will cover raw types
        if (subtype.getTypeArguments().isEmpty()) {
            return;
        } // else

        // this can happen for two reasons:
        // 1) the subclass introduced NEW type arguments when the superclass had none
        // 2) the supertype was RAW.
        // In either case, there is no reason to propagate
        if (supertype.getTypeArguments().isEmpty()) {
            return;
        }

        Set<Pair<Integer, Integer>> typeParamMappings =
                TypeArgumentMapper.mapTypeArgumentIndices(subtypeElement, supertypeElement, types);

        KeyForPropagationMerger merger =
                new KeyForPropagationMerger(typeFactory.getProcessingEnv());

        final List<AnnotatedTypeMirror> subtypeArgs = subtype.getTypeArguments();
        final List<AnnotatedTypeMirror> supertypeArgs = supertype.getTypeArguments();

        for (final Pair<Integer, Integer> path : typeParamMappings) {
            final AnnotatedTypeMirror subtypeArg = subtypeArgs.get(path.first);
            final AnnotatedTypeMirror supertypeArg = supertypeArgs.get(path.second);

            if (subtypeArg.getKind() == TypeKind.WILDCARD
                    || supertypeArg.getKind() == TypeKind.WILDCARD) {
                continue;
            }

            switch (direction) {
                case TO_SUBTYPE:
                    merger.visit(supertypeArg, subtypeArg);
                    break;

                case TO_SUPERTYPE:
                    merger.visit(subtypeArg, supertypeArg);
                    break;

                case BOTH:
                    // note if they both have an annotation nothing will happen
                    merger.visit(subtypeArg, supertypeArg);
                    merger.visit(supertypeArg, subtypeArg);
                    break;
            }
        }
    }

    /**
     * Propagate annotations from the type arguments of {@code type} to the assignment context of
     * {@code newClassTree} if one exists.
     *
     * @param newClassTree new class tree
     * @param type annotated type of {@code newClassTree}
     * @param atypeFactory factory
     */
    public void propagateNewClassTree(
            NewClassTree newClassTree,
            AnnotatedTypeMirror type,
            KeyForAnnotatedTypeFactory atypeFactory) {
        Pair<Tree, AnnotatedTypeMirror> context =
                atypeFactory.getVisitorState().getAssignmentContext();
        if (type.getKind() != TypeKind.DECLARED || context == null || context.first == null) {
            return;
        }
        TreePath path = atypeFactory.getPath(newClassTree);
        if (path == null) {
            return;
        }
        AnnotatedTypeMirror assignedTo = TypeArgInferenceUtil.assignedTo(atypeFactory, path);
        if (assignedTo == null) {
            return;
        }
        // array types and boxed primitives etc don't require propagation
        if (assignedTo.getKind() == TypeKind.DECLARED) {
            propagate(
                    (AnnotatedDeclaredType) type,
                    (AnnotatedDeclaredType) assignedTo,
                    PropagationDirection.TO_SUBTYPE,
                    atypeFactory);
        }
    }

    /**
     * An annotated type merger that merges @KeyFor annotations and only if the type that is
     * receiving an annotation has an @UnknownKeyFor annotation or NO key for annotations.
     */
    private class KeyForPropagationMerger extends AnnotatedTypeMerger {
        private final ProcessingEnvironment env;

        private KeyForPropagationMerger(ProcessingEnvironment env) {
            this.env = env;
        }

        @Override
        protected void replaceAnnotations(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
            final AnnotationMirror fromKeyFor = from.getAnnotationInHierarchy(UNKNOWN_KEYFOR);
            final AnnotationMirror toKeyFor = to.getAnnotationInHierarchy(UNKNOWN_KEYFOR);

            boolean toNeedsAnnotation =
                    toKeyFor == null || AnnotationUtils.areSame(toKeyFor, UNKNOWN_KEYFOR);
            if (fromKeyFor != null && toNeedsAnnotation) {
                AnnotationBuilder annotationBuilder = new AnnotationBuilder(env, fromKeyFor);
                to.replaceAnnotation(annotationBuilder.build());
            }
        }
    }
}
