package checkers.types;

import java.lang.annotation.Annotation;
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
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.AnnotationUtils;
import checkers.util.InternalUtils;
import checkers.util.Pair;
import checkers.util.QualifierDefaults;
import checkers.util.QualifierPolymorphism;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;

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
        super(checker, checker.getQualifierHierarchy(), root);
        this.checker = checker;
        this.treeAnnotator = createTreeAnnotator(checker);
        this.typeAnnotator = createTypeAnnotator(checker);
        this.useFlow = useFlow;

        this.poly = createQualifierPolymorphism();
        this.defaults = createQualifierDefaults();
        boolean foundDefault = false;
        // TODO: should look for a default qualifier per qualifier hierarchy.
        for (Class<? extends Annotation> qual : checker.getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.addAbsoluteDefault(AnnotationUtils.fromClass(elements, qual),
                        DefaultLocation.ALL);
                foundDefault = true;
            }
        }

        AnnotationMirror unqualified = AnnotationUtils.fromClass(elements, Unqualified.class);
        if (!foundDefault && this.isSupportedQualifier(unqualified)) {
            defaults.addAbsoluteDefault(unqualified,
                    DefaultLocation.ALL);
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
        if (flow != null) {
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
        return new TypeAnnotator(checker, this);
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

    /**
     * Create {@link QualifierDefaults} which handles user specified defaults
     * @return the QualifierDefaults class
     */
    protected QualifierDefaults createQualifierDefaults() {
        return new QualifierDefaults(elements, this);
    }

    /**
     * Creates {@link QualifierPolymorphism} which supports
     * QualifierPolymorphism mechanism
     * @return the QualifierPolymorphism class
     */
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new QualifierPolymorphism(checker, this);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED) {
            for (AnnotatedTypeMirror supertype : supertypes) {
                Element elt = ((DeclaredType) supertype.getUnderlyingType()).asElement();
                annotateImplicit(elt, supertype);
            }
        }
    }

    @Override
    public void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        assert root != null : "root needs to be set when used on trees";
        treeAnnotator.visit(tree, type);
        Element elt = InternalUtils.symbol(tree);
        typeAnnotator.visit(type, elt != null ? elt.getKind() : ElementKind.OTHER);
        defaults.annotate(tree, type);

        if (useFlow) {
            if (flow == null) {
                SourceChecker.errorAbort("Type factory \"" + this.getClass().getSimpleName() + "\" did not call postInit() to initialize flow inference!");
            }
            final Set<AnnotationMirror> inferred = flow.test(tree);
            if (inferred != null) {
                for (AnnotationMirror inf : inferred) {
                    AnnotationMirror present = type.getAnnotationInHierarchy(inf);
                    if (present != null) {
                        if (this.getQualifierHierarchy().isSubtype(inf, present)) {
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
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
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

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.constructorFromUse(tree);
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
            flowQuals.add(AnnotationUtils.fromClass(elements, cl));
        }
        return flowQuals;
    }

}
