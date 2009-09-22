package checkers.javari;

import java.lang.annotation.Annotation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import java.util.*;

import checkers.quals.*;

import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.*;

/**
 * Adds support for the javari mutability type annotations to {@link
 * AnnotatedTypeFactory}. This means that the {@link
 * JavariAnnotatedTypeFactory#getClass} method will regard some
 * inputs as carrying the appropriate mutability annotation even if
 * they do not carry it explicitly. These include:
 *
 * <ul>
 *  <li>primitive types
 *  <li>inherited mutability annotations
 * </ul>
 */
public class JavariAnnotatedTypeFactory extends AnnotatedTypeFactory {

    /**
     * Creates an annotated type factory that treats certain inputs as having
     * javari mutability annotations even if they do not explicitly carry them.
     *
     * @param env the {@link ProcessingEnvironment} to use for tree and type
     *        utilities
     * @param root the root of the syntax tree (used for TreePaths)
     */
    public JavariAnnotatedTypeFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        super(env, root);
    }

    /**
     * Returns the method overriden by the method passed as a parameter,
     * or null if there is none.
     *
     * @param methodElt an ExecutableElement for a method
     * @return an ExecutableElement representing the method directly
     * overriden by the argument, or null if none.
     */
    public ExecutableElement getOverridenMethod(ExecutableElement methodElt){

        TypeElement classElt = (TypeElement) methodElt.getEnclosingElement();
        TypeMirror classType = classElt.asType();
        List<? extends TypeMirror> supers
            = env.getTypeUtils().directSupertypes(classType);

        for(TypeMirror t : supers) {
            TypeElement superElt
                = (TypeElement) ((DeclaredType)t).asElement();
            List<ExecutableElement> methods
                = ElementFilter.methodsIn(superElt.getEnclosedElements());
            Elements eltUtils = env.getElementUtils();
            for (ExecutableElement e : methods) {
                if (eltUtils.overrides(methodElt, e, classElt))
                    return e;
            }
        }

        return null;
    }


    /**
     * Overrides getClass in AnnotatedTypeFactory to provide:
     *
     * <ul>
     *  <li> special treatment for MemberSelectTree trees
     *  <li> mutability inheritance from enclosing element
     *  <li> resolve mutability of {@code @RoMaybe}
     * </ul>
     *
     * @param tree
     *            a tree node
     * @throws IllegalArgumentException
     *             if {@code tree} is null
     * @return the {@link AnnotatedClassType} corresponding to
     *         the given tree node and the annotations written on it
     */
    public AnnotatedClassType getClass(Tree tree) {
        //if (!(tree instanceof ClassTree) && !(tree instanceof MethodTree))
        //    System.out.println("getClass: " + tree + "/" + tree.getKind());

        if (tree instanceof ArrayAccessTree) {
            return super.getClass(tree);

        } else if (tree instanceof MemberSelectTree) {
            MemberSelectTree mst = (MemberSelectTree)tree;

            AnnotatedClassType outerType =
                getClass(mst.getExpression());

            AnnotatedClassType innerType = super.getClass(tree);

            // if the inner type is specified, return it
            if (innerType.hasAnnotationAt(ReadOnly.class,
                                          AnnotationLocation.RAW)
                || innerType.hasAnnotationAt(Mutable.class,
                                             AnnotationLocation.RAW))
                return innerType;

            if (innerType.hasAnnotationAt(RoMaybe.class,
                                          AnnotationLocation.RAW)
                && outerType.hasAnnotationAt(RoMaybe.class,
                                             AnnotationLocation.RAW))
                return innerType;

            if (outerType.hasAnnotationAt(ReadOnly.class,
                                          AnnotationLocation.RAW))
                innerType.include(ReadOnly.class);

            if (outerType.hasAnnotationAt(RoMaybe.class,
                                          AnnotationLocation.RAW))
                innerType.include(RoMaybe.class);

            if (outerType.hasAnnotationAt(Mutable.class,
                                          AnnotationLocation.RAW))
                innerType.include(Mutable.class);

            return innerType;
        }

        AnnotatedClassType type = super.getClass(tree);

        if (tree instanceof PrimitiveTypeTree) return type;
        else if (tree instanceof TypeCastTree) {
            Tree typeTree = ((TypeCastTree)tree).getType();
            if (typeTree instanceof PrimitiveTypeTree)
                return type;
            if (!(type.hasAnnotationAt(ReadOnly.class,
                                           AnnotationLocation.RAW)
                  || type.hasAnnotationAt(RoMaybe.class,
                                              AnnotationLocation.RAW)
                  || type.hasAnnotationAt(Mutable.class,
                                              AnnotationLocation.RAW)))
                type.include(Mutable.class);
            return type;
        } else if (tree instanceof VariableTree) {
            VariableTree vt = (VariableTree) tree;
            if (vt.getType() instanceof PrimitiveTypeTree)
                return type;
        } else if (tree instanceof LiteralTree
                   || tree instanceof BinaryTree
                   || tree instanceof UnaryTree) {
            type.include(Mutable.class);
            return type;
        } else if (tree instanceof ConditionalExpressionTree) {
            ConditionalExpressionTree cet = (ConditionalExpressionTree) tree;
            combine(getClass(cet.getTrueExpression()),
                    getClass(cet.getFalseExpression()), type);
            return type;

        } else if (tree instanceof MethodInvocationTree){

            MethodInvocationTree mit = (MethodInvocationTree) tree;
            if (type.hasAnnotationAt(RoMaybe.class,
                                     AnnotationLocation.RAW)){
                type = resolveRoMaybe(mit);
                return type;
            }

            if (type.getAnnotatedLocations().isEmpty())
                type.include(Mutable.class);

        } else if (tree instanceof NewClassTree) {
            NewClassTree nct = (NewClassTree) tree;
            type = super.getClass(tree);
            Element classElt = type.getElement();

            // inherit receiver type if class is annotated and receiver is not
            {
                AnnotatedClassType receiverType =
                    getMethod(InternalUtils.constructor(nct)).getAnnotatedReceiverType();

                {
                    AnnotatedClassType classType =
                        getClass(getMethod(InternalUtils.constructor(nct)).getElement().getEnclosingElement());

                    if (receiverType.hasAnnotationAt(ReadOnly.class,
                                                     AnnotationLocation.RAW)
                        || classType.hasAnnotationAt(ReadOnly.class,
                                                     AnnotationLocation.RAW)) {
                        if (type.getAnnotatedLocations().isEmpty())
                            type.includeAt(ReadOnly.class,
                                           AnnotationLocation.RAW);
                    }
                }

                if (receiverType.hasAnnotationAt(RoMaybe.class,
                                                      AnnotationLocation.RAW)) {
                    if (type.getAnnotatedLocations().isEmpty())
                        type.includeAt(RoMaybe.class, AnnotationLocation.RAW);
                }

                else if (receiverType.hasAnnotationAt(Mutable.class,
                                                      AnnotationLocation.RAW)) {
                    if (type.getAnnotatedLocations().isEmpty())
                        type.includeAt(Mutable.class, AnnotationLocation.RAW);
                }

                if (receiverType.hasAnnotationAt(RoMaybe.class,
                                                 AnnotationLocation.RAW))
                    type = resolveRoMaybe(nct);

            }

            if (type.getAnnotatedLocations().isEmpty()) {
                if (nct.getEnclosingExpression() instanceof MemberSelectTree) {
                    MemberSelectTree mst =
                        (MemberSelectTree) (nct.getEnclosingExpression());
                    AnnotatedClassType referenceType =
                        getClass(mst.getExpression());

                    if (referenceType.hasAnnotationAt(ReadOnly.class,
                                                      AnnotationLocation.RAW))
                        type.include(ReadOnly.class);

                    else if (referenceType
                             .hasAnnotationAt(Mutable.class,
                                              AnnotationLocation.RAW))
                        type.include(Mutable.class);

                    return type;
                } else {
                    type.include(Mutable.class);
                }
            }
        }

        completeThisMutable(type);

        if (!type.getAnnotatedLocations().isEmpty())
            return type;

        Element elt = type.getElement();
        if (elt == null) {
            return type;
        } else if (elt.getKind() == ElementKind.LOCAL_VARIABLE
            || elt.getKind() == ElementKind.PARAMETER) {
            type.include(Mutable.class);
            return type;
        }

        return type;
    }

    // complete this-mutable with inheritted mutability
    private void completeThisMutable(AnnotatedClassType type) {
        if (type.getElement() == null) return;
        if (type.getElement().asType().getKind() != TypeKind.ARRAY)
            for (Class<? extends Annotation> c :
                     new Class[]
                {Mutable.class, ReadOnly.class, RoMaybe.class, QReadOnly.class}){
                if (type.hasAnnotationAt(c, AnnotationLocation.RAW)) {
                    for (AnnotationLocation a : type.getAnnotatedLocations())
                        if (noAnnotationsAt(type, a))
                            type.includeAt(c, a);
                }
            }
    }

    private boolean noAnnotationsAt(AnnotatedClassType type, AnnotationLocation a) {
        return !type.hasAnnotationAt(ReadOnly.class, a)
            && !type.hasAnnotationAt(RoMaybe.class, a)
            && !type.hasAnnotationAt(Mutable.class, a)
            && !type.hasAnnotationAt(QReadOnly.class, a);
    }

    // overriding so that every RoMaybe parameter is also ReadOnly
    @Override
    protected List<AnnotatedClassType> paramTypes(ExecutableElement method) {

        List<AnnotatedClassType> paramTypes = super.paramTypes(method);
        for (AnnotatedClassType pType : paramTypes) {
            if (pType.hasAnnotationAt(RoMaybe.class, AnnotationLocation.RAW)) {
                pType.include(ReadOnly.class);
            }
        }
        return paramTypes;
    }


    // overriding all signatures...
    @Override
    protected List<AnnotatedClassType> paramTypes(AnnotatedClassType type, ExecutableElement method) {
        return paramTypes(method);

    }

    // build the most restrictive type
    protected void combine(AnnotatedClassType type1, AnnotatedClassType type2, AnnotatedClassType result) {
        Set<AnnotationLocation> loc = new LinkedHashSet<AnnotationLocation>();
        loc.addAll(type1.getAnnotatedLocations());
        loc.addAll(type2.getAnnotatedLocations());
        for (AnnotationLocation a : loc) {
            if (type1.hasAnnotationAt(QReadOnly.class, a)
                || type2.hasAnnotationAt(QReadOnly.class, a))
                result.includeAt(QReadOnly.class, a);

            else if (type1.hasAnnotationAt(ReadOnly.class, a)
                || type2.hasAnnotationAt(ReadOnly.class, a))
                result.includeAt(ReadOnly.class, a);

            else if (type1.hasAnnotationAt(RoMaybe.class, a)
                || type2.hasAnnotationAt(RoMaybe.class, a))
                result.includeAt(RoMaybe.class, a);

            else if (type1.hasAnnotationAt(Mutable.class, a)
                || type2.hasAnnotationAt(Mutable.class, a))
                result.includeAt(Mutable.class, a);
        }
    }

    // determines whether a RoMaybe return type is ReadOnly,RoMaybe, or Mutable
    private AnnotatedClassType resolveRoMaybe(MethodInvocationTree tree) {
        AnnotatedMethodType method = getMethod(tree);

        ExecutableElement methodElt = method.getElement();
        RoMaybe roMaybe
            = (RoMaybe) methodElt.getAnnotation(RoMaybe.class);

        List<AnnotatedClassType> parameterTypes =
            method.getAnnotatedParameterTypes();

        List<? extends ExpressionTree> argumentTrees = tree.getArguments();

        AnnotatedClassType receiverType = method.getAnnotatedReceiverType();

        boolean allMutable = true, allRoMaybe = true;

        for (int i = 0; i < parameterTypes.size(); i++) {
            AnnotatedClassType parameterType = parameterTypes.get(i);
            AnnotatedClassType argType = getClass(argumentTrees.get(i));
            for (AnnotationLocation loc : parameterType.getAnnotatedLocations()) {
                if (parameterType.hasAnnotationAt(RoMaybe.class, loc)) {
                    if (argType.hasAnnotationAt(ReadOnly.class, loc)
                        || argType.hasAnnotationAt(QReadOnly.class, loc)) {
                        allMutable = false;
                    }
                    if (!(argType.hasAnnotationAt(RoMaybe.class, loc)
                          && !argType.hasAnnotationAt(ReadOnly.class, loc)
                          && !argType.hasAnnotationAt(Mutable.class, loc)
                          && !argType.hasAnnotationAt(QReadOnly.class, loc))) {
                        allRoMaybe = false;
                    }
                }
            }
        }

        if (receiverType.hasAnnotationAt(RoMaybe.class, AnnotationLocation.RAW)) {
            if (tree.getMethodSelect() instanceof MemberSelectTree) {
                MemberSelectTree mst
                    = (MemberSelectTree) tree.getMethodSelect();
                AnnotatedClassType referenceType = getClass(mst.getExpression());
                if (referenceType.hasAnnotationAt(ReadOnly.class,
                                                  AnnotationLocation.RAW)) {
                    allMutable = false;
                }
            } else {
                AnnotatedClassType classType
                    = getClass(methodElt.getEnclosingElement());
                if (classType.hasAnnotationAt(ReadOnly.class,
                                              AnnotationLocation.RAW)) {
                    allMutable = false;
                }
            }
        }

        AnnotatedClassType type = super.getClass(tree);
        if (allMutable) {
            type.include(Mutable.class);
            type.exclude(ReadOnly.class);
        } else if (allRoMaybe) {
            // do nothing
        } else {
            type.include(ReadOnly.class);
            type.exclude(Mutable.class);
        }
        return type;

    }

    private AnnotatedClassType resolveRoMaybe(NewClassTree tree) {
        ExecutableElement classElt = InternalUtils.constructor(tree);
        AnnotatedMethodType constructor = getMethod(classElt);

        List<AnnotatedClassType> parameterTypes =
            constructor.getAnnotatedParameterTypes();

        List<? extends ExpressionTree> argumentTrees = tree.getArguments();

        boolean allMutable = true;

        for (int i = 0; i < parameterTypes.size(); i++) {
            AnnotatedClassType parameterType = parameterTypes.get(i);
            AnnotatedClassType argType = getClass(argumentTrees.get(i));
            for (AnnotationLocation loc : parameterType.getAnnotatedLocations()) {
                if (parameterType.hasAnnotationAt(RoMaybe.class, loc)
                    && (argType.hasAnnotationAt(ReadOnly.class, loc)
                        || argType.hasAnnotationAt(QReadOnly.class, loc))) {
                    allMutable = false;
                }
            }
        }

        AnnotatedClassType type = super.getClass(tree);
        if (allMutable) {
            type.include(Mutable.class);
            type.exclude(ReadOnly.class);
        } else {
            type.include(ReadOnly.class);
            type.exclude(Mutable.class);
        }
        return type;

    }

}
