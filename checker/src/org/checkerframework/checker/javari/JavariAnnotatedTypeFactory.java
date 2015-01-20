package org.checkerframework.checker.javari;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.checker.javari.qual.Assignable;
import org.checkerframework.checker.javari.qual.Mutable;
import org.checkerframework.checker.javari.qual.PolyRead;
import org.checkerframework.checker.javari.qual.QReadOnly;
import org.checkerframework.checker.javari.qual.ReadOnly;
import org.checkerframework.checker.javari.qual.ThisMutable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * Adds implicit and default Javari annotations, only if the user does not
 * annotate the type explicitly.  The default annotations are designed to
 * minimize the need to write {@code ReadOnly} in the source code.
 * <p>
 *
 * <b>Implicit Annotations:</b>
 * All literals are implicitly treated as {@code Mutable}, including the
 * null literal.  While they are indeed immutable, this implicit type helps
 * interfacing with non-annotated libraries.
 * <p>
 *
 * <b>Default Annotations:</b>
 *
 * <ul>
 * <li>
 * This factory will add the {@link ReadOnly} annotation to a type if the
 * tree or element is
 * <ol>
 * <li value="1">a use of a known ReadOnly class (i.e. class whose declaration
 *  is annotated with {@code ReadOnly}, including a method receiver of a
 *  ReadOnly class, or
 * <li value="2">the upper bound type of a type parameter declaration, or a
 *  wildcard appearing on a class or method declaration
 * <li value="3">an access of a {@link ThisMutable} field using a ReadOnly
 * reference (e.g. {@code readOnlyRef.thisMutableField} with the obvious
 * declarations)
 * </ol>
 *
 * <li>
 * This factory will add the {@link ThisMutable} annotation to a type if the
 * input is a field of a mutable class.
 *
 * <li>
 * In all other cases, the {@link Mutable} annotation is inserted by default.
 * </ul>
 */
public class JavariAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Adds annotations from tree context before type resolution. */
    private final JavariTreePreAnnotator treePre;

    /** Adds annotations from the resulting type after type resolution. */
    private final JavariTypePostAnnotator typePost;

    /** The Javari annotations. */
    protected final AnnotationMirror READONLY, THISMUTABLE, MUTABLE, POLYREAD, QREADONLY, ASSIGNABLE;

    /**
     * Creates a new {@link JavariAnnotatedTypeFactory} that operates on a
     * particular AST.
     *
     * @param checker the checker to which this factory belongs
     */
    public JavariAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        this.READONLY = AnnotationUtils.fromClass(elements, ReadOnly.class);
        this.THISMUTABLE = AnnotationUtils.fromClass(elements, ThisMutable.class);
        this.MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        this.POLYREAD = AnnotationUtils.fromClass(elements, PolyRead.class);
        this.QREADONLY = AnnotationUtils.fromClass(elements, QReadOnly.class);
        this.ASSIGNABLE = AnnotationUtils.fromClass(elements, Assignable.class);

        this.treePre = new JavariTreePreAnnotator();
        this.typePost = new JavariTypePostAnnotator();

        postInit();
    }

    /**
     * Returns the annotation specifying the immutability type of {@code type}.
     */
    private AnnotationMirror getImmutabilityAnnotation(/*@ReadOnly*/ AnnotatedTypeMirror type) {
       return type.getAnnotationInHierarchy(READONLY);
    }

    /**
     * @param type  an annotated type mirror
     * @return true iff the type is specified an immutability type
     * other than this-mutable, false otherwise
     */
    public boolean hasImmutabilityAnnotation(/*@ReadOnly*/ AnnotatedTypeMirror type) {
        return type != null && getImmutabilityAnnotation(type) != null;
    }


    /**
     * Adds implicit annotations to a qualified type, based on its
     * tree, as follows:
     *
     * <ul>
     *
     *   <li> 1. Resolves qualified types from MemberSelectTree,
     *   inheriting from the expression to the identifier if the
     *   identifier is {@code @ThisMutable}.
     *
     *   <li> 2. Qualified class types without annotations receive the
     *   {@code @Mutable} annotation.
     *
     *   <li> 3. Qualified executable types receivers without
     *   annotations are annotated with the qualified executable type
     *   owner's annotation.
     *
     *   <li> 4. Qualified executable types parameters and return
     *   values without annotations are annotated with {@code @Mutable}.
     *
     *   <li> 5. Qualified declared types are annotated with their
     *   underlying type's element annotations.
     *
     *   <li> 6. Qualified types whose elements correspond to fields,
     *   and all its subtypes, are annotated with {@code @ReadOnly},
     *   {@code @Mutable} or {@code @PolyRead}, according to the
     *   qualified type of {@code this}.
     *
     * </ul>
     *
     * @param tree an AST node
     * @param type the type obtained from {@code tree}
     */
    @Override
    public void annotateImplicit(Tree tree, /*@Mutable*/ AnnotatedTypeMirror type, boolean useFlow) {

        // primitives are all the same
        if (type.getKind().isPrimitive()
            && !hasImmutabilityAnnotation(type)) {
            type.addAnnotation(MUTABLE);
            return;
        }

        // 1 and 2
        treePre.visit(tree, type);

        // 3, 4 and 5
        typePost.visit(type, null);

        // 6 - resolve ThisMutable from fields
/*        if (elt != null && elt.getKind() == ElementKind.FIELD &&
                 type.hasEffectiveAnnotation(THISMUTABLE)) {
            AnnotatedDeclaredType selfType = getSelfType(tree);

            if (selfType != null) {
                if (selfType.hasEffectiveAnnotation(POLYREAD)) {
                    new AnnotatedTypeReplacer(THISMUTABLE, POLYREAD).visit(type);
                } else if (selfType.hasEffectiveAnnotation(MUTABLE)) {
                    new AnnotatedTypeReplacer(THISMUTABLE, MUTABLE).visit(type);
                } else if (selfType.hasEffectiveAnnotation(READONLY)) {
                    new AnnotatedTypeReplacer(THISMUTABLE, READONLY).visit(type);
                }
            }
        }*/
        defaults.annotate(tree, type);
    }

    /**
     * Adds annotations to qualified types according to their provided
     * element, as follows:
     *
     * <ul>
     *
     *   <li> 1. Qualified class types without annotations
     *   corresponding to class or interface elements receive the
     *   {@code @Mutable} annotation.
     *
     *   <li> 2. Unannotated receivers of qualified executable types
     *   are annotated with the qualified type owner's annotation.
     *
     *   <li> 3. Unannotated qualified declared types are annotated
     *   with their underlying type's element annotations.
     *
     *   <li> 4. Qualified types whose elements correspond to fields,
     *   and all its subtypes, are annotated with {@code @ReadOnly} or
     *   {@code @ThisMutable}, according to the supertype.
     *
     * </ul>
     *
     * @param element an element
     * @param type the type obtained from {@code elt}
     */
    @Override
    public void annotateImplicit(Element element, /*@Mutable*/ AnnotatedTypeMirror type) {
        if (element.getKind().isClass() || element.getKind().isInterface()) {
            if (!hasImmutabilityAnnotation(type))
                type.addAnnotation(MUTABLE);
        }
        typePost.visit(type, null);
        defaults.annotate(element, type);
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
//TODO: SINCE THIS DOESN'T SEEM LIKE IT DOES ANYTHING USEFUL I NOW CALL THE SUPERTYPE IMPLEMENTATION
//        AnnotatedDeclaredType act = getCurrentClassType(tree);
//        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
//
//        if (methodReceiver == null) {
//            methodReceiver = act;
//        }/* else if (methodReceiver.hasAnnotation(MUTABLE)) {
//            methodReceiver.replaceAnnotation(THISMUTABLE);
//        }*/
//
//
//        return methodReceiver;
        return super.getSelfType(tree);
    }

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        for (AnnotatedTypeMirror supertype : supertypes) {
            typePost.visit(supertype, null);
        }
    }

    /**
     * Determines the type of the constructed object based on the
     * parameters passed to the constructor. The new object has the
     * same mutability as the annotation marked on the constructor
     * receiver, as resolved by this method.
     *
     * {@code @PolyRead} receiver values are resolved by looking at
     * the mutability of any parameters marked as {@code @PolyRead}. The rules
     * are similar to the ones applicable to
     * method invocation resolution, but without looking at {@code this}.
     *
     * <ul>
     *
     *  <li> 1. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} arguments, the receiver value is resolved as
     *  {@code @Mutable}.
     *
     *  <li> 2. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} or {@code @ThisMutable} arguments and the
     *  condition above is not satisfied, the receiver value is
     *  resolved as {@code @ThisMutable}.
     *
     *  <li> 3. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} or {@code @PolyRead} arguments and none of
     *  the condition above is satisfied, the receiver value is
     *  resolved as {@code @PolyRead}.
     *
     *  <li> 4. If none of the conditions above is satisfied, the
     *  receiver value is resolved as {@code @ReadOnly}.
     *
     * </ul>
     *
     * @param tree  the new class tree
     * @return AnnotatedExecutableType corresponding to the type being
     * constructed, with the resolved type on its receiver.
     */
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse = super.constructorFromUse(tree);
        AnnotatedExecutableType exType = fromUse.first;
        List<AnnotatedTypeMirror> typeargs = fromUse.second;

        List<AnnotatedTypeMirror> parameterTypes = AnnotatedTypes.expandVarArgs(this, exType, tree.getArguments());
        List<AnnotatedTypeMirror> argumentTypes = AnnotatedTypes.getAnnotatedTypes(this, parameterTypes, tree.getArguments());

        boolean allMutable = true, allPolyRead = true, allThisMutable = true;

        // look at parameters and arguments
        for (int i = 0; i < parameterTypes.size(); i++) {
            AnnotatedTypeMirror pType = parameterTypes.get(i);

            if (pType.hasEffectiveAnnotation(POLYREAD)) {
                AnnotatedTypeMirror aType = argumentTypes.get(i);

                if (aType.hasEffectiveAnnotation(THISMUTABLE) || aType.hasEffectiveAnnotation(POLYREAD))
                    allMutable = false;

                if (aType.hasEffectiveAnnotation(READONLY) || aType.hasEffectiveAnnotation(QREADONLY)) {
                    allMutable = false; allThisMutable = false;
                }

                if (!(aType.hasEffectiveAnnotation(POLYREAD)
                      && !aType.hasEffectiveAnnotation(READONLY)
                      && !aType.hasEffectiveAnnotation(THISMUTABLE)
                      && !aType.hasEffectiveAnnotation(QREADONLY)))
                    allPolyRead = false;
            }
        }

        // replacement: annotation to be put in place of @PolyRead
        AnnotationMirror replacement;
        if (allMutable) replacement = MUTABLE;               // case 1
        else if (allThisMutable) replacement = THISMUTABLE;  // case 2
        else if (allPolyRead) replacement = POLYREAD;        // case 3
        else replacement = READONLY;                         // case 4

        if (replacement != POLYREAD)  // do not replace if replacement is also @PolyRead
            new AnnotatedTypeReplacer(POLYREAD, replacement).visit(exType);

        return Pair.of(exType, typeargs);
    }

    /**
     * Determines the type of the invoked method based on the passed method
     * invocation tree.
     *
     * Invokes the super method, then resolves annotations {@code @PolyRead}
     * at the raw level on return values by looking at the
     * mutability of any parameters marked as {@code @PolyRead}. For
     * this purpose, a {@code @PolyRead} annotation on the receiver
     * counts as if {@code this} were being passed as an argument to a
     * parameter marked as {@code @PolyRead}.
     *
     * <ul>
     *
     *  <li> 1. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} arguments, the return value is resolved as
     *  {@code @Mutable}.
     *
     *  <li> 2. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} or {@code @ThisMutable} arguments and the
     *  condition above is not satisfied, the return value is resolved
     *  as {@code @ThisMutable}.
     *
     *  <li> 3. If all parameters marked as {@code @PolyRead} receive
     *  {@code @Mutable} or {@code @PolyRead} arguments and none of
     *  the condition above is satisfied, the return value is resolved
     *  as {@code @PolyRead}.
     *
     *  <li> 4. If none of the conditions above is satisfied, the
     *  return value is resolved as {@code @ReadOnly}.
     *
     * </ul>
     *
     * @param tree  the method invocation tree
     * @return AnnotatedExecutableType with return value resolved as described.
     */
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
        AnnotatedExecutableType type = mfuPair.first;

        AnnotatedTypeMirror returnType = type.getReturnType();

        List<AnnotatedTypeMirror> parameterTypes = AnnotatedTypes.expandVarArgs(this, type, tree.getArguments());
        List<AnnotatedTypeMirror> argumentTypes = AnnotatedTypes.getAnnotatedTypes(this, parameterTypes, tree.getArguments());

        boolean allMutable = true, allPolyRead = true, allThisMutable = true;

        // look at parameters and arguments
        // TODO: get method properly
        for (int i = 0; i < parameterTypes.size(); i++) {
            AnnotatedTypeMirror pType = parameterTypes.get(i);

            // look at it if parameter is PolyRead
            if (pType.hasEffectiveAnnotation(POLYREAD)) {
                AnnotatedTypeMirror aType = argumentTypes.get(i);

                if (aType.hasEffectiveAnnotation(THISMUTABLE) || aType.hasEffectiveAnnotation(POLYREAD))
                    allMutable = false;

                if (aType.hasEffectiveAnnotation(READONLY) || aType.hasEffectiveAnnotation(QREADONLY)) {
                    allMutable = false; allThisMutable = false;
                }

                if (!(aType.hasEffectiveAnnotation(POLYREAD)
                      && !aType.hasEffectiveAnnotation(READONLY)
                      && !aType.hasEffectiveAnnotation(THISMUTABLE)
                      && !aType.hasEffectiveAnnotation(QREADONLY)))
                    allPolyRead = false;
            }
        }

        AnnotatedTypeMirror receiverType = type.getReceiverType();

        // look at receiver type and reference
        if (receiverType != null &&
                receiverType.hasEffectiveAnnotation(POLYREAD)) {
            // if MemberSelectTree, we can just look at the expression tree
            ExpressionTree exprTree = tree.getMethodSelect();
            AnnotatedTypeMirror exprReceiver = this.getReceiverType(exprTree);
            if (exprReceiver.hasEffectiveAnnotation(READONLY)) {
                allMutable = false;
                allThisMutable = false;
                allPolyRead = false;
            } else if (exprReceiver.hasEffectiveAnnotation(POLYREAD)) {
                allMutable = false;
            }
        }

        // replacement: annotation to be put in place of @PolyRead
        AnnotationMirror replacement;
        if (allMutable) replacement = MUTABLE;               // case 1
        else if (allThisMutable) replacement = THISMUTABLE;  // case 2
        else if (allPolyRead) replacement = POLYREAD;        // case 3
        else replacement = READONLY;                         // case 4

        if (replacement != POLYREAD && returnType.hasEffectiveAnnotation(POLYREAD))
            new AnnotatedTypeReplacer(POLYREAD, replacement).visit(type);

        return mfuPair;
    }

    /**
     * We modify this callback method to replace {@code @ThisMutable}
     * implicit annotations with the qualified supertype annotation,
     * if the owner doesn't have a {@code @ReadOnly} annotation.
     * <p>
     * Note on the given example that, if {@code @ThisMutable tmObject}
     * were resolved as {@code @ReadOnly tmObject}, the code snippet
     * would be legal. Such a class could then be created to obtain
     * {@code @Mutable} access to {@code tmObject} from a {@code @ReadOnly}
     * reference to it, without type-checker errors.
     *
     * <pre>@PolyRead Object breakJavari(@ReadOnly MyClass this, @PolyRead Object s) {
     *   tmObject = s;
     *   return null;
     *  }
     * </pre>
     *
     * @param type  the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element   the element of the field or method
     */
    @Override
    public void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
        final AnnotationMirror ownerAnno = getImmutabilityAnnotation(owner);
        if (ownerAnno != THISMUTABLE) {
            new AnnotatedTypeReplacer(THISMUTABLE, ownerAnno).visit(type);
        }
    }

    /**
     * A visitor to get annotations from a tree.
     *
     * Annotations obtained from a MemberSelectTree are added to the parameter p.
     */
    private class JavariTreePreAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

        /**
         * Selects the appropriate annotation from the identifier, or
         * inherits it from the expression, as follows:
         *
         * <ul>
         *
         *  <li> 1. If the identifier qualified type is annotated with
         *  {@code @ReadOnly} or {@code @Mutable}, the parameter
         *  receives the same annotation.
         *
         *  <li> 2. If not, and if the expression qualified type has
         *  any annotation, the parameter receives those annotations.
         *
         *  <li> 3. If the expression qualified type has no
         *  annotation, then the parameter receives a {@code
         *  @ThisMutable} annotation.
         *
         * </ul>
         *
         * @param node MemberSelectTree to be analyzed, of the form {@code expression . identifier}
         * @param p AnnotatedTypeMirror parameter to be annotated
         */
        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror p) {
            AnnotatedTypeMirror exType = getAnnotatedType(node.getExpression());
            AnnotatedTypeMirror idType = fromElement(TreeUtils.elementFromUse(node));

            /*if (// idType.getKind() != TypeKind.TYPEVAR &&
                    // !hasImmutabilityAnnotation(p) &&
                    idType.hasEffectiveAnnotation(READONLY))                     // case 1
                p.replaceAnnotation(READONLY);
            else*/ if (//!hasImmutabilityAnnotation(p) &&
                    idType.hasEffectiveAnnotation(MUTABLE))                 // case 1
                p.replaceAnnotation(MUTABLE);
            else if (hasImmutabilityAnnotation(exType))             // case 2
                p.replaceAnnotation(getImmutabilityAnnotation(exType));
            else                                                    // case 3
                p.replaceAnnotation(THISMUTABLE);

            if (p.getKind().isPrimitive()) {
                // READONLY is invalid for primitives. Fix the type.
                // I think it would be cleaner to treat readonly primitives as
                // final.
                p.replaceAnnotation(MUTABLE);
            }

            return super.visitMemberSelect(node, p);
        }

        /**
         * Inserts {@code @Mutable} annotations on null literal trees.
         */
        @Override
        public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror p) {
            if (node.getKind() == Tree.Kind.NULL_LITERAL)
                p.addAnnotation(MUTABLE);
            return super.visitLiteral(node, p);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            assert p.getKind() == TypeKind.DECLARED;
            if (!hasImmutabilityAnnotation(p)) {
                p.addAnnotation(MUTABLE);
            }

            return super.visitNewClass(node, p);
        }

        @Override
        public Void visitClass(ClassTree node, AnnotatedTypeMirror p) {
            if (!hasImmutabilityAnnotation(p))
                p.addAnnotation(MUTABLE);
            return super.visitClass(node, p);
        }
    }

    /**
     * Scanner responsible for adding implicit annotations to qualified types.
     *
     * <ul>
     *
     *   <li> 1. Annotates unannotated receivers of qualified
     *   executable types with the qualified type owner's annotation;
     *   annotated its parameters with {@code @Mutable}, if they have
     *   no annotation, and annotates its return type with {@code
     *   @Mutable}, if it has no annotation.
     *
     *   <li> 2. Annotates unannotated qualified declared types with
     *   their underlying type's element annotations.
     *
     *   <li> 3. Annotates unnanotated qualified types whose elements
     *   correspond to fields with {@code @ThisMutable}.
     *
     *   <li> 4. Annotated unnanotated qualified types corresponding to arrays
     *   with {@code @Mutable}.
     *
     * </ul>
     */
    private class JavariTypePostAnnotator extends AnnotatedTypeScanner<Void, Void> {

        /**
         * If the receiver has no annotation, annotates it with the
         * same annotation as the executable type owner. If the
         * parameters or the return type have no annotations, annotate
         * it with {@code @Mutable}.
         */
        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Void p) {
            AnnotatedDeclaredType receiver = type.getReceiverType();
            if (receiver != null &&
                    !hasImmutabilityAnnotation(receiver)) {                // case 1
                AnnotatedDeclaredType owner = (AnnotatedDeclaredType)
                    getAnnotatedType(type.getElement().getEnclosingElement());
                assert hasImmutabilityAnnotation(owner);
                receiver.addAnnotation(getImmutabilityAnnotation(owner));
            }
/*
            AnnotatedTypeMirror returnType = type.getReturnType();
            if (!hasImmutabilityAnnotation(returnType)
                // WMD doesn't understand why primitives are excluded
                && !returnType.getKind().isPrimitive()
                && returnType.getKind() != TypeKind.VOID
                && returnType.getKind() != TypeKind.TYPEVAR)
                returnType.addAnnotation(MUTABLE);*/

            return super.visitExecutable(type, p);
        }

        /**
         * If the declared type has no annotation, annotates it with
         * the same annotation as its underlying type's element.
         */
        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
            if (!hasImmutabilityAnnotation(type)) {                  // case 2
                TypeElement tElt = (TypeElement) type.getUnderlyingType().asElement();
                AnnotatedTypeMirror tType = fromElement(tElt);
                if (hasImmutabilityAnnotation(tType)) {
                    type.addAnnotation(getImmutabilityAnnotation(tType));
                } /*else
                    type.addAnnotation(MUTABLE);*/
            }
            /*
            if (elem != null && elem.getKind().isField()) {
                // TODO: Javari wants different defaulting rules for type arguments
                // of field types: instead of THISMUTABLE use MUTABLE.
                // What is a nicer way to do this?
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }
                visitedNodes.put(type, null);
                Void r = scan(type.getTypeArguments(), null);
                return r;
            } else {*/
                return super.visitDeclared(type, p);
            //}
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            if (!hasImmutabilityAnnotation(type)) {
                type.addAnnotation(MUTABLE);
            }
            return super.visitPrimitive(type, p);
        }

        /**
         * Ensures that AnnotatedArrayTypes are annotated with {@code
         * @Mutable}, if they have no annotation yet.
         */
        @Override
        public Void visitArray(AnnotatedArrayType type, Void p) {
            if (!hasImmutabilityAnnotation(type)) {                  // case 4
                type.addAnnotation(MUTABLE);
            }
            return super.visitArray(type, p);
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            /*
            System.out.println("TypePost.visitTV: " + type);
            // In a declaration the upperbound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getUpperBoundField() != null &&
                    !hasImmutabilityAnnotation(type.getUpperBound())) {
                System.out.println("Here? elem: " + elem);
                ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;
                if (elemKind.isClass() || elemKind.isInterface()
                        || elemKind == ElementKind.CONSTRUCTOR
                        || elemKind == ElementKind.METHOD
                        || elemKind == ElementKind.PARAMETER
                        || elemKind == ElementKind.TYPE_PARAMETER)
                    // case 5: upper bound within a class/method declaration
                    type.getUpperBound().addAnnotation(READONLY);
                else if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getUpperBound().addAnnotation(MUTABLE);
            }

            return super.visitTypeVariable(type, elem);*/
            return null;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, Void p) {
            // In a declaration the upper bound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getExtendsBound() != null
                    && !hasImmutabilityAnnotation(type.getExtendsBound())) {
                /*ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;
                if (elemKind.isClass() || elemKind.isInterface()
                        || elemKind == ElementKind.CONSTRUCTOR
                        || elemKind == ElementKind.METHOD)
                    // case 5: upper bound within a class/method declaration
                    type.getExtendsBound().addAnnotation(READONLY);
                else*/ if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getExtendsBound().addAnnotation(MUTABLE);
            }

            return super.visitWildcard(type, p);
        }
    }


    /**
     * Type scanner to replace annotations on annotated types.
     */
    private class AnnotatedTypeReplacer extends SimpleAnnotatedTypeScanner<Void, Void> {
        private final AnnotationMirror oldAnnotation, newAnnotation;

        /** Initializes the class to replace oldAnnotation with newAnnotation. */
        AnnotatedTypeReplacer(AnnotationMirror oldAnnotation, AnnotationMirror newAnnotation) {
            this.oldAnnotation = oldAnnotation;
            this.newAnnotation = newAnnotation;
        }

        @Override
        protected Void defaultAction(AnnotatedTypeMirror type, Void p) {
            if (type.hasEffectiveAnnotation(oldAnnotation)) {
                type.removeAnnotation(oldAnnotation);
                type.addAnnotation(newAnnotation);
            }
            return super.defaultAction(type, p);
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new JavariQualifierHierarchy(factory);
    }

    private final class JavariQualifierHierarchy extends GraphQualifierHierarchy {

        public JavariQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory, MUTABLE);
        }

        /**
         * Returns a singleton collection with the most restrictive immutability
         * annotation that is a supertype of the annotations on both collections.
         */
        @Override
        public Set<AnnotationMirror> leastUpperBounds(Collection<? extends AnnotationMirror> c1,
                Collection<? extends AnnotationMirror> c2) {
            Map<String, AnnotationMirror> ann =
                new HashMap<String, AnnotationMirror>();
            for (AnnotationMirror anno : c1)
                ann.put(AnnotationUtils.annotationName(anno).toString(), anno);
            for (AnnotationMirror anno : c2)
                ann.put(AnnotationUtils.annotationName(anno).toString(), anno);

            if (ann.containsKey(QReadOnly.class.getCanonicalName()))
                return Collections.singleton(QREADONLY);
            else if (ann.containsKey(ReadOnly.class.getCanonicalName()))
                return Collections.singleton(READONLY);
            else if (ann.containsKey(PolyRead.class.getCanonicalName()))
                return Collections.singleton(POLYREAD);
            else
                return Collections.singleton(MUTABLE);
        }
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new JavariTypeHierarchy(checker, qualHierarchy,
                                       checker.hasOption("ignoreRawTypeArguments"),
                                       checker.hasOption("invariantArrays"));
    }


    /**
     * Implements the {@code @QReadOnly} behavior on generic types,
     * creating a new {@link org.checkerframework.framework.type.TypeHierarchy} class that allows a
     * comparison of type arguments to succeed if the left hand side
     * is annotated with {@code @QReadOnly} or if the regular
     * comparison succeeds.
     */
    private class JavariTypeHierarchy extends DefaultTypeHierarchy  {

        public JavariTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy,
                                   boolean ignoreRawTypes, boolean invariantArrayComponents) {
            super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
        }

        @Override
        public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
            return subtype.getKind().isPrimitive()
                || supertype.getKind().isPrimitive()
                || super.isSubtype(subtype, supertype, visited);
        }

        @Override
        protected boolean isContainedBy(AnnotatedTypeMirror inside, AnnotatedTypeMirror outside,
                                        VisitHistory visited, boolean canBeCovariant) {
            return outside.hasEffectiveAnnotation(QREADONLY)
               || super.isContainedBy(inside, outside, visited, canBeCovariant);
        }
    }

}
