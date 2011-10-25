package checkers.igj;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.comp.TargetType;

import checkers.igj.quals.*;
import checkers.types.*;
import checkers.util.*;
import checkers.util.GenericsUtils.LocationVisitor;

import static checkers.igj.IGJImmutability.*;

/**
 * Adds support for IGJ annotations ({@code @ReadOnly},
 * {@code @Mutable}, {@code @Immutable}) to {link
 * AnnotatedTypeFactory}. This means that the
 * {@link IGJAnnotatedTypeFactory#getClass} and
 * {@link IGJAnnotatedTypeFactory#getMethod} methods will infer IGJ
 * immutability type even if they do not carry an IGJ annotation.
 * There include:
 * 
 * <ul>
 * <li>primitive, class, and enum types
 * <li>the results of binary operations
 * <li>literals (except the null literal) and arrays of literals
 * </ul>
 */
public class IGJAnnotatedTypeFactory extends AnnotatedTypeFactory {

    /** Default IGJ Immutability Annotation for method receivers * */
    protected final static Class<? extends Annotation> DEFAULT_METHOD_RECEIVER_ANNOTATION =
        Mutable.class;

    protected final static Class<? extends Annotation> DEFAULT_CONSTRUCTOR_RECEIVER_ANNOTATION =
        AssignsFields.class;
    
    /** Default IGJ Immutability Annotation for classes and instances * */
    protected final static Class<? extends Annotation> DEFAULT_CLASS_ANNOTATION =
        Mutable.class;

    /** Default IGJ Immutability Annotation for new class trees * */
    protected final static Class<? extends Annotation> DEFAULT_NEW_CLASS_ANNOTATION =
        IGJPlaceHolder.class;

    // TODO: Refactor code
    protected VisitorState state;

    public IGJAnnotatedTypeFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        super(env, root);
    }

    @Override
    public AnnotatedClassType getClass(Element element) {
        AnnotatedClassType type = super.getClass(element);
        
        if (element.getKind() != ElementKind.CLASS
                && element.getKind() != ElementKind.INTERFACE) {
            for (AnnotationLocation loc : 
                immutableFinder.visit(element.asType()))
                if (getIGJAnnotationDataAt(type, loc) == null)
                    type.includeAt(Immutable.class, loc);
        } else {
            if (isImmutable(element.asType(), false) && 
                    getIGJAnnotationDataAt(type, AnnotationLocation.RAW) == null)
                type.includeAt(Immutable.class, AnnotationLocation.RAW);
        }
        
        enrichWithDefault(type);
        return type;
    }

    private ClassTypeVisitor classTypeVisitor = new ClassTypeVisitor(this);

    private void enrichWithDefault(AnnotatedClassType type) {
        // If we don't have it
        Set<AnnotationLocation> validLocs = 
            type.getUnderlyingType() == null ? 
                    Collections.singleton(AnnotationLocation.RAW) :
                        TypesUtils.getValidLocations(type.getUnderlyingType());

        for (AnnotationLocation loc : validLocs) {
            if (getIGJAnnotationDataAt(type, loc) == null) {
                if (loc.getTypeFrom(type.getUnderlyingType()) != null &&
                    loc.getTypeFrom(type.getUnderlyingType()).getKind() == TypeKind.TYPEVAR)
                    continue;
                type.includeAt(Mutable.class, loc);
            }
        }
    }
    
    private void enrichWithDefault(AnnotatedMethodType method) {
        if (method.getElement().getReturnType() != null &&
                method.getElement().getReturnType().getKind() != TypeKind.VOID)
            enrichWithDefault(method.getAnnotatedReturnType());
        for (AnnotatedClassType param: method.getAnnotatedParameterTypes())
            enrichWithDefault(param);
    }
    
    @Override
    public AnnotatedClassType getClass(Tree tree) {
        AnnotatedClassType type = super.getClass(tree);
        
        type = classTypeVisitor.visit(tree, type);

        // Handle primitives
        if (tree.getKind() != Tree.Kind.CLASS){
            for (AnnotationLocation loc : 
                immutableFinder.visit(type.getUnderlyingType(), null)) {
                if (getIGJAnnotationDataAt(type, loc) == null)
                    type.includeAt(Immutable.class, loc);
            }
        }
        
        enrichWithDefault(type);
        return type;
    }

    @Override
    public AnnotatedMethodType getMethod(Tree tree) {
        AnnotatedMethodType method;
        if (tree.getKind() == Tree.Kind.NEW_CLASS) {
            ExecutableElement constructor = InternalUtils.constructor((NewClassTree)tree);
            
            method = getMethod(constructor);
            
            // if Receiver not specified
            final AnnotatedClassType receiverType = method.getAnnotatedReceiverType();
            AnnotationData anno = getIGJAnnotationDataAt(receiverType, AnnotationLocation.RAW);
            
            final AnnotatedClassType returnType = method.getAnnotatedReturnType();
            
            switch (IGJImmutability.valueOf(anno)) {
            case MUTABLE:
            case IMMUTABLE:
                returnType.include(anno);
                break;
            default:
                AnnotatedClassType consEncClass = getClass(ElementUtils.enclosingClass(constructor));
                final AnnotationData properReturnAnnotation = 
                    getIGJAnnotationDataAt(consEncClass, AnnotationLocation.RAW);
                returnType.include(properReturnAnnotation);                    
            }
        } else {
            method = super.getMethod(tree);
        }
        
        if (tree.getKind() != Tree.Kind.METHOD_INVOCATION
                && tree.getKind() != Tree.Kind.NEW_CLASS) {
            enrichWithDefault(method);
            return method;
        }
        
        Set<AnnotationData> restricted = new HashSet<AnnotationData>();
        List<? extends ExpressionTree> passedArgs;
        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            MethodInvocationTree methodInvTree = (MethodInvocationTree) tree;
            // Resolve
            if (!ElementUtils.isStatic(method.getElement())) {
                resolveBindings(method, getBinding(methodInvTree.getMethodSelect()));
                restricted.addAll(getBinding(methodInvTree.getMethodSelect()).values());
            }
            passedArgs = methodInvTree.getArguments();
        } else
            passedArgs = ((NewClassTree)tree).getArguments();
        
        List<AnnotatedClassType> arguments =
            new ArrayList<AnnotatedClassType>();
        for (Tree arg : passedArgs)
            arguments.add(getClass(arg));
        resolveBindings(method, arguments, restricted);
        
        AnnotatedClassType returnType = method.getAnnotatedReturnType();
        // replace any left wildcards with placement holder
        for (AnnotationData wildcard : returnType.getAnnotationData(I.class,
                true)) {
            if (!restricted.contains(wildcard)) {
                returnType.exclude(wildcard);
                returnType.include(annotations.createAnnotation(
                        IGJPlaceHolder.class.getCanonicalName(), wildcard
                        .getLocation()));
            }
        }
        
        // enriching defaults
        enrichWithDefault(method);
        return method;
    }
    
    @Override
    protected List<AnnotatedClassType> paramTypes(ExecutableElement method) {
        List<AnnotatedClassType> paramTypes =
            new ArrayList<AnnotatedClassType>();
        for (VariableElement varElement : method.getParameters()) {
            paramTypes.add(getClass(varElement));
        }
        return paramTypes;
    }

    void setVisitorState(VisitorState state) {
        this.state = state;
        classTypeVisitor.state = state;
    }

    @Override
    protected AnnotatedClassType returnType(ExecutableElement method) {
        AnnotatedClassType returnType = super.returnType(method);
        Set<AnnotationLocation> locs = immutableFinder.visit(method.getReturnType(), null);
        for (AnnotationLocation loc : locs) {
            if (getIGJAnnotationDataAt(returnType, loc) == null)
                returnType.includeAt(Immutable.class, loc);
        }
        if (method.getKind() != ElementKind.CONSTRUCTOR &&
                getIGJAnnotationDataAt(returnType, AnnotationLocation.RAW) == null)
            returnType.include(Mutable.class);
        return returnType;
    }
    
    private Map<String, AnnotationData> getBinding(ExpressionTree tree) {
        if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            IdentifierTree iden = (IdentifierTree)tree;
            Element elem = InternalUtils.symbol(tree);
            if (ElementUtils.isStatic(elem))
                return Collections.emptyMap();
            if (iden.getName().contentEquals("this") || 
                    iden.getName().contentEquals("super") ||
                    elem.getKind() == ElementKind.METHOD ||
                    elem.getKind() == ElementKind.FIELD) {
                AnnotatedClassType classType =
                    getClass(TreeUtils.enclosingClass(TreePath.getPath(root, tree)));
                return getBinding(getSelfType(classType));
            }
        } else if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            ExpressionTree enclosingSelect = ((MemberSelectTree)tree).getExpression();
            return getBinding(getClass(enclosingSelect));
        }
        return Collections.emptyMap();
    }
    private Map<String, AnnotationData> getBinding(AnnotatedClassType type) {
        AnnotationData annotation = getIGJAnnotationDataAt(type, AnnotationLocation.RAW);
        
        final  TypeMirror underlyingType = type.getUnderlyingType();
        if (underlyingType == null || underlyingType.getKind() != TypeKind.DECLARED)
            return Collections.emptyMap();
        
        AnnotatedClassType classType = getClass(((DeclaredType)underlyingType).asElement());
        AnnotationData anno = getIGJAnnotationDataAt(classType, AnnotationLocation.RAW);
        if (isWildcard(anno))
            return Collections.singletonMap(getID(anno), annotation);

        return Collections.emptyMap();
    }
    
    
    @Override
    protected AnnotatedClassType receiverType(ExecutableElement method) {
        AnnotatedClassType receiverType = super.receiverType(method);

        
        if (getIGJAnnotationDataAt(receiverType,
                AnnotationLocation.RAW) != null) 
            return receiverType;

        IGJImmutability classImmutability =
            getIGJImmutabilityAt(
                    getClass(ElementUtils.enclosingClass(method)),
                    AnnotationLocation.RAW);

        if (classImmutability == IMMUTABLE) {
            // Include ReadOnly instead of Immutable to ease inference
            // Notice: Annotate
            if (method.getKind() == ElementKind.CONSTRUCTOR)
                receiverType.include(annotations.createAnnotation(
                        DEFAULT_CONSTRUCTOR_RECEIVER_ANNOTATION.getCanonicalName(),
                        AnnotationLocation.RAW));
            else
                receiverType.include(annotations.createAnnotation(ReadOnly.class.getCanonicalName(), AnnotationLocation.RAW));
                
            receiverType.include(annotations.createAnnotation(
                    (method.getKind() == ElementKind.CONSTRUCTOR ? DEFAULT_CONSTRUCTOR_RECEIVER_ANNOTATION : ReadOnly.class)
                    .getCanonicalName(),
                    AnnotationLocation.RAW));
        } else if (classImmutability == MUTABLE) {
            receiverType.include(Mutable.class);
        } else if (method.getKind() == ElementKind.CONSTRUCTOR) {
                receiverType.include(DEFAULT_CONSTRUCTOR_RECEIVER_ANNOTATION);
        } else {
            receiverType.include(DEFAULT_METHOD_RECEIVER_ANNOTATION);
        }

        return receiverType;
    }
    
    /**
     * Finds the union of the two {@code AnnotationData} sets of IGJ
     * immutability annotations ({@code ReadOnly}, {@code Immutable},
     * and {@code Mutable}).
     * 
     * If a location has multiple annotations, assigns a
     * {@code ReadOnly} annotation to the location.
     * 
     * @param c1
     *            a {@code Set} of IGJ {@code AnnotationData}s
     * @param c2
     *            a {@code Set} of IGJ {@code AnnotationData}s
     * @return the union of the two {@code Set}s
     */
    protected Set<AnnotationData> union(Collection<AnnotationData> c1,
            Collection<AnnotationData> c2) {
        Map<AnnotationLocation, AnnotationData> union =
            new HashMap<AnnotationLocation, AnnotationData>();

        for (AnnotationData annon : c1)
            union.put(annon.getLocation(), annon);

        for (AnnotationData annon : c2) {
            AnnotationLocation location = annon.getLocation();
            AnnotationData oldAnnon = union.put(location, annon);
            if (oldAnnon != null && !IGJImmutability.isEqual(oldAnnon, annon)) {
                union.put(location, annotations.createAnnotation(
                        ReadOnly.class.getCanonicalName(), location));
            }
        }

        Set<AnnotationData> results =
            new HashSet<AnnotationData>(union.values());
        return results;
    }

    /**
     * Resolves the binding for the passed methodType, by replacing
     * the wildcard annotations with the corresponding annotations
     * using the bindings.
     * 
     * @param methodType
     *            the method type to be resolved
     * @param bindings
     *            the wildcard ID's mapped to the corresponding
     *            annotation
     * @return true if any wildcard has been replaced/resolved.
     */
    protected boolean resolveBindings(AnnotatedMethodType methodType,
            Map<String, AnnotationData> bindings) {
        if (methodType == null || bindings == null)
            return false;
        boolean result = false;

        // TODO: Clone method
        AnnotatedMethodType method = methodType;

        // No need to resolve receiver! Yay! Less Coding!
        // IGJ prohibits Threw Annotation

        resolveBindings(method.getAnnotatedReturnType(), bindings);
        for (AnnotatedClassType type : method.getAnnotatedParameterTypes()) {
            // Be aware of order
            result = resolveBindings(type, bindings) || result;
        }

        return result;
    }

    /**
     * Resolves the wildcards in type, based on the mappings in
     * bindings.
     * 
     * When Called on
     * 
     * {@code @I("I") Map<@I("T") Date, @I("G") Calendar>}, with the following
     *       bindings: "T" = {@code @Immutable } "G" = {@code @ReadOnly }
     * 
     * it'll mutate type to
     * {@code @I("T") Map<@Immutable Date, @ReadOnly Calendar> }
     * 
     * @param type
     * @param bindings
     * @return true iff any wildcard has been resolved
     */
    protected boolean resolveBindings(AnnotatedClassType type,
            Map<String, AnnotationData> bindings) {
        if (type == null || bindings == null)
            return false;

        boolean result = false;
        for (AnnotationData annotation : type
                .getAnnotationData(I.class, true)) {
            String value = IGJImmutability.getID(annotation);
            AnnotationData resolvedAnnotation = bindings.get(value);
            if (resolvedAnnotation == null)
                continue; // We cannot get rid of this!

            AnnotationData newAnnotation =
                annotations.createAnnotation(IGJImmutability
                        .getAnnotationTypeName(resolvedAnnotation),
                        annotation.getLocation(), resolvedAnnotation
                                .getValues());
            type.exclude(annotation);
            type.include(newAnnotation);
            result = true;
        }
        return result;
    }

    /**
     * Resolves the binding for a method based on the arguments that
     * are based on the arguments that are passed to it. It resolves
     * the wildcard to the most restrictive annotation.
     * 
     * It replaces all unresolved wildcards with an
     * {@code IGJPlaceHolder}.
     * 
     * @param method
     *            the method to be resolved
     * @param arguments
     *            {@code AnnotatedClassType}'s of the arguments
     *            passed to the method in the invocation in the same
     *            order
     * @return true iff any wildcard is resolved
     */
    protected boolean resolveBindings(AnnotatedMethodType method,
            List<AnnotatedClassType> arguments, Set<AnnotationData> restricted) {
        assert method.getAnnotatedParameterTypes().size() == arguments.size();

        // First Pass : Figure out bindings
        Map<String, AnnotationData> bindings =
            new HashMap<String, AnnotationData>();
        List<AnnotatedClassType> requiredArgs =
            method.getAnnotatedParameterTypes();
        for (int i = 0; i < requiredArgs.size(); ++i) {
            AnnotatedClassType requiredArg = requiredArgs.get(i);
            AnnotatedClassType actualArg = arguments.get(i);

            for (AnnotationData wildcard : requiredArg.getAnnotationData(
                    I.class, true)) {
                AnnotationData actualAnnotation =
                    getIGJAnnotationDataAt(actualArg,
                            wildcard.getLocation());
                String id = IGJImmutability.getID(wildcard);
                AnnotationData previous = bindings.put(id, actualAnnotation);
                if (previous != null
                        && !IGJImmutability.isSubtype(actualAnnotation,
                                previous)) {
                    bindings.put(id, annotations.createAnnotation(
                            ReadOnly.class.getCanonicalName(),
                            AnnotationLocation.RAW, null));
                }
            }
        }

        // SecondPass: resolve
        boolean result = false;
        for (AnnotatedClassType requiredArg : requiredArgs) {
            // Watch for order
            result = resolveBindings(requiredArg, bindings) || result;
        }
        // Watch for order
        AnnotatedClassType returnType = method.getAnnotatedReturnType();
        result = resolveBindings(returnType, bindings) || result;
        restricted.addAll(bindings.values());
        return result;
    }
    
    /*
     * This exists to fix a bug in the Checkers framework. Need to determine
     * what the expected behavior is
     */
    @Override
    protected void aliasFromElement(Element elt, AnnotatedClassType type) {
        if (elt == null)
            return;

        Element te = env.getTypeUtils().asElement(elt.asType());
        if (te != null) {
            for (AnnotationMirror mirror : te.getAnnotationMirrors()) {
                InternalAnnotation anno = annotations.createAnnotation(mirror);
                if (anno.getTarget().type == TargetType.CLASS_EXTENDS || anno.getTarget().type == TargetType.CLASS_EXTENDS_GENERIC_OR_ARRAY) {
                    if (isIGJAnnotation(anno) && getIGJImmutabilityAt(type, anno.getLocation()) != null)
                        continue;
                    type.annotate(anno);
                }
            }
        }

    }

    /**
     * A location visitor that finds the location of the immutable
     * types by default.
     * 
     * The visitor returns a set of the primitives (or their boxed
     * types) and String types.
     */
    private TypeVisitor<Set<AnnotationLocation>, Void> immutableFinder =
        new LocationVisitor<Set<AnnotationLocation>, Void>() {

            @Override
            protected Set<AnnotationLocation> reduce(
                    Set<AnnotationLocation> s1, Set<AnnotationLocation> s2) {
                Set<AnnotationLocation> result =
                    new HashSet<AnnotationLocation>();
                if (s1 != null)
                    result.addAll(s1);
                if (s2 != null)
                    result.addAll(s2);
                return result;
            }

            protected Set<AnnotationLocation> defaultAction(TypeMirror e,
                    Void p) {
                return Collections.<AnnotationLocation> emptySet();
            }

            /*
             * If the "deep" component of the array is immutable, it adds to
             * the return list the location of the last level.
             * 
             * Note that this only work if we have the type of the entire
             * array.
             * 
             * TODO: Todo check for partial array type.
             */
            public Set<AnnotationLocation> visitArray(ArrayType t, Void p) {
                // TODO: Test this
                // Assume that we have the entire array!

                // Find the dimension of the array
                // and whether it is an array of String or primitives
                TypeMirror component = TypesUtils.getDeepComponent(t);
                int dimensions = TypesUtils.getArrayDimensions(t);
                int locationOffset = 
                    (component.getKind() != TypeKind.DECLARED) ? 0 :
                        ((DeclaredType)component).getTypeArguments().size();
                Set<AnnotationLocation> results =
                    new HashSet<AnnotationLocation>();
                
                if (component.getKind() == TypeKind.DECLARED) {
                    for (AnnotationLocation loc : scan(component, p)) {
                        if (!loc.equals(AnnotationLocation.RAW))
                            results.add(loc);
                    }
                }
                
                if (isImmutable(component, true)) {
                    List<Integer> newLoc =
                        new ArrayList<Integer>(getCurrentLocation().asList());
                    newLoc.add(locationOffset + dimensions - 1);
                    results =  reduce(results, Collections.singleton(
                            AnnotationLocation.fromList(newLoc)));
                }
                
                
                return results;
            }

            public Set<AnnotationLocation> visitDeclared(DeclaredType t,
                    Void p) {
                // Need to take care of String Literals too!
                Set<AnnotationLocation> result = Collections.emptySet();                
                // is a Lateral
                if (isImmutable(t, true))
                    result = Collections.singleton(getCurrentLocation());
                
                return reduce(result, super.visitDeclared(t, p));
            }

            @Override
            public Set<AnnotationLocation> visitPrimitive(PrimitiveType t, Void p) {
                return Collections.singleton(getCurrentLocation());
            }
            
            public Set<AnnotationLocation> visitTypeVariable(TypeVariable t,
                    Void p) {
                return scan(t.getUpperBound(), p);
            }

            public Set<AnnotationLocation> visitWildcard(WildcardType t,
                    Void p) {
                // Not quite right
                if (t.getSuperBound() == null || t.getSuperBound().getKind() == TypeKind.TYPEVAR)
                    return Collections.emptySet();
                else
                    return scan(t.getSuperBound(), p);
            }
        };
        
        @Deprecated
        private boolean isBoxedPrimitive(TypeMirror t) {
            // worry about boxing!
            // TODO: Remove this once the JDK is annotated
            try {
                return (env.getTypeUtils().unboxedType(t) != null);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        
        protected boolean isImmutable(TypeMirror t, boolean declared) {
            final boolean isPrimitive = t.getKind().isPrimitive();
            final boolean isString = 
                (t.getKind() == TypeKind.DECLARED) &&
                        TypesUtils.getQualifiedName((DeclaredType)t).contentEquals("java.lang.String");
            final boolean isDeclaredImmutable =
                declared && (t.getKind() == TypeKind.DECLARED) &&
                    getClass(((DeclaredType)t).asElement())
                    .hasAnnotationAt(Immutable.class, AnnotationLocation.RAW);
            final boolean isBoxedPrimitive = isBoxedPrimitive(t);

            return (isPrimitive || isString || isBoxedPrimitive || isDeclaredImmutable);
        }
        
        protected AnnotatedClassType getSelfType(AnnotatedClassType classType) {
            IGJImmutability thisImmutability = state.thisImmutability;
            if (thisImmutability == IGJImmutability.ASSIGNSFIELDS)
                thisImmutability = IGJImmutability.READONLY;
            
            if (thisImmutability == IGJImmutability.READONLY) {
                if (classType.hasAnnotationAt(I.class, AnnotationLocation.RAW) ||
                        classType.hasAnnotationAt(Immutable.class, AnnotationLocation.RAW))
                    return classType;
            }

            classType.exclude(getIGJAnnotationDataAt(
                    classType, AnnotationLocation.RAW));
            classType.include(annotations.createAnnotation(
                    thisImmutability.getAnnotation().getCanonicalName(), AnnotationLocation.RAW));

            return classType;
        }


    /**
     * 
     * A helper class that returns the type that corresponds to the tree.
     */
    private static class ClassTypeVisitor extends
            SimpleTreeVisitor<AnnotatedClassType, AnnotatedClassType> {

        IGJAnnotatedTypeFactory factory;
        VisitorState state;

        public ClassTypeVisitor(IGJAnnotatedTypeFactory factory) {
            this.factory = factory;
            this.state = factory.state;
        }

        @Override
        protected AnnotatedClassType defaultAction(Tree tree,
                AnnotatedClassType type) {
            TypeMirror t = factory.trees.getTypeMirror(TreePath.getPath(factory.root, tree));
            if (t != null) {
                if (t.getKind() == TypeKind.NULL)
                    type.include(IGJPlaceHolder.class);
                else if (t.getKind().isPrimitive())
                    type.include(Immutable.class);
            }
            // Default action. Simply return the same passed type
            return type;
        }

        @Override
        public AnnotatedClassType visitNewClass(NewClassTree tree,
                AnnotatedClassType type) {
            if (getIGJAnnotationDataAt(type, AnnotationLocation.RAW) == null)
                type.include(getIGJAnnotationDataAt(factory.getMethod(tree).getAnnotatedReturnType(), AnnotationLocation.RAW));
            return type;
        }

        @Override
        public AnnotatedClassType visitIdentifier(IdentifierTree tree,
                AnnotatedClassType type) {

            if (tree.getName().contentEquals("this") ||
                    tree.getName().contentEquals("super")) {
                return factory.getSelfType(factory.getClass(
                        TreeUtils.enclosingClass(TreePath.getPath(factory.root, tree))));
            } else if (type.getElement().getKind() == ElementKind.FIELD) {
                // Trick does not necessary work
                factory.resolveBindings(type, factory.getBinding(tree));
            }

            return type;            
        }


        @Override
        public AnnotatedClassType visitConditionalExpression(
                ConditionalExpressionTree tree, AnnotatedClassType type) {
            // For Conditional expression, find the union of type of true
            // and false expressions
            Set<AnnotationData> trueAnnotations = 
                getIGJAnnotationData(
                        factory.getClass(tree.getTrueExpression()));
            
            Set<AnnotationData> falseAnnotations =
                getIGJAnnotationData(
                        factory.getClass(tree.getFalseExpression()));
            
            Set<AnnotationData> union =
                factory.union(trueAnnotations, falseAnnotations);
            
            for (AnnotationData annon : union)
                type.include(annon);
            
            return type;
        }

        @Override
        public AnnotatedClassType visitClass(ClassTree tree,
                AnnotatedClassType type) {
            if (getIGJAnnotationDataAt(type,
                    AnnotationLocation.RAW) != null) 
                // Already has immutability type! done!
                return type;
            
            TreePath path = TreePath.getPath(factory.root, tree);
            if (TreeUtils.enclosingMethod(path) != null) {
                // Inner class within a method!
                AnnotatedClassType receiverType =
                    factory.getMethod(TreeUtils.enclosingMethod(path))
                    .getAnnotatedReceiverType();
                AnnotationData annotation =
                    getIGJAnnotationDataAt(receiverType,
AnnotationLocation.RAW);
                type.include(annotation);
            } else {
                // TODO: Handle inner static classes if needed!
                type.include(DEFAULT_CLASS_ANNOTATION);
            }
            
            return type;
        }

        @Override
        public AnnotatedClassType visitMethodInvocation(
                MethodInvocationTree tree, AnnotatedClassType type) {
            AnnotatedMethodType method = factory.getMethod(tree);
            return method.getAnnotatedReturnType();
        }

        @Override
        public AnnotatedClassType visitMemberSelect(MemberSelectTree tree,
                AnnotatedClassType type) {
            
            if (type.getElement() != null && 
                    !ElementUtils.isStatic(type.getElement()))
                factory.resolveBindings(type, factory.getBinding(tree));

            return type;
        }
    }
}
