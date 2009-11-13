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
import checkers.flow.Flow;
import checkers.quals.DefaultLocation;
import checkers.quals.DefaultQualifier;
import checkers.quals.ImplicitFor;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.AnnotationUtils;
import checkers.util.InternalUtils;
import checkers.util.QualifierDefaults;
import checkers.util.QualifierPolymorphism;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit annotations
 * via {@link ImplicitFor}, and user-specified defaults via {@link DefaultQualifier}
 *
 * @see Flow
 */
public class BasicAnnotatedTypeFactory<Checker extends BaseTypeChecker> extends AnnotatedTypeFactory {

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
    protected final Flow flow;

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
        this.treeAnnotator = createTreeAnnotator(checker);
        this.typeAnnotator = createTypeAnnotator(checker);
        this.useFlow = useFlow;
        this.poly = new QualifierPolymorphism(checker, this);
        Set<AnnotationMirror> flowQuals = createFlowQualifiers(checker);
        this.flow = useFlow ? createFlow(checker, root, flowQuals) : null;
        this.defaults = new QualifierDefaults(this, this.annotations);
        for (Class<? extends Annotation> qual : checker.getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.setAbsoluteDefaults(this.annotations.fromClass(qual),
                        Collections.singleton(DefaultLocation.ALL));
                break;
            }
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
        return new TreeAnnotator(checker);
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
        return new Flow(checker, root, flowQuals, this);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED)
            for (AnnotatedTypeMirror supertype : supertypes)
                // FIXME: Recursive initialization for defaults fields
                if (defaults != null)
                    defaults.annotate(((DeclaredType)supertype.getUnderlyingType()).asElement(), supertype);
    }

    // Indicate whether flow has performed the analysis or not
    boolean scanned = false;
    boolean finishedScanning = false;
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        assert root != null : "root needs to be set when used on trees";
        if (useFlow && !scanned) {
            // Perform the flow analysis at the first invocation of
            // annotateImplicit.  note that flow may call .getAnnotatedType
            // so scanned is set to true before flow.scan
            scanned = true;
            // Apply flow-sensitive qualifier inference.
            flow.scan(root, null);
            finishedScanning = true;
        }
        treeAnnotator.visit(tree, type);
        if (useFlow) {
            final AnnotationMirror inferred = flow.test(tree);
            if (inferred != null) {
                type.clearAnnotations();
                type.addAnnotation(inferred);
            }
        }
        // TODO: This is quite ugly
        if (!useFlow || finishedScanning
                || tree.getKind() == Tree.Kind.METHOD
                || tree.getKind() == Tree.Kind.CLASS
                || tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            Element elt = InternalUtils.symbol(tree);
            typeAnnotator.visit(type, elt != null ? elt.getKind() : ElementKind.OTHER);
            defaults.annotate(tree, type);
        }
    }

    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typeAnnotator.visit(type, elt.getKind());
        defaults.annotate(elt, type);
    }

    @Override
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        AnnotatedExecutableType method = super.methodFromUse(tree);
        poly.annotate(tree, method);
        poly.annotate(method.getElement(), method);
        return method;
    }

    // **********************************************************************
    // Helper method
    // **********************************************************************

    /**
     * Returns the set of annotations to be inferred in flow analysis
     */
    protected Set<AnnotationMirror> createFlowQualifiers(Checker checker) {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

        Set<AnnotationMirror> flowQuals = new HashSet<AnnotationMirror>();
        for (Class<? extends Annotation> cl : checker.getSupportedTypeQualifiers()) {
            flowQuals.add(annoFactory.fromClass(cl));
        }
        return flowQuals;
    }

}
