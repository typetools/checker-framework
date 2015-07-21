package org.checkerframework.qualframework.poly;

import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.QualifierMapVisitor;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.util.QualifierContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/** Type factory with qualifier polymorphism support.  This type factory
 * extends an underlying qualifier system with qualifier variables, combined
 * qualifiers (using {@link CombiningOperation}), wildcards, typechecking
 * support for all of the above, substitution for accessing fields whose types
 * refer to qualifier parameters, and qualifier inference for method qualifier
 * parameters.
 */
public abstract class QualifierParameterTypeFactory<Q> extends DefaultQualifiedTypeFactory<QualParams<Q>> {
    QualifierHierarchy<Q> groundHierarchy;

    public QualifierParameterTypeFactory(QualifierContext<QualParams<Q>> context) {
        super(context);
    }

    @Override
    protected abstract QualifierParameterAnnotationConverter<Q> createAnnotationConverter();

    @Override
    public QualifierParameterAnnotationConverter<Q> getAnnotationConverter() {
        return (QualifierParameterAnnotationConverter<Q>)super.getAnnotationConverter();
    }

    /** Create a {@link QualifierHierarchy} for ground qualifiers (represented
     * by instances of {@code Q}).
     */
    protected abstract QualifierHierarchy<Q> createGroundQualifierHierarchy();

    /** Get the ground qualifier hierarchy used by this type system. */
    public QualifierHierarchy<Q> getGroundQualifierHierarchy() {
        if (groundHierarchy == null) {
            groundHierarchy = createGroundQualifierHierarchy();
        }
        return groundHierarchy;
    }

    @Override
    protected QualifierHierarchy<QualParams<Q>> createQualifierHierarchy() {
        return QualifierParameterHierarchy.fromGround(getGroundQualifierHierarchy());
    }

    @Override
    protected QualifierParameterTypeAnnotator<Q> createTypeAnnotator() {
        return new QualifierParameterTypeAnnotator<Q>(getContext(), getAnnotationConverter(),
                new ContainmentHierarchy<>(new PolyQualHierarchy<>(getGroundQualifierHierarchy())));
    }

    @Override
    protected QualifierParameterTreeAnnotator<Q> createTreeAnnotator() {
        return new QualifierParameterTreeAnnotator<Q>(this);
    }

    /*
    public final QualParams<Q> applyCaptureConversion(QualParams<Q> objectQual) {
        if (objectQual == null || objectQual == QualParams.<Q>getBottom()
                || objectQual == QualParams.<Q>getTop())
            return objectQual;
        return objectQual.capture();
    }
    */

    /** Apply substitution to get the effective type of a class member when
     * accessed through an instance with particular qualifiers.  This method
     * roughly corresponds to AnnotatedTypes.asMemberOf.
     */
    private QualParams<Q> qualifierAsMemberOf(QualParams<Q> memberQual, QualParams<Q> objectQual) {

        if (memberQual == getQualifierHierarchy().getBottom()) {
            // Substituting in the object qualifier would not change the bottom qualifier.
            return getQualifierHierarchy().getBottom();

        } else if (objectQual == getQualifierHierarchy().getBottom()) {
            // objectQual (the receiver) is bottom. Right now just return the existing qualifier.
            // If objectQual is not an @Var, then nothing should have been substituted anyway.
            // If objectQual is an @Var, then what ground qualifier should be used?

            return memberQual;
        }

        return memberQual.substituteAll(objectQual);
    }

