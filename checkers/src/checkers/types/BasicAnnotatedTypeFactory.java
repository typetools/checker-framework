package checkers.types;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.DefaultFlow;
import checkers.flow.DefaultFlowState;
import checkers.flow.Flow;
import checkers.quals.DefaultLocation;
import checkers.quals.DefaultQualifier;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit annotations
 * via {@link ImplicitFor}, and user-specified defaults via {@link DefaultQualifier}
 *
 * @see Flow
 */
public class BasicAnnotatedTypeFactory<Checker extends BaseTypeChecker> extends AnnotatedTypeFactory {

    /** The type checker to use. */
    protected Checker checker;

    /** should use flow by default */
    protected static boolean FLOW_BY_DEFAULT = true;

    /** to annotate types based on the given tree */
    protected final TypeAnnotator typeAnnotator;

    /** to annotate types based on the given un-annotated types */
    protected final TreeAnnotator treeAnnotator;

    /** to handle any polymorphic types */
    protected final QualifierPolymorphism poly;

    /** to handle defaults specified by the user */
    protected final QualifierDefaults defaults;

    //// Flow related fields
    /** Should use flow analysis? */
    protected boolean useFlow;

    /** Flow sensitive instance */
    protected Flow flow;

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param root the compilation unit to scan
     * @param useFlow whether flow analysis should be performed
     */
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root, boolean useFlow) {
        super(checker, root);
        this.checker = checker;
        this.treeAnnotator = createTreeAnnotator(checker);
        this.typeAnnotator = createTypeAnnotator(checker);
        this.useFlow = useFlow;
        this.poly = new QualifierPolymorphism(checker, this);

        this.defaults = new QualifierDefaults(this, this.annotations);
        boolean foundDefault = false;
        for (Class<? extends Annotation> qual : checker.getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.addAbsoluteDefault(this.annotations.fromClass(qual),
                        Collections.singleton(DefaultLocation.ALL));
                foundDefault = true;
            }
        }

        AnnotationMirror unqualified = this.annotations.fromClass(Unqualified.class);
        if (!foundDefault && this.isSupportedQualifier(unqualified)) {
            defaults.addAbsoluteDefault(unqualified,
                    Collections.singleton(DefaultLocation.ALL));
        }

        // every subclass must call postInit!
        if (this.getClass().equals(BasicAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    @Override
    protected void postInit() {
        super.postInit();

        /*
         * Create the flow instance here, as subclasses might need to
         * initialize additional fields first.
         * For example, in NullnessATF, the rawnessFactory needs to be
         * initialized before calling scan.
         */
        Set<AnnotationMirror> flowQuals = createFlowQualifiers(checker);
        this.flow = useFlow ? createFlow(checker, root, flowQuals) : null;
        if (flow!=null) {
            flow.scan(root);
        }
    }

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param root the compilation unit to scan
     */
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root) {
        this(checker, root, FLOW_BY_DEFAULT);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    /**
     * Returns a {@link TreeAnnotator} that adds annotations to a type based
     * on the contents of a tree.
     *
     * Subclasses may override this method to specify more appriopriate
     * {@link TreeAnnotator}
     *
     * @return a tree annotator
     */
    protected TreeAnnotator createTreeAnnotator(Checker checker) {
        return new TreeAnnotator(checker, this);
    }

    /**
     * Returns a {@link TypeAnnotator} that adds annotations to a type based
     * on the content of the type itself.
     *
     * @return a type annotator
     */
    protected TypeAnnotator createTypeAnnotator(Checker checker) {
        return new TypeAnnotator(checker);
    }

    /**
     * Returns a {@link Flow} instance that performs flow sensitive analysis
     * to infer qualifiers on unqualified types.
     *
     * @param checker   the checker
     * @param root      the compilation unit associated with this factory
     * @param flowQuals the qualifiers to infer
     * @return  the flow analysis class
     */
    protected Flow createFlow(Checker checker, CompilationUnitTree root,
            Set<AnnotationMirror> flowQuals) {
        return new DefaultFlow<DefaultFlowState>(checker, root, flowQuals, this);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED)
            for (AnnotatedTypeMirror supertype : supertypes) {
                // FIXME: Recursive initialization for defaults fields
                if (defaults != null) {
                    defaults.annotate(((DeclaredType)supertype.getUnderlyingType()).asElement(), supertype);
                }
            }
    }

    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type,
            boolean iUseFlow) {
        assert root != null : "root needs to be set when used on trees";
        treeAnnotator.visit(tree, type);
        Element elt = InternalUtils.symbol(tree);
        typeAnnotator.visit(type, elt != null ? elt.getKind() : ElementKind.OTHER);
        defaults.annotate(tree, type);

        if (iUseFlow) {
            final Set<AnnotationMirror> inferred = flow.test(tree);
            if (inferred != null) {
                for (AnnotationMirror inf : inferred) {
                    AnnotationMirror present = type.getAnnotationInHierarchy(inf);
                    if (present!=null) {
                        if (this.qualHierarchy.isSubtype(inf, present)) {
                            // TODO: why is the above check needed? Shouldn't inferred
                            // qualifiers always be subtypes?
                            type.replaceAnnotation(inf);
                        }
                    } else {
                        type.addAnnotation(inf);
                    }
                }
            }
        }
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        annotateImplicit(tree, type, this.useFlow);
    }

    @Override
    public AnnotatedTypeMirror getDefaultedAnnotatedType(VariableTree tree) {
        AnnotatedTypeMirror res = this.fromMember(tree);
        this.annotateImplicit(tree.getType(), res, false);
        return res;
    }

    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typeAnnotator.visit(type, elt.getKind());
        defaults.annotate(elt, type);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;
        poly.annotate(tree, method);
        return mfuPair;
    }

    // **********************************************************************
    // Helper method
    // **********************************************************************

    /**
     * Returns the set of annotations to be inferred in flow analysis
     */
    protected Set<AnnotationMirror> createFlowQualifiers(Checker checker) {
        Set<AnnotationMirror> flowQuals = AnnotationUtils.createAnnotationSet();
        for (Class<? extends Annotation> cl : checker.getSupportedTypeQualifiers()) {
            flowQuals.add(annotations.fromClass(cl));
        }
        return flowQuals;
    }

}
