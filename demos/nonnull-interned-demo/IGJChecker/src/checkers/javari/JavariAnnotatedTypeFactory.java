package checkers.javari;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import checkers.types.*;
import checkers.util.TreeUtils;
import checkers.util.AnnotationUtils;
import static checkers.types.AnnotatedTypeMirror.*;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.tree.JCTree;

import checkers.quals.*;

/**
 * Generates AnnotatedTypeMirrors with Javari annotations from Trees and Elements.
 */
public class JavariAnnotatedTypeFactory extends AnnotatedTypeFactory {

    /** Adds annotations from tree context before type resolution. */
    private final TreePreAnnotator treePre;

    /** Adds annotations from the resulting type after type resolution. */
    private final TypePostAnnotator typePost;

    /** The Javari annotations. */
    private final AnnotationMirror READONLY, THISMUTABLE, MUTABLE, ROMAYBE, QREADONLY, ASSIGNABLE;

    /**
     * Creates a new {@link JavariAnnotatedTypeFactory} that operates on a
     * particular AST.
     *
     * @param env the current processing environment for this annotation
     *                processor
     * @param root the AST on which this type factory operates
     */
    public JavariAnnotatedTypeFactory(ProcessingEnvironment env,
        CompilationUnitTree root) {
        super(env, root);
        this.treePre = new TreePreAnnotator();
        this.typePost = new TypePostAnnotator();
        this.READONLY = annotations.fromName(ReadOnly.class.getCanonicalName());
        this.THISMUTABLE = annotations.fromName(ThisMutable.class.getCanonicalName());
        this.MUTABLE = annotations.fromName(Mutable.class.getCanonicalName());
        this.ROMAYBE = annotations.fromName(RoMaybe.class.getCanonicalName());
        this.QREADONLY = annotations.fromName(QReadOnly.class.getCanonicalName());
        this.ASSIGNABLE = annotations.fromName(Assignable.class.getCanonicalName());
    }

    /**
     * Returns the annotation specifying the immutability type of {@code type}.
     */
    private AnnotationMirror getImmutabilityAnnotation(/*@ReadOnly*/ AnnotatedTypeMirror type) {
        if (type.hasAnnotation(READONLY))
            return READONLY;
        else if (type.hasAnnotation(MUTABLE))
            return MUTABLE;
        else if (type.hasAnnotation(ROMAYBE))
            return ROMAYBE;
        else if (type.hasAnnotation(QREADONLY))
            return QREADONLY;
        else if (type.hasAnnotation(THISMUTABLE))
            return THISMUTABLE;
        else
            return null;
    }

    /**
     * @param type  an annotated type mirror
     * @return  true iff the type is specified an immutability type other than this-mutable,
     *          false otherwise
     */
    public boolean hasImmutabilityAnnotation(/*@ReadOnly*/ AnnotatedTypeMirror type) {
        return getImmutabilityAnnotation(type) != null && !type.hasAnnotation(THISMUTABLE);
    }


    @Override
    protected void annotateImplicit(Tree tree, /*@Mutable*/ AnnotatedTypeMirror type) {
        treePre.visit(tree, type);
    }