    /** Visitor to apply {@code qualifierAsMemberOf} at every location within a
     * {@link QualifiedTypeMirror}.
     */
    private final QualifierMapVisitor<QualParams<Q>, QualParams<Q>, QualParams<Q>> AS_MEMBER_OF_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, QualParams<Q>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> memberQual, QualParams<Q> objectQual) {
                return qualifierAsMemberOf(memberQual, objectQual);
            }
        };

    @Override
    public QualifiedTypeMirror<QualParams<Q>> postAsMemberOf(
            QualifiedTypeMirror<QualParams<Q>> memberType,
            QualifiedTypeMirror<QualParams<Q>> receiverType,
            Element memberElement) {

        // Don't run postAsMemberOf when viewing members from inside a class.
        if (receiverType.getUnderlyingType().isDeclaration()) {
            return memberType;
        }

        final QualParams<Q> effectiveReceiverQualifier;
        switch (receiverType.getKind()) {
            case WILDCARD:
                effectiveReceiverQualifier = ((QualifiedWildcardType<QualParams<Q>>) receiverType).getExtendsBound().getQualifier();
                break;
            case TYPEVAR:
                if (((QualifiedTypeVariable<QualParams<Q>>) receiverType).isPrimaryQualifierValid()) {
                    effectiveReceiverQualifier = receiverType.getQualifier();
                } else {
                    effectiveReceiverQualifier = this.getQualifiedTypeParameterBounds(
                            ((QualifiedTypeVariable<QualParams<Q>>) receiverType).getDeclaration().getUnderlyingType()).
                            getUpperBound().getQualifier();
                }
                break;
            default:
                effectiveReceiverQualifier = receiverType.getQualifier();
        }

        return AS_MEMBER_OF_VISITOR.visit(memberType, effectiveReceiverQualifier);
    }

    /** Visitor to apply substitution at every location within a {@link
     * QualifiedTypeMirror}.  This is used in {@code methodFromUse} to
     * substitute in the newly-inferred values for method qualifier parameters.
     */
    private final QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, Wildcard<Q>> substs) {
                if (params.equals(getQualifierHierarchy().getBottom())) {
                    return getQualifierHierarchy().getBottom();
                }
                return params.substituteAll(substs);
            }
        };


    @Override
    public Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> methodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, QualifiedTypeMirror<QualParams<Q>> receiverType) {

        Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> result = super.methodFromUse(tree,
                methodElt, receiverType);

        Element elt = result.first.getUnderlyingType().asElement();
        Set<String> qualParams = getAnnotationConverter().getDeclaredParameters(
                elt, getDeclAnnotations(elt), getDecoratedElement(elt));

        if (qualParams.isEmpty()) {
            // This check is not just a performance optimization - it saves us
            // from crashing in one obscure corner case.  An `enum`
            // declarations gets an auto-generated constructor with an
            // auto-generated `super()` call.  But the actual java.lang.Enum
            // constructor takes two arguments.  So trying to do inference on
            // that super call will cause a crash.  (This problem shows up as
            // an IndexOutOfBoundsException in tests/all-systems/Enums.java.)
            // The constructor has no qualifier parameters, though, so we can
            // skip processing it using this check.
            return result;
        }

        List<QualifiedTypeMirror<QualParams<Q>>> formals = new ArrayList<>();
        List<QualifiedTypeMirror<QualParams<Q>>> actuals = new ArrayList<>();
        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            formals.addAll(getQualifiedTypes().expandVarArgs(result.first, ((MethodInvocationTree)tree).getArguments()));
            for (ExpressionTree actualExpr : ((MethodInvocationTree)tree).getArguments()) {
                actuals.add(getQualifiedType(actualExpr));
            }
        }

        if (! ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
            // Need to include receivers in the inference.
            formals.add(result.first.getReceiverType());
            actuals.add(receiverType);
        }

        QualifierParameterHierarchy<Q> hierarchy = (QualifierParameterHierarchy<Q>)getQualifierHierarchy();
        MethodParameterInference<Q> inference = new MethodParameterInference<>(
                new ArrayList<>(qualParams), formals, actuals,
                groundHierarchy, new PolyQualHierarchy<>(groundHierarchy),
                hierarchy, getTypeHierarchy());

        Map<String, PolyQual<Q>> subst = inference.infer();

        if (subst != null) {
            Map<String, Wildcard<Q>> wildSubst = new HashMap<>();
            for (String name : subst.keySet()) {
                wildSubst.put(name, new Wildcard<>(subst.get(name)));
            }

            QualifiedExecutableType<QualParams<Q>> newMethodType =
                    (QualifiedExecutableType<QualParams<Q>>)SUBSTITUTE_VISITOR.visit(result.first, wildSubst);
            List<QualifiedTypeMirror<QualParams<Q>>> newTypeArgs = new ArrayList<>();
            for (QualifiedTypeMirror<QualParams<Q>> qtm : result.second) {
                newTypeArgs.add(SUBSTITUTE_VISITOR.visit(qtm, wildSubst));
            }
            result = Pair.of(newMethodType, newTypeArgs);
        } else {
            // TODO: report error
        }

        return result;
    }

    @Override
    public List<QualifiedTypeMirror<QualParams<Q>>> postDirectSuperTypes(
            QualifiedTypeMirror<QualParams<Q>> subtype,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> supertypes) {
        QualParams<Q> subQuals = subtype.getQualifier();
        if (subQuals == null) {
            return new ArrayList<>(supertypes);
        }

        List<QualifiedTypeMirror<QualParams<Q>>> result = new ArrayList<>();
        for (QualifiedTypeMirror<QualParams<Q>> supertype : supertypes) {
            QualParams<Q> superQuals;
            if (subQuals == getQualifierHierarchy().getBottom()) {
                // If subclass qualifier is bottom, use bottom for the superclass qualifier.

                // Substituting in bottom is undefined -- If there are any superclass qualifier parameters,
                // what should be the arguments to those parameters? Because of invariant subtyping, there
                // are no arguments where the resulting qualifier would still be bottom.

                superQuals = subQuals;
            } else {
                superQuals = supertype.getQualifier().substituteAll(subQuals);
                // substituteAll performs substitutions on the primary, but when viewing the superclass we want to
                // use the exact primary qualifier of the subclass.
                // This was needed to get the Ternary.java test to work.
                superQuals.setPrimary(subQuals.getPrimary());
            }

            result.add(SetQualifierVisitor.apply(supertype, superQuals));

        }

        return result;
    }
}
