package org.checkerframework.checker.igj;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

import org.checkerframework.checker.igj.qual.AssignsFields;
import org.checkerframework.checker.igj.qual.I;
import org.checkerframework.checker.igj.qual.Immutable;
import org.checkerframework.checker.igj.qual.Mutable;
import org.checkerframework.checker.igj.qual.ReadOnly;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeVisitor;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;

/**
 * Adds implicit and default IGJ annotations, only if the user does not
 * annotate the type explicitly.  The default annotations are designed
 * to minimize the number of {@code Immutable} or {@code ReadOnly}
 * appearing in the source code.
 * <p>
 *
 * Implicit Annotations for literals:<br>
 * Immutable  -  any primitive literal (e.g. integer, long, boolean, etc.)<br>
 * IGJBottom  -  a null literal
 * <p>
 *
 * However, due to the default setting being similar to the implicit
 * annotations, there is no significant distinction between the two in
 * implementation.
 * <p>
 *
 * Default Annotations:
 * <p>
 *
 * This factory will add the {@link Immutable} annotation to a type if the
 * input is
 * <ol>
 * <li value="1">(*)a primitive type,
 * <li value="2">a known immutable type, if the class type is annotated as
 *    {@code Immutable}
 * </ol>
 *
 * It will add the {@link ReadOnly} annotation to a type if the input is
 * <ol>
 * <li value="3">a method receiver for an immutable class
 * <li value="4">a result of unification of different immutabilities (e.g.
 *    within Conditional Expressions)
 * <li value="5">supertype of a wildcard/type parameter in a class/method declaration
 * </ol>
 *
 * It will add {@link IGJBottom}, a special bottom annotation to a type if
 * the input can be assigned to anything, like the following cases:
 * <ol>
 * <li value="6">(*)the input is a {@code null} literal
 * <li value="7">(*)the input is an unannotated new array tree
 * <li value="8">the input is an unannotated new class tree invoking a constructor
 *    of {@code ReadOnly} or {@code AssignsFields} receiver type
 * <li value="9">the input is the class or interface declaration
 * </ol>
 *
 * It will add the {@link Mutable} annotation to a type if
 * <ol>
 * <li value="10">any remaining unqualified types (i.e. Mutable is the default)
 * </ol>
 *
 * Implementation detail:  (*) cases are handled with a meta annotation
 * rather than in this class.
 * <p>
 *
 * Furthermore, it resolves {@link I} annotation to the proper annotation,
 * according to its specification (described in {@link I} javadoc).
 */