    @Override
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type) {
        typePost.visit(type);
    }


    /**
     * Returns a type of the class and the field {@code this}.
     */
    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType act = this.getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver = this.getCurrentMethodReceiver(tree);

        if (methodReceiver == null || !hasImmutabilityAnnotation(methodReceiver))
            return act;

        return methodReceiver;
    }


    /**
     * Returns a singleton collection with the most restrictive immutability
     * annotation that is a supertype of the annotations on both collections.
     */
    @Override
    public Collection<AnnotationMirror> unify(Collection<AnnotationMirror> c1,
            Collection<AnnotationMirror> c2) {
        Map<String, AnnotationMirror> ann =
            new HashMap<String, AnnotationMirror>();
        for (AnnotationMirror anno : c1)
            ann.put(AnnotationUtils.annotationName(anno), anno);
        for (AnnotationMirror anno : c2)
            ann.put(AnnotationUtils.annotationName(anno), anno);

        if (ann.containsKey(QReadOnly.class.getCanonicalName()))
            return Collections.singleton(QREADONLY);
        else if (ann.containsKey(ReadOnly.class.getCanonicalName()))
            return Collections.singleton(READONLY);
        else if (ann.containsKey(RoMaybe.class.getCanonicalName()))
            return Collections.singleton(ROMAYBE);
        else
            return Collections.singleton(MUTABLE);
    }



    @Override
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        AnnotatedExecutableType type = super.methodFromUse(tree);

        ExecutableElement executableElt = type.getElement();
        AnnotatedTypeMirror returnType = type.getReturnType();

        if (returnType.hasAnnotation(ROMAYBE)) {

            List<AnnotatedTypeMirror> parameterTypes = type.getParameterTypes();
            List<? extends ExpressionTree> argumentTrees = tree.getArguments();
            AnnotatedTypeMirror receiverType = type.getReceiverType();

            boolean allMutable = true, allRoMaybe = true;

            // look at parameters and arguments
            // TODO: deal with varargs parameters
            for (int i = 0; i < parameterTypes.size(); i++) {
                AnnotatedTypeMirror pType = parameterTypes.get(i);
                // only look at it if parameter is RoMaybe
                if (pType.hasAnnotation(ROMAYBE)) {
                    AnnotatedTypeMirror aType = fromExpression(argumentTrees.get(i));

                    // if argument has no Mutable and has annotations, not all are Mutable
                    if (!aType.hasAnnotation(MUTABLE) && !aType.getAnnotations().isEmpty())
                        allMutable = false;

                    // if argument has no RoMaybe, not all args are RoMaybe
                    if (!aType.hasAnnotation(ROMAYBE))
                        allRoMaybe = false;

                }
            }

            // look at receiver type and reference
            if (receiverType.hasAnnotation(ROMAYBE)) {
                // if MemberSelectTree, we can just look at the expression tree
                ExpressionTree exprTree = tree.getMethodSelect();
                if (exprTree.getKind() == Tree.Kind.MEMBER_SELECT) {
                    MemberSelectTree mst = (MemberSelectTree) exprTree;
                    AnnotatedTypeMirror referenceType = getAnnotatedType(mst.getExpression());

                    if (referenceType.hasAnnotation(READONLY)) {
                        allMutable = false;
                        allRoMaybe = false;
                    } else if (referenceType.hasAnnotation(ROMAYBE)) {
                        allMutable = false;
                    }

                } else {
                    // not MemberSelectTree, get context from method's enclosing class
                    AnnotatedTypeMirror classType
                        = fromElement(executableElt.getEnclosingElement());
                    if (classType.hasAnnotation(READONLY)) {
                        allMutable = false;
                        allRoMaybe = false;
                    } else if (classType.hasAnnotation(ROMAYBE)) {
                        allMutable = false;
                    }
                }
            }

            // now, add appropriate type to return list
            if (allMutable) returnType.addAnnotation(MUTABLE);
            else if (allRoMaybe) returnType.addAnnotation(ROMAYBE);
            else returnType.addAnnotation(READONLY);

        } else { // return type has no RoMaybe, just get the annotated return type
            if (returnType.hasAnnotation(READONLY))returnType.addAnnotation(READONLY);
            else if (!returnType.getKind().isPrimitive()) returnType.addAnnotation(MUTABLE);
            else returnType.addAnnotation(THISMUTABLE);
        }

        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> returnTypeMap
            = Collections.singletonMap(type.getReturnType(), returnType);
        type = type.substitute(returnTypeMap);

        return type;
    }


    /**
     * A visitor to get the annotations from a tree.
     */
    private class TreePreAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

        /** Default action: do nothing. */
        @Override
        public Void defaultAction(Tree node, AnnotatedTypeMirror p) {
            return null;
        }


        /** Ensures every literal type has a {code @Mutable} annotation. */
        @Override
        public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror p) {
            p.addAnnotation(MUTABLE);
            return super.visitLiteral(node, p);
        }


        /** Ensures the result has a {code @Mutable} annotation. */
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror p) {
            p.addAnnotation(MUTABLE);
            return super.visitBinary(node, p);
        }


        /** Ensures the result has a {code @Mutable} annotation. */
        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror p) {
            p.addAnnotation(MUTABLE);
            return super.visitUnary(node, p);
        }

        /** Ensures the result has the internal this-mutable annotation, if nothing else */
        @Override
        public Void visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeMirror p) {
            if (!hasImmutabilityAnnotation(p))
                p.addAnnotation(THISMUTABLE);
            return super.visitPrimitiveType(node, p);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror p) {
            if (!hasImmutabilityAnnotation(p)) {
                AnnotatedDeclaredType x = getSelfType(node);
                if (x.hasAnnotation(READONLY)) p.addAnnotation(READONLY);
                else if (x.hasAnnotation(MUTABLE)) p.addAnnotation(MUTABLE);
                else if (x.hasAnnotation(ROMAYBE)) p.addAnnotation(ROMAYBE);
                else p.addAnnotation(MUTABLE);
            }
            return super.visitIdentifier(node, p);
        }


        /** Returns list of annotations on the new class constructor; resolves {code @RoMaybe} if applicable. */
        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {

            Element constructorElt = ((JCTree.JCNewClass)node).constructor;
            AnnotatedExecutableType executableType =
                (AnnotatedExecutableType) fromElement(constructorElt);
            AnnotatedTypeMirror receiverType = executableType.getReceiverType();

            // if receiver type has RoMaybe, we must resolve it by looking at the arguments
            if (receiverType.hasAnnotation(ROMAYBE)) {
                List<AnnotatedTypeMirror> parameterTypes = executableType.getParameterTypes();
                List<? extends ExpressionTree> argumentTrees = node.getArguments();

                boolean allMutable = true, allRoMaybe = true;

                for (int i = 0; i < parameterTypes.size(); i++) {
                    AnnotatedTypeMirror pType = parameterTypes.get(i);
                    if (pType.hasAnnotation(ROMAYBE)) {
                        AnnotatedTypeMirror aType = fromExpression(argumentTrees.get(i));

                        if (aType.hasAnnotation(READONLY) || aType.hasAnnotation(QREADONLY))
                            allMutable = false;

                        if (!(aType.hasAnnotation(ROMAYBE)
                              && !aType.hasAnnotation(READONLY)
                              && !aType.hasAnnotation(MUTABLE)
                              && !aType.hasAnnotation(QREADONLY)))
                            allRoMaybe = false;
                    }
                }

                if (allMutable) {
                    if (!p.hasAnnotation(MUTABLE)) p.addAnnotation(MUTABLE);
                } else if (allRoMaybe) {
                    if (!p.hasAnnotation(ROMAYBE)) p.addAnnotation(ROMAYBE);
                } else {
                    if (!p.hasAnnotation(READONLY)) p.addAnnotation(READONLY);
                }

            } else {   // if receiver has no romaybe, just return the receiver type
                if (receiverType.hasAnnotation(READONLY)) p.addAnnotation(READONLY);
                if (receiverType.hasAnnotation(MUTABLE)) p.addAnnotation(MUTABLE);
            }

            return super.visitNewClass(node, p);
        }

        /**
         * Selects the appropriate annotation from the inner type, or inherits
         * it from the outer type according to the Javari typesystem.
         */
        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror p) {

            AnnotatedTypeMirror outerType = fromExpression(node.getExpression());
            AnnotatedTypeMirror innerType = fromElement(elementFromUse(node));

            if (innerType.hasAnnotation(READONLY))
                p.addAnnotation(READONLY);
            else if (innerType.hasAnnotation(MUTABLE))
                p.addAnnotation(MUTABLE);
            else if (innerType.hasAnnotation(ROMAYBE) && outerType.hasAnnotation(ROMAYBE))
                p.addAnnotation(ROMAYBE);
            else if (outerType.hasAnnotation(READONLY))
                p.addAnnotation(READONLY);
            else if (outerType.hasAnnotation(MUTABLE))
                p.addAnnotation(MUTABLE);
            else if (outerType.hasAnnotation(ROMAYBE))
                p.addAnnotation(ROMAYBE);

            return super.visitMemberSelect(node, p);
        }

    }


    /**
     * Responsible for adding implicit (not written on the code) annotations.
     */
    private class TypePostAnnotator extends AnnotatedTypeScanner<Void, Void> {

        /**
         * Passes down the enclosing class annotation, if any; else try
         * to retrieve from annotation if available, from enclosing
         * class if field, from supertype if class or interface,
         * or mutable otherwise.
         */
        @Override
        public Void scan(AnnotatedTypeMirror type, Void p) {
            // we don't want to add annotations if there are annotations already
            if (!hasImmutabilityAnnotation(type) && type.getElement() != null) {
                ElementKind eltKind = type.getElement().getKind();

                if (eltKind == ElementKind.LOCAL_VARIABLE) {
                    type.addAnnotation(MUTABLE);

                } else if (eltKind.isField()) {
                    // inherit annotations from enclosing class
                    AnnotatedTypeMirror enclosingType =
                        fromElement(type.getElement().getEnclosingElement());

                    if (enclosingType.hasAnnotation(READONLY))
                        type.addAnnotation(READONLY);

                } else if (eltKind.isClass()) {

                    AnnotatedTypeMirror superType
                        = ((AnnotatedDeclaredType) type).getSuperclass();

                    // if Object, supertype has underlying type NoType with
                    // TypeKind NONE, and no annotations

                    if (superType.hasAnnotation(MUTABLE))
                        type.addAnnotation(MUTABLE);
                    else if (superType.hasAnnotation(READONLY))
                        type.addAnnotation(READONLY);
                    else if (superType.hasAnnotation(ROMAYBE))
                        type.addAnnotation(ROMAYBE);
                    else
                        type.addAnnotation(MUTABLE);

                } else if (eltKind.isInterface()) {

                    AnnotatedDeclaredType iType = (AnnotatedDeclaredType) type;
                    boolean annotated = false;
                    for (AnnotatedTypeMirror i : iType.getInterfaces()) {
                        if (i.hasAnnotation(READONLY)) {
                            type.addAnnotation(READONLY);
                            break;
                        }
                    }
                    if (!annotated) {
                        for (AnnotatedTypeMirror i : iType.getInterfaces()) {
                            if (i.hasAnnotation(ROMAYBE)) {
                                type.addAnnotation(ROMAYBE);
                                annotated = true;
                                break;
                            }
                        }
                    }

                }

            }
            return super.scan(type, p);
        }

        private void scanReceiver(AnnotatedDeclaredType rType) {
            if (!hasImmutabilityAnnotation(rType)) {
                AnnotatedTypeMirror act = visitorState.getClassType();
                if (act.hasAnnotation(READONLY)) rType.addAnnotation(READONLY);
                else if (act.hasAnnotation(MUTABLE)) rType.addAnnotation(MUTABLE);
                else if (act.hasAnnotation(ROMAYBE)) rType.addAnnotation(ROMAYBE);
            }
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Void p) {
            Void v = scan(type.getReturnType(), p);
            scanReceiver(type.getReceiverType());
            v = scanAndReduce(type.getParameterTypes(), p, v);
            v = scanAndReduce(type.getThrownTypes(), p, v);
            v = scanAndReduce(type.getTypeVariables(), p, v);
            return v;
        }

    }
}
