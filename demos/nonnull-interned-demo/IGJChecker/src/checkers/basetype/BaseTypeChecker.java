package checkers.basetype;

import java.util.Properties;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.metaquals.QualifierRoot;
import checkers.metaquals.SubtypeOf;
import checkers.metaquals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.util.GraphAnnotationRelations;
import checkers.util.TypeRelations;

import javax.lang.model.*;
import javax.lang.model.element.AnnotationMirror;
import javax.annotation.processing.*;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * checkers.source.SourceVisitor} implementation for typical assignment and
 * pseudo-assignment checking of annotated types. Most typechecker plug-ins will
 * want to extend this class.
 * 
 * <p>
 * 
 * Subclasses must implement the {@link BaseTypeChecker#isSubtype} method to
 * define the relationships between various annotated and unannotated types in
 * their custom type systems. Note that {@link BaseTypeChecker#isSubtype} must
 * operate on type annotations and not the underlying types (which are checked
 * by the compiler's built-in type checker).
 * 
 * @see BaseTypeChecker#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"*"})
public abstract class BaseTypeChecker extends SourceChecker {

    protected AnnotationRelations annoRelations;
    private TypeRelations typeRelations;
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.annoRelations = this.createAnnotationRelations();
    }
    
    @Override
    protected Properties getMessages() {
        Properties properties = new Properties();
        properties.put("assignability.invalid", 
                "Cannot (re-)assign %s through the reference: %s");
        properties.put("receiver.invalid",
                "incompatible types.\nfound   : %s\nrequired: %s");
        properties.put("type.incompatible", 
                "incompatible types.\nfound   : %s\nrequired: %s");
        properties.put("type.invalid",
                "%s may not be applied to the type \"%s\"");
        properties.put("override.return.invalid", 
                "%s in %s cannot override %s in %s; " + 
                "attempting to use an incompatible return type" + 
                "\nfound   : %s\nrequired: %s");
        properties.put("override.param.invalid", 
                "%s in %s cannot override %s in %s; " +
                "attempting to use an incompatible parameter type" +
                "\nfound   : %s\nrequired: %s");
        properties.put("override.receiver.invalid", 
                "%s in %s cannot override %s in %s; " +
                "attempting to use an incompatible receiver type" +
                "\nfound   : %s\nrequired: %s");
        properties.put("method.invocation.invalid", 
                "call to %s not allowed on the given receiver." + 
                "\nfound   : %s\nrequired: %s");
        properties.put("generic.argument.invalid", 
                "invalid type argument;" +
                "\nfound   : %s\nrequired: %s");
        return properties;
    }

    @Override
    protected BaseTypeVisitor<?, ?> getSourceVisitor(CompilationUnitTree root) {
        return new BaseTypeVisitor<Void, Void>(this, root);
    }

    /**
     * Tests whether the one annotated type is a subtype of another, with
     * respect to the annotations on these types. Subclasses may wish to ignore
     * annotations that are not related to the type qualifiers they check.
     * 
     * @param sup the parent type
     * @param sub the child type
     * @return true iff {@code sub} is a subtype of {@code sup}
     */
    public boolean isSubtype(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        return typeRelations.isSubtype(sup, sub);
    }

    /**
     * Tests whether the one annotated type is a valid use of another, with respect
     * to the annotations on these types, without descending into generic or
     * array types (i.e. only performing the validity check on the raw type or 
     * outmost array dimension).
     * 
     * In most cases, the usetype simply needs to be a suptype of elemType.
     * 
     * @param elemType  the type of the class (TypeElement)
     * @param useType   the use of the class (instance type)
     * @return  if the useType is a valid use of elemType
     */
    public boolean isValidUse(AnnotatedTypeMirror elemType, AnnotatedTypeMirror useType) {
        return isSubtype(elemType.getErased(), useType.getErased());
    }

    /**
     * Tests whether the variable accessed is an assignable variable or not, given
     * the current scope
     * 
     * @param varType   the annotated variable type
     * @param variable  the tree access tree
     * @return  true iff variable is assignable in the current scope
     */
    public boolean isAssignable(AnnotatedTypeMirror varType, Tree variable) {
        return true;
    }

    /**
     * @return the supported type qualifiers for this checkers
     */
    public Class<?>[] getSupportedTypeQualifiers() {
        Class<?> classType = this.getClass();
        TypeQualifiers typeQualifiersAnnotation =
            classType.getAnnotation(TypeQualifiers.class);
        if (typeQualifiersAnnotation == null)
            return null;
        return typeQualifiersAnnotation.value();
    }

    /**
     * A helper method that constructs the type qualifiers hierarchy tree.
     * 
     * The current implementation uses reflection and meta-annotation to
     * build the annotation relation tree
     * 
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected GraphAnnotationRelations createAnnotationRelations() {
        AnnotationFactory annoFactory = new AnnotationFactory(env);

        Class<?>[] typeQualifiers = getSupportedTypeQualifiers();
        if (typeQualifiers == null)
            return null;
        
        GraphAnnotationRelations annotationRelations = new GraphAnnotationRelations();
        for (Class<?> typeQualifier : typeQualifiers) {
            String typeQualifierName = typeQualifier.getCanonicalName();
            AnnotationMirror typeQualifierAnno = annoFactory.fromName(typeQualifierName);
            if (typeQualifier.getAnnotation(QualifierRoot.class) != null) {
                annotationRelations.setRootAnnotation(typeQualifierAnno);
                break;
            }
        }
        
        // Build the actual graph
        for (Class<?> typeQualifier : typeQualifiers) {
            String typeQualifierName = typeQualifier.getCanonicalName();
            AnnotationMirror typeQualifierAnno = annoFactory.fromName(typeQualifierName);
            // Set subtype relations
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                annotationRelations.addSubtype(typeQualifierAnno, annotationRelations.getRootAnnotation());
                continue;
            }
            for (Class<?> superAnno : typeQualifier.getAnnotation(SubtypeOf.class).value()) {
                AnnotationMirror superAnnotation = null;
                if (superAnno != Void.class) {
                    String superAnnotationName = superAnno.getCanonicalName();
                    superAnnotation = annoFactory.fromName(superAnnotationName);
                }
                annotationRelations.addSubtype(typeQualifierAnno, superAnnotation);
            }
        }
        return annotationRelations;
    }
    
    /**
     * Update the checker with the currently visited root and
     * get the appropriate factory
     * 
     * Subclasses should consider overriding {@link createFactory}, rather
     * than this.
     * 
     * @param env   the processing environment
     * @param root  the currently visited compilation unit
     * @return  the appropriate type factory 
     */
    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        AnnotatedTypeFactory factory = createFactory(env, root);
        this.typeRelations = createTypeRelations(factory);
        return factory;
    }

    /**
     * Creates a factory without updating the currently visited root
     * 
     * @param env   the processing environment
     * @param root  the currently visited compilation unit
     * @return  the appropriate type factory 
     */
    public AnnotatedTypeFactory createFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        return new AnnotatedTypeFactory(env, root);
    }
    
    /**
     * Creates the type subtyping checker using the current type qualifier 
     * hierarchy.
     * 
     * @param factory   the annotated type factory used currently
     * @return  the type relations class to check type subtyping
     */
    protected TypeRelations createTypeRelations(AnnotatedTypeFactory factory) {
        return new TypeRelations(factory, env, annoRelations);
    }
}