//
// To ease dealing with libraries, this inserts the bottom qualifier
// rather than immutable in many cases, like all literals.
// Should change that
public class IGJAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    //
    // IGJ tries to adhere to the various rules specified by the
    // type system and the conventions of the framework, except for two
    // things:
    // 1. overloading the meaning of BOTTOM_QUAL
    //    Review the javadoc of #createQualiferHierarchy
    //
    // 2. Having two qualifiers for a given type in one particular case
    //    which is that the self type (i.e. type of 'this' identifier) within
    //    a method with an AssignsFields receiver within I classes, then the self type is
    //    '@AssignsFields @I EnclosingClass' and they are treated as
    //    Incomparable.  This is useful in the following cases:
    //
    //    a. for method invocability tests, a method with an AssignsFields receiver from within
    //       a readonly context can be called only via AssignsFields reference
    //       of 'this'.  I cannot be a receiver type, so it doesn't interfere.
    //
    //    b. for assignment, 'this' can be assigned to '@I EnclosingClass'
    //       reference within such methods (assignment encompasses the escape
    //       of this when passed to method parameters).  Fields and variables
    //       cannot be AssignsFields, so it's safe.
    //
    //    The design of QualifierHierarchy.isSubtype(Collection, Collection)
    //    reflect this choice.
    //
    /** Supported annotations for IGJ.  Used for subtyping rules. **/
    protected final AnnotationMirror READONLY, MUTABLE, IMMUTABLE, I, ASSIGNS_FIELDS, BOTTOM_QUAL;

    /** the {@link I} annotation value key */
    protected static final String IMMUTABILITY_KEY = "value";

    /**
     * Constructor for IGJAnnotatedTypeFactory object.
     *
     * @param checker the checker to which this factory belongs
     */
    public IGJAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        READONLY = AnnotationUtils.fromClass(elements, ReadOnly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        I = AnnotationUtils.fromClass(elements, I.class);
        ASSIGNS_FIELDS = AnnotationUtils.fromClass(elements, AssignsFields.class);
        BOTTOM_QUAL = AnnotationUtils.fromClass(elements, IGJBottom.class);

        addAliasedAnnotation(org.jmlspecs.annotation.Immutable.class, IMMUTABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        addAliasedAnnotation(net.jcip.annotations.Immutable.class, IMMUTABLE);

        // TODO: Add an alias for the Pure JML annotation. It's not a type qualifier, I think adding
        // it above does not work. Also see NullnessAnnotatedTypeFactory.
        // this.addAliasedDeclAnnotation(org.jmlspecs.annotation.Pure.class, Pure.class, annotationToUse);

        this.postInit();
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new IGJTreePreAnnotator(this)
        );
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                new IGJTypePostAnnotator(this),
                super.createTypeAnnotator()
        );
    }

    // TODO: do store annotations into the Element -> remove this override
    // Currently, many test cases fail without this.
    @Override
    public void postProcessClassTree(ClassTree tree) {
    }

    // **********************************************************************
    // add implicit annotations
    // **********************************************************************

    /**
     * Helper class for annotating unannotated types.
     */
    private class IGJTypePostAnnotator extends TypeAnnotator {
        public IGJTypePostAnnotator(IGJAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * For Declared types:
         *  Classes are mutable
         *  Interface declaration are placeholders
         *  Enum and annotations  are immutable
         */
        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
            if (!hasImmutabilityAnnotation(type)) {
                // Actual element
                TypeElement element = (TypeElement)type.getUnderlyingType().asElement();
                AnnotatedDeclaredType elementType = fromElement(element);
                // ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;

                if (TypesUtils.isBoxedPrimitive(type.getUnderlyingType())
                        || element.getQualifiedName().contentEquals("java.lang.String")
                        || ElementUtils.isObject(element)) {
                    // variation of case 1
                    // TODO: These cases are more of hacks and they should
                    // really be immutable or readonly
                    type.addAnnotation(BOTTOM_QUAL);
                } else if (elementType.hasEffectiveAnnotation(IMMUTABLE)) {
                    // case 2: known immutable types
                    type.addAnnotation(IMMUTABLE);
                }
            }
            return null; //super.visitDeclared(type, p);
            /*
            if (!hasImmutabilityAnnotation(type)) {
                // Actual element
                TypeElement element = (TypeElement)type.getUnderlyingType().asElement();
                AnnotatedDeclaredType elementType = fromElement(element);
                // ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;

                if (TypesUtils.isBoxedPrimitive(type.getUnderlyingType())
                        || element.getQualifiedName().contentEquals("java.lang.String")
                        || ElementUtils.isObject(element)) {
                    // variation of case 1
                    // TODO: These cases are more of hacks and they should
                    // really be immutable or readonly
                    type.replaceAnnotation(BOTTOM_QUAL);
                } else if (elementType.hasEffectiveAnnotation(IMMUTABLE)) {
                    // case 2: known immutable types
                    type.replaceAnnotation(IMMUTABLE);
                //} else if (elemKind == ElementKind.LOCAL_VARIABLE) {
                //    type.replaceAnnotation(READONLY);
                } else if (elementType.hasEffectiveAnnotation(MUTABLE)) { // not immutable
                    // case 7: mutable by default
                    type.replaceAnnotation(MUTABLE);
                //} else if (elemKind.isClass() || elemKind.isInterface()) {
                    // case 9: class or interface declaration
                //    type.replaceAnnotation(BOTTOM_QUAL);
                //} else if (elemKind.isField()) {
                    /*
                        && type.getElement() != null // We don't know the field context here
                        && getAnnotatedType(ElementUtils.enclosingClass(type.getElement())).hasEffectiveAnnotation(IMMUTABLE)) {
                    type.replaceAnnotation(IMMUTABLE);
                    TODO: This case is not exercised by any of the test cases. Is it needed?

                } else if (element.getKind().isClass() || element.getKind().isInterface()) {
                    // case 10
                    type.replaceAnnotation(MUTABLE);
                } else {
                    assert false : "shouldn't be here!";
                }

            }
            return super.visitDeclared(type, p);
            */
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Void p) {
            AnnotatedDeclaredType receiver;
            if (type.getElement().getKind() == ElementKind.CONSTRUCTOR) {
                receiver = (AnnotatedDeclaredType) type.getReturnType();
            } else {
                receiver = type.getReceiverType();
            }

            if (receiver != null &&
                    hasImmutabilityAnnotation(receiver)) {
                return super.visitExecutable(type, p);
            }

            TypeElement ownerElement = ElementUtils.enclosingClass(type.getElement());
            AnnotatedDeclaredType ownerType = getAnnotatedType(ownerElement);

            if (type.getElement().getKind() == ElementKind.CONSTRUCTOR) {
                // TODO: hack
                if (ownerType.hasEffectiveAnnotation(MUTABLE) || ownerType.hasEffectiveAnnotation(BOTTOM_QUAL))
                    receiver.replaceAnnotation(MUTABLE);
                else
                    receiver.replaceAnnotation(ASSIGNS_FIELDS);
            } else if (receiver == null) {
                // Nothing to do for static methods.
            } else if (ElementUtils.isObject(ownerElement) || ownerType.hasEffectiveAnnotation(IMMUTABLE)) {
                // case 3
                receiver.replaceAnnotation(BOTTOM_QUAL);
            } else {
                // case 10: rest
                receiver.replaceAnnotation(MUTABLE);
            }

            return super.visitExecutable(type, p);
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            // In a declaration the upperbound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getUpperBoundField() != null
                    && !hasImmutabilityAnnotation(type.getUpperBoundField())) {
                // ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;
                /*if (elemKind.isClass() || elemKind.isInterface()
                        || elemKind == ElementKind.CONSTRUCTOR
                        || elemKind == ElementKind.METHOD)
                    // case 5: upper bound within a class/method declaration
                    type.getUpperBoundField().replaceAnnotation(READONLY);
                else*/ if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getUpperBoundField().replaceAnnotation(MUTABLE);
            }

            return super.visitTypeVariable(type, p);
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, Void p) {
            // In a declaration the upper bound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getExtendsBound() != null
                    && !hasImmutabilityAnnotation(type.getExtendsBound())) {
                // ElementKind elemKind = elem != null ? elem.getKind() : ElementKind.OTHER;
                /*if (elemKind.isClass() || elemKind.isInterface()
                        || elemKind == ElementKind.CONSTRUCTOR
                        || elemKind == ElementKind.METHOD)
                    // case 5: upper bound within a class/method declaration
                    type.getExtendsBound().replaceAnnotation(READONLY);
                else*/ if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getExtendsBound().replaceAnnotation(MUTABLE);
            }

            return super.visitWildcard(type, p);
        }
    }

    /**
     * Helper class to annotate trees.
     *
     * It only adds a BOTTOM_QUAL for new classes and new arrays,
     * when an annotation is not specified
     */
    private class IGJTreePreAnnotator extends TreeAnnotator {

        public IGJTreePreAnnotator(IGJAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            /*
            if (node.getClassBody() != null) {
                System.out.println("Visit anonymous: " + node + " + input: " + p);
                AnnotatedTypeMirror tt = IGJAnnotatedTypeFactory.this.getAnnotatedType(node.getIdentifier());
                p.replaceAnnotations(tt.getAnnotations());
                System.out.println("  final type: " + p);
                // Is this the right way to handle anonymous classes?
            } else */
            if (!hasImmutabilityAnnotation(p)) {
                AnnotatedTypeMirror ct = fromElement(
                        ((AnnotatedDeclaredType)p).getUnderlyingType().asElement());

                if (!hasImmutabilityAnnotation(ct) || ct.hasAnnotationRelaxed(I)) {
                    AnnotatedExecutableType con = getAnnotatedType(TreeUtils.elementFromUse(node));
                    if (con.getReceiverType() != null &&
                            con.getReceiverType().hasEffectiveAnnotation(IMMUTABLE))
                        p.replaceAnnotation(IMMUTABLE);
                    else
                        p.replaceAnnotation(MUTABLE);
                } else {
                    // case 2: known immutability type
                    p.addAnnotations(ct.getAnnotations());
                }
            }

            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror p) {
            if (!hasImmutabilityAnnotation(p)) {
                AnnotatedTypeMirror castedType = getAnnotatedType(node.getExpression());
                p.addAnnotations(castedType.getAnnotations());
            }
            return null;
        }
    }

    @Override
    protected AnnotatedDeclaredType getImplicitReceiverType(ExpressionTree tree) {
        AnnotatedDeclaredType receiver = super.getImplicitReceiverType(tree);
        if (receiver != null && !isMostEnclosingThisDeref(tree)) {
            receiver.replaceAnnotation(READONLY);
        }
        return receiver;
    }

    /**
     * Returns the type of field {@code this},  for the scope of this tree.
     * In IGJ, the self type is the method receiver in this scope.
     */
    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType act = getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver;
        if (isWithinConstructor(tree)) {
            methodReceiver = (AnnotatedDeclaredType) getAnnotatedType(visitorState.getMethodTree()).getReturnType();
        } else {
            methodReceiver = getCurrentMethodReceiver(tree);
        }

        if (methodReceiver == null)
            return act;
        // Are we in a mutable or Immutable scope
        if (isWithinConstructor(tree) && !methodReceiver.hasEffectiveAnnotation(MUTABLE)) {
            methodReceiver.replaceAnnotation(ASSIGNS_FIELDS);
        }

        if (methodReceiver.hasEffectiveAnnotation(MUTABLE) ||
                methodReceiver.hasEffectiveAnnotation(IMMUTABLE)) {
            return methodReceiver;
        } else if (act.hasAnnotationRelaxed(I) || act.hasEffectiveAnnotation(IMMUTABLE)) {
            if (methodReceiver.hasEffectiveAnnotation(ASSIGNS_FIELDS))
                act.replaceAnnotation(ASSIGNS_FIELDS);
            return act;
        } else
            return methodReceiver;
    }

    // **********************************************************************
    // resolving @I Immutability
    // **********************************************************************

    /**
     * Replace all instances of {@code @I} in the super types with the
     * immutability of the current type
     *
     * @param type  the type whose supertypes are requested
     * @param supertypes    the supertypes of type
     */
    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        Map<String, AnnotationMirror> templateMapping =
            new ImmutabilityTemplateCollector().visit(type);

        new ImmutabilityResolver().visit(supertypes, templateMapping);
        for (AnnotatedTypeMirror supertype: supertypes) {
            typeAnnotator.visit(supertype, null);
        }
    }

    /**
     * Resolve the instances of {@code @I} in the {@code elementType} based
     * on {@code owner}, according to is specification.
     */
    @Override
    public void postAsMemberOf(AnnotatedTypeMirror elementType,
            AnnotatedTypeMirror owner, Element element) {
        resolveImmutabilityTypeVar(elementType, owner);
    }

    @Override
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type,
            Set<AnnotationMirror> fromClass) {
        // Ignore annotations inherited from a class.
        // TODO: this mechanism is implemented in special IGJ logic and
        // should be cleaned up.
    }

    /**
     * Resolves {@code @I} in the type of the method type base on the method
     * invocation tree parameters.  Any unresolved {@code @I}s is resolved to a
     * place holder type.
     *
     * It resolves {@code @I} annotation in the following way:
     * <ul>
     *  <li>based on the tree receiver, done automatically through implicit
     *      invocation of
     *      {@link AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)}</li>
     *  <li>based on the invocation passed parameters</li>
     *  <li>if any yet unresolved immutability variables get resolved to a
     *      wildcard type</li>
     * </ul>
     */
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
        AnnotatedExecutableType type = mfuPair.first;

        // javac produces enum super calls with zero arguments even though the
        // method element requires two.
        // See also BaseTypeVisitor.visitMethodInvocation and
        // CFGBuilder.CFGTranslationPhaseOne.visitMethodInvocation
        if (TreeUtils.isEnumSuper(tree)) return mfuPair;

        List<AnnotatedTypeMirror> requiredArgs = AnnotatedTypes.expandVarArgs(this, type, tree.getArguments());
        List<AnnotatedTypeMirror> arguments = AnnotatedTypes.getAnnotatedTypes(this, requiredArgs, tree.getArguments());

        ImmutabilityTemplateCollector collector = new ImmutabilityTemplateCollector();
        Map<String, AnnotationMirror> matchingMapping = collector.visit(arguments, requiredArgs);
        if (!matchingMapping.isEmpty())
            new ImmutabilityResolver().visit(type, matchingMapping);

        // For finding resolved types, rather than to actually resolve immutability
        Map<String, AnnotationMirror> fromReceiver = collector.visit(getReceiverType(tree));
        final Map<String, AnnotationMirror> mapping =
            collector.reduce(matchingMapping, fromReceiver);
        new AnnotatedTypeScanner<Void, Void>() {
            @Override
            public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
                if (type.hasAnnotationRelaxed(I)) {
                    AnnotationMirror anno =
                        type.getAnnotation(I.class);
                    if (!mapping.containsValue(anno)) {
                        type.replaceAnnotation(BOTTOM_QUAL);
                    }
                }
                return super.visitDeclared(type, p);
            }
        }.visit(type);
        return mfuPair;
    }

    /**
     * Infers the immutability of {@code @I}s based on the provided types, and
     * replace all instances of {@code @I} with their corresponding qualifiers.
     * The {@code @I} annotations that are not resolved are left intact.
     *
     * @param type      the type with {@code @I} annotation
     * @param provided  the types with qualifiers that may be bound to
     *                  {@code @I}
     * @return true iff a qualifier has been resolved.
     */
    private boolean resolveImmutabilityTypeVar(AnnotatedTypeMirror type,
            AnnotatedTypeMirror ...provided) {
        ImmutabilityTemplateCollector collector = new ImmutabilityTemplateCollector();

        // maps the @I values to I resolved annotations
        Map<String, AnnotationMirror> templateMapping = Collections.emptyMap();

        for (AnnotatedTypeMirror pt : provided)
            templateMapping = collector.reduce(templateMapping, collector.visit(pt));

        // There is nothing to resolve
        if (templateMapping.isEmpty())
            return false;

        new ImmutabilityResolver().visit(type, templateMapping);
        return true;
    }

    /**
     * A helper class that resolves the immutability on a types based on a
     * provided mapping.
     *
     * It returns a set of the annotations that were inserted. This is important
     * to recognize which immutability type variables were resolved and which
     * are to be made into place holder.
     */
    private class ImmutabilityResolver extends
    AnnotatedTypeScanner<Void, Map<String, AnnotationMirror>> {

        public void visit(Iterable<? extends AnnotatedTypeMirror> types,
                Map<String, AnnotationMirror> templateMapping) {
            if (templateMapping != null && !templateMapping.isEmpty()) {
                for (AnnotatedTypeMirror type : types)
                    visit(type, templateMapping);
            }
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type,
                Map<String, AnnotationMirror> p) {
            if (type.hasAnnotationRelaxed(I)) {
                String immutableString =
                    AnnotationUtils.getElementValue(getImmutabilityAnnotation(type),
                            IMMUTABILITY_KEY, String.class, true);
                if (p.containsKey(immutableString)) {
                    type.replaceAnnotation(p.get(immutableString));
                }
            }

            return super.visitDeclared(type, p);
        }
    }

    /**
     * A Helper class that tries to resolve the immutability type variable,
     * as the type variable is assigned to the most restricted immutability
     */
    private class ImmutabilityTemplateCollector
    extends SimpleAnnotatedTypeVisitor<Map<String, AnnotationMirror>, AnnotatedTypeMirror> {

        public Map<String, AnnotationMirror> reduce(Map<String, AnnotationMirror> r1,
                Map<String, AnnotationMirror> r2) {
            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (r1 != null)
                result.putAll(r1);

            if (r2 != null) {
                // Need to be careful about overlap
                for (String key : r2.keySet()) {
                    if (!result.containsKey(key))
                        result.put(key, r2.get(key));
                    else if (!AnnotationUtils.areSame(result.get(key), r2.get(key)))
                        result.put(key, READONLY);
                }
            }
            return result;
        }

        public Map<String, AnnotationMirror> visit(Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> actualTypes) {
            Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();

            Iterator<? extends AnnotatedTypeMirror> itert = types.iterator();
            Iterator<? extends AnnotatedTypeMirror> itera = actualTypes.iterator();

            while (itert.hasNext() && itera.hasNext()) {
                AnnotatedTypeMirror type = itert.next();
                AnnotatedTypeMirror actualType = itera.next();
                result = reduce(result, visit(type, actualType));
            }
            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitDeclared(
                AnnotatedDeclaredType type, AnnotatedTypeMirror actualType) {

            if (actualType == null) {
                TypeElement elem = (TypeElement)type.getUnderlyingType().asElement();
                actualType = fromElement(elem);
            }

            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (typeVar.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                typeVar.add((TypeVariable)actualType.getUnderlyingType());
                Map<String, AnnotationMirror> result = visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                typeVar.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD)
                return visit(type, ((AnnotatedWildcardType)actualType).getExtendsBound());

            if (actualType.getKind() != type.getKind())
                return Collections.emptyMap();

            assert actualType.getKind() == type.getKind();
            type = (AnnotatedDeclaredType) AnnotatedTypes.asSuper(types, IGJAnnotatedTypeFactory.this, type, actualType);
            if (type == null)
                return Collections.emptyMap();
            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType)actualType;

            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (dcType.hasAnnotationRelaxed(I)) {
                String immutableString =
                    AnnotationUtils.getElementValue(getImmutabilityAnnotation(dcType),
                            IMMUTABILITY_KEY, String.class, true);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // TODO: Assertion fails some times
                // assert immutability != null;
                if (immutability!=null && !immutability.equals(ASSIGNS_FIELDS))
                    result.put(immutableString, immutability);
            }

            if (type != dcType && !type.wasRaw() && !dcType.wasRaw()) {
                result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));
            }
            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {
            if (actualType == null)
                return visit(type.getComponentType(), null);
            if (actualType.getKind() == TypeKind.DECLARED)
                return visit(AnnotatedTypes.asSuper(types, IGJAnnotatedTypeFactory.this, type, actualType), actualType);

            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (typeVar.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                typeVar.add((TypeVariable)actualType.getUnderlyingType());
                Map<String, AnnotationMirror> result = visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                typeVar.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD)
                return visit(type, ((AnnotatedWildcardType)actualType).getExtendsBound());

            if (type.getKind() != actualType.getKind())
                return visit(type, null);
            assert type.getKind() == actualType.getKind();
            AnnotatedArrayType arType = (AnnotatedArrayType)actualType;

            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (arType.hasAnnotationRelaxed(I)) {
                String immutableString =
                    AnnotationUtils.getElementValue(getImmutabilityAnnotation(arType),
                            IMMUTABILITY_KEY, String.class, true);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // Assertion fails some times
                assert immutability != null;
                if (!type.hasEffectiveAnnotation(ASSIGNS_FIELDS))
                    result.put(immutableString, immutability);
            }

            result = reduce(result, visit(type.getComponentType(), arType.getComponentType()));
            return result;
        }

        private final Set<TypeVariable> typeVar = new HashSet<TypeVariable>();

        @Override
        public Map<String, AnnotationMirror> visitTypeVariable(
                AnnotatedTypeVariable type, AnnotatedTypeMirror actualType) {
            if (actualType == null)
                return Collections.emptyMap();

            if (actualType.getKind() == TypeKind.WILDCARD
                    && ((AnnotatedWildcardType)actualType).getSuperBound() != null)
                actualType = ((AnnotatedWildcardType)actualType).getSuperBound();

            AnnotatedTypeMirror typeSuper = findType(type, actualType);
            if (typeSuper.getKind() != TypeKind.TYPEVAR)
                return visit(typeSuper, actualType);

            assert typeSuper.getKind() == actualType.getKind() : actualType;
            assert type.getKind() == actualType.getKind() : actualType;
            AnnotatedTypeVariable tvType = (AnnotatedTypeVariable)typeSuper;

            typeVar.add(type.getUnderlyingType());
            // a type variable cannot be annotated
            Map<String, AnnotationMirror> result = visit(type.getUpperBound(), tvType.getUpperBound());
            typeVar.remove(type.getUnderlyingType());
            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitWildcard(
                AnnotatedWildcardType type, AnnotatedTypeMirror actualType) {
            if (actualType == null)
                return Collections.emptyMap();

            AnnotatedTypeMirror typeSuper = findType(type, actualType);
            if (typeSuper.getKind() != TypeKind.WILDCARD)
                return visit(typeSuper, actualType);
            // TODO: Fix this
            if (typeSuper.getKind() != actualType.getKind())
                return Collections.emptyMap();

            assert typeSuper.getKind() == actualType.getKind() : actualType;
            AnnotatedWildcardType wcType = (AnnotatedWildcardType)typeSuper;

            if (type.getExtendsBound() != null && wcType.getExtendsBound() != null)
                return visit(type.getExtendsBound(), wcType.getExtendsBound());
            else if (type.getSuperBound() != null && wcType.getSuperBound() != null)
                return visit(type.getSuperBound(), wcType.getSuperBound());
            else
                return new HashMap<String, AnnotationMirror>();
        }

        private AnnotatedTypeMirror findType(AnnotatedTypeMirror type, AnnotatedTypeMirror actualType) {
            AnnotatedTypeMirror result = AnnotatedTypes.asSuper(types, IGJAnnotatedTypeFactory.this, type, actualType);
            // result shouldn't be null, will test this hypothesis later
            // assert result != null;
            return (result != null ? result : type);
        }
    }

    // **********************************************************************
    // Random utility methods
    // **********************************************************************

    /**
     * Returns the annotation specifying the immutability type of {@code type}.
     */
    private AnnotationMirror getImmutabilityAnnotation(AnnotatedTypeMirror type) {
        // @I and @AssignsFields annotate the type of 'this' together
        // this one ensures that it returns @I
        //
        if (type.hasAnnotationRelaxed(I))
            return type.getAnnotation(I.class);
        if (hasImmutabilityAnnotation(type)) {
            return type.getAnnotationInHierarchy(READONLY);
        } else {
            return null;
        }
    }

    /**
     * @return  true iff the type has an immutability qualifier,
     *          false otherwise
     */
    private boolean hasImmutabilityAnnotation(AnnotatedTypeMirror type) {
        return type.isAnnotatedInHierarchy(READONLY);
    }

    // **********************************************************************
    // Factory methods
    // **********************************************************************

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new IGJQualifierHierarchy(factory);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new IGJTypeHierarchy(checker, getQualifierHierarchy(),
                                    checker.hasOption("ignoreRawTypeArguments"),
                                    checker.hasOption("invariantArrays"));
    }

    //
    // IGJ makes an interesting use of BOTTOM_QUAL (IGJBottom).  It gets used
    // in two ways:
    //
    // __ AS BOTTOM QUALIFIER __
    //
    // This is the intended use and design for it
    //
    // A bottom qualifier is needed to annotate some expressions, like 'null'.
    // Otherwise, null would need to be '@Mutable @Immutable <nulltype>' so
    // it could be assigned to everything.
    //
    // __ AS SUPER QUALIFIER __
    //
    // As I used IGJ, I realized that it's useful to have an annotation
    // that acts as a place holder qualifier that is a supertype of
    // everything.  The semantics of such qualifier is a bit
    // different from ReadOnly.
    //
    // It's only because the existence of un-annotated code that we need this
    // annotation, and IGJ's promise that un-annotated code should type-check.
    //
    // TODO: Explain these cases more
    //
    private final class IGJQualifierHierarchy extends GraphQualifierHierarchy {
        public IGJQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, BOTTOM_QUAL);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, I) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, I)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, I)) {
                lhs = I;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, I)) {
                rhs = I;
            }
            return (AnnotationUtils.areSame(rhs, BOTTOM_QUAL)
                    || AnnotationUtils.areSame(lhs, BOTTOM_QUAL)
                    || super.isSubtype(rhs, lhs));
        }

        @Override
        public boolean isSubtype(Collection<? extends AnnotationMirror> rhs, Collection<? extends AnnotationMirror> lhs) {
            if (lhs.isEmpty() || rhs.isEmpty()) {
                ErrorReporter.errorAbort("GraphQualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
            }
            // TODO: sometimes there are multiple mutability annotations in a type and
            // the check in the superclass that the sets contain exactly one annotation
            // fails. I replaced "addAnnotation" calls with "replaceAnnotation" calls,
            // but then other test cases fail. Some love needed here.
            for (AnnotationMirror lhsAnno : lhs) {
                for (AnnotationMirror rhsAnno : rhs) {
                    if (isSubtype(rhsAnno, lhsAnno)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Represents the annotated type hierarchy of the IGJ type system.
     *
     * The IGJ type system diverges from the JLS in two ways:
     * 1. Type arguments are always co-variant with respect to
     *    {@link IGJBottom}
     *
     * 2. If the type is a read-only or an immutable type, then type arguments
     *    may change co-variantly in a safe manner
     */
    private final class IGJTypeHierarchy extends DefaultTypeHierarchy {

        public IGJTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy,
                                boolean ignoreRawTypes, boolean invariantArrayComponents) {
            super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents, true);
        }


        /**
         * Uses the JLS specification (as implemented in {@link org.checkerframework.framework.type.DefaultTypeHierarchy},
         * if the variable type, lhs, is mutable; otherwise, allows the type
         * arguments to change while maintaining subtype relationship.
         *
         * This allows for subtyping relationships of the kind:
         * <pre>  @Mutable List&lt;@Mutable Date&gt; &lt;: @ReadOnly List&lt;@ReadOnly Date&gt;<\pre>
         */
        @Override
        public Boolean visitTypeArgs(final AnnotatedDeclaredType subtype, final AnnotatedDeclaredType supertype,
                                      final VisitHistory visited, final boolean subtypeIsRaw, final boolean supertypeIsRaw) {

            boolean ignoreTypeArgs = ignoreRawTypes && (subtypeIsRaw || supertypeIsRaw);

            if(!ignoreTypeArgs) {
                if (supertype.hasEffectiveAnnotation(MUTABLE)) {
                    return super.visitTypeArgs(subtype, supertype, visited,  subtypeIsRaw, supertypeIsRaw);
                }

                return super.visitTypeArgs(subtype, supertype, visited, subtypeIsRaw, supertypeIsRaw);
            }

            return true;
        }

        //NOTE: This is needed because sometimes we call the supertype behavior of visitTypeArgs and
        @Override
        protected boolean isContainedBy(AnnotatedTypeMirror inside, AnnotatedTypeMirror outside,
                                        VisitHistory visited, boolean canBeCovariant) {
            //seems weird to have both inside and outside checked for BOTTOM_QUAL
            if (inside.hasEffectiveAnnotation(BOTTOM_QUAL) || outside.hasEffectiveAnnotation(BOTTOM_QUAL)) {
                return true;
            }

            return super.isContainedBy(inside, outside, visited, canBeCovariant);
        }
    }

}
