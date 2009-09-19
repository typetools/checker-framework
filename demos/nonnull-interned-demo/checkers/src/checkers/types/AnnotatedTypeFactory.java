package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.quals.*;
import checkers.util.*;
import checkers.types.InternalUtils;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.*;

/**
 * A factory for generating annotated types. It is capable of
 * creating annotated class and method types from Tree API nodes. It is designed
 * to operate on a single syntax tree (compilation unit) at a time. It is
 * appropriate for checkers that use {@code {@link TreeScanner}}s (which also
 * operate on a single syntax tree at a time) to have a single factory per
 * scanner instance.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotatedTypeFactory {

    /** The {@link Trees} instance to use for tree node pathfinding. */
    protected final Trees trees;

    /** The root of the syntax tree that this factory operates on. */
    protected final CompilationUnitTree root;

    /** The scanner to use for locating annotations. */
    protected final InternalAnnotationScanner scanner;

    /** The processing environment to use for accessing compiler internals. */
    protected final ProcessingEnvironment env;

    /** The factory to use for creating annotations. */
    protected final AnnotationFactory annotations;

    /** Whether an annotation is needed on casts from an annotated type. */
    protected boolean ignoreSameTypeCast = false;

    /**
     * Constructs a factory from the given {@link ProcessingEnvironment} instance and syntax
     * tree root. (These parameters are required so that the factory may conduct
     * the appropriate annotation-gathering analyses on certain tree types.)
     *
     * @param env
     *            the {@link ProcessingEnvironment} instance to use
     * @param root
     *            the root of the syntax tree that this factory produces
     *            annotated types for
     * @throws IllegalArgumentException
     *             if either argument is {@code null}
     */
    public AnnotatedTypeFactory(ProcessingEnvironment env, CompilationUnitTree root) {

        if (env == null)
            throw new IllegalArgumentException("null env");

        if (root == null)
            throw new IllegalArgumentException("null root");

        this.env = env;

        @Nullable Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.root = root;
        this.scanner = new InternalAnnotationScanner(root, env, trees);
        this.annotations = new AnnotationFactory(env);
    }

    /**
     * Creates a new, "empty" {@link AnnotatedClassType} or subtype thereof.
     * It may be overridden by extending implementations so that the default
     * implementation acquires the extended behavior.
     *
     * @return a new, "empty" {@link AnnotatedClassType}
     */
    protected AnnotatedClassType createClassType(ProcessingEnvironment env) {
        return new AnnotatedClassType(env);
    }

    /**
     * Creates a new, "empty" {@link AnnotatedMethodType} or subtype thereof.
     * It may be overridden by extending implementations so that the default
     * implementation acquires the extended behavior.
     */
    protected AnnotatedMethodType createMethodType(ProcessingEnvironment env) {
        return new AnnotatedMethodType(env);
    }

    /**
     * Updates the given {@link AnnotatedClassType} with annotations from the
     * given {@link Element}. Used for "alias"-style declarations like
     * {@code class A extends @B C} so that instances of {@code A} carry the
     * annotation {@code @B}.
     *
     * @param elt the element to use as an annotation source
     * @param type the type to annotate
     */
    protected void aliasFromElement(Element elt, AnnotatedClassType type) {
        if (elt == null)
            return;

        @Nullable Element te = env.getTypeUtils().asElement(elt.asType());
        if (te != null) {
            for (AnnotationMirror mirror : te.getAnnotationMirrors()) {
                InternalAnnotation anno = annotations.createAnnotation(mirror);
                @Nullable AnnotationTarget target = anno.getTarget();
                if (target == null) /*nnbug*/
                    continue;
                if (target.type == TargetType.CLASS_EXTENDS || target.type == TargetType.CLASS_EXTENDS_GENERIC_OR_ARRAY) {
                    type.annotate(anno);
                }
            }

            // For class type parameter bounds.
            if (te instanceof TypeParameterElement) {
                TypeParameterElement tpe = (TypeParameterElement)te;
                @Nullable Element genElt = tpe.getGenericElement();
                if (genElt instanceof TypeElement) {
                    TypeElement gte = (TypeElement)genElt;
                    int index = gte.getTypeParameters().indexOf(tpe);
                    for (InternalAnnotation ia : annotations.createAnnotations(gte)) {
                        @Nullable AnnotationTarget at = ia.getTarget();
                        if (at != null && 
                                (at.type == TargetType.CLASS_TYPE_PARAMETER_BOUND 
                                 || at.type == TargetType.CLASS_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY) 
                                && at.bound == index)
                            type.annotate(ia);
                    }
                }
            }
        }

    }

    /**
     * Produces a completed {@link AnnotatedClassType} from the given {@link
     * Element}.
     *
     * @param element the {@link Element} to use as an annotation source
     * @return the {@link AnnotatedClassType} for the given element
     */
    public AnnotatedClassType getClass(@Nullable Element element) {
        // Trees are more informative
        if (trees.getTree(element) != null) {
            return getClass(trees.getTree(element));
        }

        AnnotatedClassType type = createClassType(env);

        if (element == null) /*nnbug*/
            return type;

        type.setElement(element);
        type.setUnderlyingType(ElementUtils.getType(element));
        
        for (InternalAnnotation annotation : annotations.createAnnotations(element))
            type.annotate(annotation);

        aliasFromElement(element, type);
        annotateDefaults(element, type);
        return type;
    }

    /**
     * Creates an {@link AnnotatedClassType} from the given {@link Tree} node.
     *
     * @param tree
     *            a tree node
     * @throws IllegalArgumentException
     *             if {@code tree} is null
     * @return the {@link AnnotatedClassType} corresponding to the given tree
     *         node and the annotations written on it
     */
    public AnnotatedClassType getClass(@Nullable Tree tree) {

        if (tree instanceof ParenthesizedTree)
            return getClass(((@NonNull ParenthesizedTree)tree).getExpression());
        if (tree instanceof MethodInvocationTree)
            return getMethod(tree).returnType;
        else if (tree instanceof AssignmentTree) {
            return getClass(((@NonNull AssignmentTree)tree).getVariable());
        } else if (tree instanceof CompoundAssignmentTree) {
            return getClass(((@NonNull CompoundAssignmentTree)tree).getVariable());
        } else if (tree instanceof UnaryTree) {
            return getClass(((@NonNull UnaryTree)tree).getExpression());
        }
        
        AnnotatedClassType type = createClassType(env);

        // The scanner has the contract that all annotations returned are either
        // type argument annotations or raw type annotations _for that tree_, and nothing
        // else.

        if (tree == null)
            throw new IllegalArgumentException("null tree");

        tree = TreeInfo.skipParens((JCTree)tree);
        assert tree != null; /*nnmaybe*/

        type.setTree(tree);
        @Nullable TreePath path = TreePath.getPath(root, tree);
        if (path != null) {
            @Nullable TypeMirror t = trees.getTypeMirror(path);
            if (t != null) /*nnbug*/
                type.setUnderlyingType(t);
        }
        
        @Nullable InternalAnnotationGroup group = scanner.visit(tree, null);
        assert group != null : String.format("invalid tree type %s\n", tree.getKind());

        if (group.hasElement())
            type.setElement(group.getElement());

        // If we're casting from something that's already, say, @NonNull,
        // return the annotated type of the expression if "ignoreSameTypeCast"
        // is set.
        if (ignoreSameTypeCast
                && tree instanceof TypeCastTree 
                && group.getAnnotations().isEmpty())
            return getClass(((TypeCastTree)tree).getExpression());

        for (InternalAnnotation annotation : group)
            type.annotate(annotation);

        @Nullable Tree symExpression = null;
        switch (tree.getKind()) {
        case NEW_CLASS:
            symExpression = ((NewClassTree)tree).getIdentifier();
            break;
        case TYPE_CAST:
            symExpression = ((TypeCastTree)tree).getType();
            break;
        case VARIABLE:
        case IDENTIFIER:
        case MEMBER_SELECT:
            symExpression = tree;
            break;
        case ARRAY_ACCESS:
            ArrayAccessTree arrayAccess = (ArrayAccessTree) tree;
            AnnotatedClassType aaType = getClass(arrayAccess.getExpression());
            // FIXME: Doesn't handle Map<@A K, @B V>[@C]
            AnnotationLocation newRoot =
                AnnotationLocation.fromArray(new int[] { 0 });
            for (AnnotationData data : aaType.getAnnotationData(true)) {
                AnnotationLocation location = data.getLocation();
                if (location.isSubtreeOf(newRoot)) {
                    type.include(asSubOf(data, newRoot));
                }
            }
        default:
            if (tree instanceof LiteralTree) break;
        }
        if (symExpression != null) {
            @Nullable Element sym = InternalUtils.symbol(symExpression);
            if (sym != null)
                aliasFromElement(sym, type);
        }

        annotateDefaults(tree, type);
        return type;
    }

    /**
     * Adapts {@link applyDefaults} for {@link Element} instances.
     *
     * @param elt the element associated with the type being checked
     */
    private void annotateDefaults(Element elt, AnnotatedClassType cls) {

        AnnotationUtils aUtils = new AnnotationUtils(this.env);

        Map<TypeElement, Set<DefaultLocation>> defaults 
            = aUtils.findDefaultLocations(elt);

        applyDefaults(defaults, elt.asType(), cls);
    }

    /**
     * Adapts {@link applyDefaults} for {@link Tree} instances.
     */
    private void annotateDefaults(Tree tree, AnnotatedClassType cls) {

        AnnotationUtils aUtils = new AnnotationUtils(this.env);

        @Nullable TreePath path = this.trees.getPath(root, tree);
        if (path == null) return;

        @Nullable TypeMirror type = this.trees.getTypeMirror(path);
        if (type == null)
            return;

        Map<TypeElement, Set<DefaultLocation>> defaults 
            = aUtils.findDefaultLocations(path);

        applyDefaults(defaults, type, cls);
    }
     
    /**
     * Includes default annotations on an {@link AnnotatedClassType}.
     *
     * @param defaults a mapping from annotations (as {@link TypeElement}s) to
     *        the set of locations where the annotations should be included
     * @param type the type associated with {@code cls} (used as a source of
     *        generic type information)
     * @param cls the type to which the default annotations should be added
     */
    private final void applyDefaults(Map<TypeElement, Set<DefaultLocation>> defaults,
            TypeMirror type, AnnotatedClassType cls) {

        TypesUtils tUtils = new TypesUtils(this.env);
        @Nullable TypeMirror theType;
        if (type instanceof ExecutableType)
            theType = ((ExecutableType)type).getReturnType();
        else
            theType = type;
        assert theType != null; /*nninvariant*/

        for (Map.Entry<TypeElement, Set<DefaultLocation>> entry : 
                defaults.entrySet()) {

            TypeElement aElt = entry.getKey();
            String aName = aElt.getQualifiedName().toString();

            Set<DefaultLocation> locations = entry.getValue();

            if (locations.contains(DefaultLocation.ALL)) {
                if (!cls.hasAnnotationAt(Nullable.class, AnnotationLocation.RAW))
                    cls.include(annotations.createAnnotation(aName, AnnotationLocation.RAW));
                for (AnnotationLocation loc : tUtils.allLocations(theType))
                    if (!cls.hasAnnotationAt(Nullable.class, loc))
                        cls.include(annotations.createAnnotation(aName, loc));
            }
        }

    }

    /**
     * A utility method that returns a new {@code AnnotationData} as if it is rooted at the given newRoot.
	 * This requires that the location of data is a subroot of newRoot.
	 *
	 * @param data	the AnnotationData to be copied
	 * @param newRoot	the new {@code AnnotationData} root
	 * @return a new {@code AnnotationData} of the same type and values as data with the new location
	 */
	 // TODO: Seriously consider moving this to a utilities class
    @Deprecated
    public AnnotationData asSubOf(AnnotationData data, AnnotationLocation newRoot) {
        assert data.getLocation().isSubtreeOf(newRoot);
        List<Integer> oldLocationList = data.getLocation().asList();
        List<Integer> newLocationList = data.getLocation().asList().subList(newRoot.asList().size(), oldLocationList.size());
        AnnotationLocation newLocation = AnnotationLocation.fromList(newLocationList);
        @Nullable TypeElement te = (TypeElement)((DeclaredType)data.getType()).asElement();
        assert te != null; /*nninvariant*/
        @Nullable Name dataName  = te.getQualifiedName();
        assert dataName != null; /*nninvariant*/
        return annotations.createAnnotation(dataName, newLocation, data.getValues());
    }

    /**
     * Produces a list for the annotated types of each {@code throws} clause
     * for the given method.
     *
     * @param method the {@link Element} to use as the annotation source
     * @return the annotated types of each {@code throws} clause of the given
     *         method
     */
    protected List<AnnotatedClassType> throwsTypes(ExecutableElement method) {

        List<InternalAnnotation> throwsAnnos = new LinkedList<InternalAnnotation>();
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            InternalAnnotation a = annotations.createAnnotation(mirror);
            if (!a.isExtended()) continue;

            @Nullable AnnotationTarget target = a.getTarget();
            if (target == null) continue; /*nnbug*/
            if (target.type == TargetType.THROWS)
                throwsAnnos.add(a);
        }

        List<AnnotatedClassType> throwsTypes = new LinkedList<AnnotatedClassType>();
        List<? extends @Nullable TypeMirror> thrown = method.getThrownTypes();
        for (int i = 0; i <thrown.size(); i++) {
            AnnotatedClassType aType = createClassType(env);
            for (InternalAnnotation a : throwsAnnos) {
                @Nullable AnnotationTarget target = a.getTarget();
                if (target == null) continue; /*nnbug*/
                if (target.parameter == i)
                    aType.annotate(a);
            }
            aType.setElement(method);
            aType.setUnderlyingType(method.getReturnType());
            throwsTypes.add(aType);
        }

        return Collections.<@NonNull AnnotatedClassType>unmodifiableList(throwsTypes);
    }

    /**
     * Produces the annotated type of the given method's return value.
     *
     * @param method the {@link Element} to use as the annotation source
     * @return the annotated type of the method's return value
     */
    protected AnnotatedClassType returnType(ExecutableElement method) {

        AnnotatedClassType aType = createClassType(env);
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            InternalAnnotation a = annotations.createAnnotation(mirror);
            // only annotate for non-extended (raw return type) and return generic
            @Nullable AnnotationTarget target = a.getTarget();
            if (target == null) continue; /*nnbug*/
            if (!a.isExtended() || target.type
                    == TargetType.METHOD_RETURN_GENERIC_OR_ARRAY)
                aType.annotate(a);
        }

        aType.setElement(method);
        aType.setUnderlyingType(method.getReturnType());
        annotateDefaults(method, aType);
        return aType;
    }

    @Deprecated
    protected AnnotatedClassType returnType(AnnotatedClassType type, ExecutableElement method) {
        AnnotatedClassType aType = returnType(method);
        return aType;
    }

    protected AnnotatedClassType receiverType(ExecutableElement method) {

        AnnotatedClassType aType = createClassType(env);
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            InternalAnnotation a = annotations.createAnnotation(mirror);
            if (!a.isExtended()) continue;
            @Nullable AnnotationTarget target = a.getTarget();
            if (target == null) continue;
            if (target.type == TargetType.METHOD_RECEIVER)
                aType.annotate(a);
        }

        aType.setElement(method);
        aType.setUnderlyingType(ElementUtils.enclosingClass(method).asType());
        return aType;
    }

    protected List<AnnotatedClassType> paramTypes(ExecutableElement method) {

        List<AnnotatedClassType> paramTypes = new LinkedList<AnnotatedClassType>();
        int i = 0;
        for (List<? extends InternalAnnotation> lst : InternalAnnotation
                .forMethodParameters(method, env)) {
            AnnotatedClassType paramType = createClassType(env);
            for (InternalAnnotation a : lst) {
                paramType.annotate(a);
            }
            paramType.setElement(method);
            @Nullable Element p = method.getParameters().get(i);
            assert p != null; /*nninvariant*/
            paramType.setUnderlyingType(p.asType());
            annotateDefaults(p, paramType);
            paramTypes.add(paramType);
            i++;
        }
        return paramTypes;
    }

    @Deprecated
    protected List<AnnotatedClassType> paramTypes(AnnotatedClassType type, ExecutableElement method) {

        List<AnnotatedClassType> paramTypes = paramTypes(method);
        return paramTypes;
    }

    public boolean isOuterClassReference(MemberSelectTree ms) {

        if (ms == null || !(ms.getExpression() instanceof MemberSelectTree))
            return false;

        MemberSelectTree tms = (MemberSelectTree)ms.getExpression();

        if (tms == null || !(tms.getExpression() instanceof IdentifierTree))
            return false;

        IdentifierTree idf = (IdentifierTree)tms.getExpression();
        @Nullable Element idsym = InternalUtils.symbol(idf);
        return (idsym != null && idsym.getKind() == ElementKind.CLASS && tms.getIdentifier().contentEquals("this"));
    }

    /**
     * Creates an {@link AnnotatedMethodType} from the given {@link Tree} node.
     *
     * @param tree
     *            a tree node
     * @return the {@link AnnotatedMethodType} corresponding to the given tree
     *         node and the annotations written on its various parts
     */
    public AnnotatedMethodType getMethod(@Nullable Tree tree) {

        AnnotatedMethodType type = createMethodType(env);

        ExecutableElement method = methodSymbol(tree);
        if (method == null)
            return type;

        AnnotatedClassType select = methodSelect(tree);

        type.returnType = returnType(select, method);
        type.receiverType = receiverType(method);
        type.paramTypes = paramTypes(select, method);
        type.throwsTypes = throwsTypes(method);
        type.setElement(method);

        if (trees.getTree(method) != null) {
            MethodTree methodTree = trees.getTree(method);
            type.returnType.setTree(methodTree.getReturnType());
            type.receiverType.setTree(null);
            
            for (int i = 0; i < methodTree.getParameters().size(); ++i) {
                @Nullable Tree p = methodTree.getParameters().get(i);
                assert p != null; /*nninvariant*/
                type.paramTypes.get(i).setTree(p);
            }
            for (int i = 0; i < methodTree.getThrows().size(); ++i) {
                @Nullable Tree t = methodTree.getThrows().get(i);
                assert t != null; /*nninvariant*/
                type.throwsTypes.get(i).setTree(t);
            }
        }
        
        if (tree != null && select.getElement() != null) {
            assert tree != null; // FIXME: flow workaround
            updateMethodGenerics(method, tree, select, type);
        }

        return type;
    }

    protected AnnotatedClassType methodSelect(@Nullable Tree tree) {
        AnnotatedClassType select;
        @Nullable MethodInvocationTree mi = null;
        if (tree instanceof MethodInvocationTree) {
            mi = (MethodInvocationTree)tree;
            if (mi != null && mi.getMethodSelect() instanceof MemberSelectTree) {
                @Nullable MemberSelectTree ms = (MemberSelectTree)mi.getMethodSelect();
                assert ms != null; /*nninvariant*/
                select = getClass(ms.getExpression());
            } else
                select = createClassType(env);

        } else
            select = createClassType(env);
        return select;
    }

    protected ExecutableElement methodSymbol(@Nullable Tree tree) {
        // Attempt to get the symbol for the tree; if it's not the symbol
        // for a method (an "executable element") try to get the symbol for
        // the tree's enclosing method. If that doesn't work, fail.
        @Nullable Element e = InternalUtils.symbol(tree);
        if (!(e instanceof ExecutableElement)) {
            @Nullable TreePath path = TreePath.getPath(root, tree);
            e = InternalUtils.symbol(InternalUtils.enclosingMethod(path));
        }
        if (!(e instanceof ExecutableElement))
            throw new IllegalArgumentException("no symbol for tree"); /*nnbug*/

        return (@NonNull ExecutableElement) e;
    }

    protected void updateMethodGenerics(ExecutableElement method, Tree tree,
            AnnotatedClassType select, AnnotatedMethodType type) {

        GenericsUtils g = new GenericsUtils(env, this);

        // TODO: this should eventually be moved to GenericsUtils, perhaps
        // Build a list of additional mappings for method type parameters.
        @Nullable TreePath path = TreePath.getPath(root, tree);
        @Nullable Element elt = InternalUtils.enclosingSymbol(path);
        assert elt != null; /*nninvariant*/

        Map<TypeMirror, Set<AnnotationData>> matches = 
            new HashMap<TypeMirror, Set<AnnotationData>>();
        for (AnnotationMirror a : elt.getAnnotationMirrors()) {
            InternalAnnotation ia = annotations.createAnnotation(a);
            @Nullable AnnotationTarget at = ia.getTarget();
            if (at == null) continue; /*nnbug*/
            if (InternalUtils.refersTo(at.ref, tree) &&
                    at.type == TargetType.METHOD_TYPE_ARGUMENT) {
                @Nullable TypeParameterElement tpe = method.getTypeParameters().get(at.parameter);
                assert tpe != null; /*nninvariant*/
                @Nullable TypeMirror t = tpe.asType();
                assert t != null; /*nninvariant*/ 
                matches.put(t, Collections.<@NonNull AnnotationData>singleton(ia));
            }
        }
        
        // For the return type.
        Set<AnnotationData> returnAnnos = g.annotationsFor(
                method.getReturnType(), select.getElement(), matches, true);
        for (AnnotationData a : returnAnnos)
            type.returnType.annotate(a);

        // For formal parameters.
        int i = 0;
        for (VariableElement var : method.getParameters()) {
            Set<AnnotationData> paramAnnos = g.annotationsFor(
                    var.asType(), select.getElement(), matches, true);
            for (AnnotationData a : paramAnnos)
                type.paramTypes.get(i).annotate(a);
            i++;
        }
    }

    public AnnotatedMethodType getMethod(ExecutableElement method) {

        // Trees is more informative
        if (trees.getTree(method) != null)
            return getMethod(trees.getTree(method));

        AnnotatedMethodType type = createMethodType(env);
        type.returnType = returnType(method);
        type.receiverType = receiverType(method);
        type.paramTypes = paramTypes(method);
        type.throwsTypes = throwsTypes(method);
        type.setElement(method);
        return type;
    }
    
    public CompilationUnitTree getCompilationUnitTree() {
        return this.root;
    }
}
