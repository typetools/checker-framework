package checkers.igj;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

import checkers.igj.quals.I;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.Mutable;
import checkers.igj.quals.ReadOnly;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.*;

import com.sun.source.tree.*;

/**
 * Adds implicit and default IGJ annotations, only if the user does not
 * annotate the type explicitly.  The default annotations are designed
 * to minimize the number of {@code Immutable} or {@code ReadOnly}
 * appearing in the source code.
 * <p>
 *
 * Implicit Annotations for literals:<br/>
 * Immutable  -  any primitive literal (e.g. integer, long, boolean, etc.)<br/>
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
public class IGJAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<IGJChecker> {

    static {  FLOW_BY_DEFAULT = true;  }

    /** The various IGJ annotations. */
    private final AnnotationMirror READONLY, MUTABLE, IMMUTABLE, I,
            BOTTOM_QUAL, ASSIGNS_FIELDS;

    /** the {@link I} annotation value key */
    protected static final String IMMUTABILITY_KEY = "value";

    /**
     * Constructor for IGJAnnotatedTypeFactory object.
     *
     * @param checker the checker to which this factory belongs
     * @param root  the compilation unit the annotation processor is
     *              processing currently
     */
    public IGJAnnotatedTypeFactory(IGJChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        READONLY = checker.READONLY;
        MUTABLE = checker.MUTABLE;
        IMMUTABLE = checker.IMMUTABLE;
        I = checker.I;
        BOTTOM_QUAL = checker.BOTTOM_QUAL;
        ASSIGNS_FIELDS = checker.ASSIGNS_FIELDS;

        addAliasedAnnotation(org.jmlspecs.annotation.Immutable.class, IMMUTABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);

        // TODO: Add an alias for the Pure JML annotation. It's not a type qualifier, I think adding
        // it above does not work. Also see NullnessAnnotatedTypeFactory.

        this.postInit();
    }

    @Override
    protected Set<AnnotationMirror> createFlowQualifiers(IGJChecker checker) {
        Set<AnnotationMirror> flowQuals = AnnotationUtils.createAnnotationSet();
        for (Class<? extends Annotation> cl : checker.getSupportedTypeQualifiers()) {
            if (!I.class.equals(cl))
                flowQuals.add(annotations.fromClass(cl));
        }
        return flowQuals;
    }

    @Override
    protected TreeAnnotator createTreeAnnotator(IGJChecker checker) {
        return new IGJTreePreAnnotator(checker);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(IGJChecker checker) {
        return new IGJTypePostAnnotator(checker);
    }

    // **********************************************************************
    // add implicit annotations
    // **********************************************************************

    /**
     * Helper class for annotating unannotated types.
     */
    private class IGJTypePostAnnotator extends TypeAnnotator {
        public IGJTypePostAnnotator(IGJChecker checker) {
            super(checker);
        }

        /**
         * For Declared types:
         *  Classes are mutable
         *  Interface declaration are placeholders
         *  Enum and annotations  are immutable
         */
        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, ElementKind p) {
            if (!hasImmutabilityAnnotation(type)) {
                // Actual element
                TypeElement element = (TypeElement)type.getUnderlyingType().asElement();
                AnnotatedDeclaredType elementType = fromElement(element);

                if (TypesUtils.isBoxedPrimitive(type.getUnderlyingType())
                        || element.getQualifiedName().contentEquals("java.lang.String")
                        || ElementUtils.isObject(element)) {
                    // variation of case 1
                    // TODO: These cases are more of hacks and they should
                    // really be immutable or readonly
                    type.addAnnotation(BOTTOM_QUAL);
                } else if (elementType.hasEffectiveAnnotation(IMMUTABLE))
                    // case 2: known immutable types
                    type.addAnnotation(IMMUTABLE);
                else if (p == ElementKind.LOCAL_VARIABLE)
                    type.addAnnotation(READONLY);
                else if (elementType.hasEffectiveAnnotation(MUTABLE)) // not immutable
                    // case 7: mutable by default
                    type.addAnnotation(MUTABLE);
                else if (p.isClass() || p.isInterface())
                    // case 9: class or interface declaration
                    type.addAnnotation(BOTTOM_QUAL);
                else if (p.isField()
                        && type.getElement() != null // We don't know the field context here
                        && getAnnotatedType(ElementUtils.enclosingClass(type.getElement())).hasEffectiveAnnotation(IMMUTABLE)) {
                    type.addAnnotation(IMMUTABLE);
                }
                else if (element.getKind().isClass() || element.getKind().isInterface())
                    // case 10
                    type.addAnnotation(MUTABLE);
                else
                    assert false : "shouldn't be here!";

            }
            return super.visitDeclared(type,
                    p == ElementKind.LOCAL_VARIABLE || p == ElementKind.FIELD ? ElementKind.OTHER : p);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, ElementKind p) {
            if (hasImmutabilityAnnotation(type.getReceiverType()))
                return super.visitExecutable(type, p);

            AnnotatedDeclaredType receiver = type.getReceiverType();
            TypeElement ownerElement = ElementUtils.enclosingClass(type.getElement());
            AnnotatedDeclaredType ownerType = getAnnotatedType(ownerElement);

            if (type.getElement().getKind() == ElementKind.CONSTRUCTOR) {
                // TODO: hack
                if (ownerType.hasEffectiveAnnotation(MUTABLE) || ownerType.hasEffectiveAnnotation(BOTTOM_QUAL))
                    receiver.addAnnotation(MUTABLE);
                else
                    receiver.addAnnotation(ASSIGNS_FIELDS);
            } else if (ElementUtils.isObject(ownerElement) || ownerType.hasEffectiveAnnotation(IMMUTABLE)) {
                // case 3
                receiver.addAnnotation(BOTTOM_QUAL);
            } else {
                // case 10: rest
                receiver.addAnnotation(MUTABLE);
            }

            return super.visitExecutable(type, p);
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, ElementKind p) {
            // In a declaration the upperbound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getUpperBoundField() != null
                    && !hasImmutabilityAnnotation(type.getUpperBoundField())) {
                if (p.isClass() || p.isInterface()
                        || p == ElementKind.CONSTRUCTOR
                        || p == ElementKind.METHOD)
                    // case 5: upper bound within a class/method declaration
                    type.getUpperBoundField().addAnnotation(READONLY);
                else if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getUpperBoundField().addAnnotation(MUTABLE);
            }

            return super.visitTypeVariable(type, p);
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, ElementKind p) {
            // In a declaration the upper bound is ReadOnly, while
            // the upper bound in a use is Mutable
            if (type.getExtendsBound() != null
                    && !hasImmutabilityAnnotation(type.getExtendsBound())) {
                if (p.isClass() || p.isInterface()
                        || p == ElementKind.CONSTRUCTOR
                        || p == ElementKind.METHOD)
                    // case 5: upper bound within a class/method declaration
                    type.getExtendsBound().addAnnotation(READONLY);
                else if (TypesUtils.isObject(type.getUnderlyingType()))
                    // case 10: remaining cases
                    type.getExtendsBound().addAnnotation(MUTABLE);
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

        public IGJTreePreAnnotator(IGJChecker checker) {
            super(checker, IGJAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            if (!hasImmutabilityAnnotation(p)) {
                AnnotatedTypeMirror ct = fromElement(
                        ((AnnotatedDeclaredType)p).getUnderlyingType().asElement());

                if (!hasImmutabilityAnnotation(ct) || ct.hasAnnotationRelaxed(I)) {
                    AnnotatedExecutableType con = getAnnotatedType(TreeUtils.elementFromUse(node));
                    if (con.getReceiverType().hasEffectiveAnnotation(IMMUTABLE))
                        p.addAnnotation(IMMUTABLE);
                    else
                        p.addAnnotation(MUTABLE);
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
            receiver.removeAnnotation(ASSIGNS_FIELDS);
            receiver.addAnnotation(READONLY);
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
        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);

        if (methodReceiver == null)
            return act;
        // Are we in a mutable or Immutable scope
        if (isWithinConstructor(tree) && !methodReceiver.hasEffectiveAnnotation(MUTABLE)) {
            methodReceiver.clearAnnotations();
            methodReceiver.addAnnotation(ASSIGNS_FIELDS);
        }

        if (methodReceiver.hasEffectiveAnnotation(MUTABLE) ||
                methodReceiver.hasEffectiveAnnotation(IMMUTABLE)) {
            return methodReceiver;
        } else if (act.hasAnnotationRelaxed(I) || act.hasEffectiveAnnotation(IMMUTABLE)) {
            if (methodReceiver.hasEffectiveAnnotation(ASSIGNS_FIELDS))
                act.addAnnotation(ASSIGNS_FIELDS);
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
        for (AnnotatedTypeMirror supertype: supertypes)
            typeAnnotator.visit(supertype, ElementKind.OTHER);
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

    /**
     * Resolves {@code @I} in the type of the method type base on the method
     * invocation tree parameters.  Any unresolved {@code @I}s is resolved to a
     * place holder type.
     *
     * It resolves {@code @I} annotation in the following way:
     * <ul>
     *  <li>based on the tree receiver, done automatically through implicit
     *      invocation of
     *      {@link AnnotatedTypes#asMemberOf(AnnotatedTypeMirror, Element)}</li>
     *  <li>based on the invocation passed parameters</li>
     *  <li>if any yet unresolved immutability variables get resolved to a
     *      wildcard type</li>
     * </ul>
     */
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
        AnnotatedExecutableType type = mfuPair.first;

        List<AnnotatedTypeMirror> arguments = atypes.getAnnotatedTypes(tree.getArguments());
        List<AnnotatedTypeMirror> requiredArgs = atypes.expandVarArgs(type, tree.getArguments());
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
                        type.getAnnotation(I.class.getCanonicalName());
                    if (!mapping.containsValue(anno)) {
                        type.removeAnnotation(I);
                        type.addAnnotation(BOTTOM_QUAL);
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
                    AnnotationUtils.parseStringValue(getImmutabilityAnnotation(type),
                            IMMUTABILITY_KEY);
                if (p.containsKey(immutableString)) {
                    type.removeAnnotation(I);
                    type.addAnnotation(p.get(immutableString));
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
            type = (AnnotatedDeclaredType)atypes.asSuper(type, actualType);
            if (type == null)
                return Collections.emptyMap();
            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType)actualType;

            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (dcType.hasAnnotationRelaxed(I)) {
                String immutableString =
                    AnnotationUtils.parseStringValue(getImmutabilityAnnotation(dcType),
                            IMMUTABILITY_KEY);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // TODO: Assertion fails some times
                // assert immutability != null;
                if (immutability!=null && !immutability.equals(ASSIGNS_FIELDS))
                    result.put(immutableString, immutability);
            }

            if (type != dcType && type.isParameterized() && dcType.isParameterized())
                result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));
            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {
            if (actualType == null)
                return visit(type.getComponentType(), null);
            if (actualType.getKind() == TypeKind.DECLARED)
                return visit(atypes.asSuper(type, actualType), actualType);

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
                    AnnotationUtils.parseStringValue(getImmutabilityAnnotation(arType),
                            IMMUTABILITY_KEY);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // Assertion failes some times
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
            AnnotatedTypeMirror result = atypes.asSuper(type, actualType);
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
            return type.getAnnotation(I.class.getCanonicalName());
        if (hasImmutabilityAnnotation(type)) {
            return type.getAnnotations().iterator().next();
        } else {
            return null;
        }
    }

    /**
     * @return  true iff the type has an immutability qualifier,
     *          false otherwise
     */
    private boolean hasImmutabilityAnnotation(AnnotatedTypeMirror type) {
        // return type.hasAnnotation(READONLY) || type.hasAnnotation(MUTABLE) ||
        //        type.hasAnnotation(IMMUTABLE) || type.hasAnnotation(I);
        return type.isAnnotated();
    }
}
