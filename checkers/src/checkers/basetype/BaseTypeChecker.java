package checkers.basetype;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.igj.quals.*;
import checkers.quals.PolymorphicQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.annotation.processing.*;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * checkers.source.SourceVisitor} implementation for typical assignment and
 * pseudo-assignment checking of annotated types.  Pseudo-assignment checks
 * include method overriding checks, parameter passing, and method invocation.
 *
 * Most type-checker plug-ins will want to extend this class, instead of
 * {@link SourceChecker}.  Checkers which require annotated types but not
 * subtype checking (e.g. for testing purposes)
 * should extend {@link SourceChecker}.  Non-type checkers (e.g. for enforcing
 * coding styles) should extend {@link AbstractProcessor} (or even
 * {@link SourceChecker}) as the Checker Framework is not designed for such
 * checkers.
 *
 * Non-type checkers (e.g. checkers to enforce coding
 * styles) should extend {@link SourceChecker} or {@link AbstractProcessor}
 * directly; the Checker Framework is not designed for such checkers.
 *
 * <p>
 *
 * It is a convention that, for a type system Foo, the checker, the visitor,
 * and the annotated type factory are named as  <i>FooChecker</i>,
 * <i>FooVisitor</i>, and <i>FooAnnotatedTypeFactory</i>.  Some factory
 * methods uses this convention to construct the appriopriate classes
 * reflectively.
 *
 * <p>
 *
 * {@code BaseTypeChecker} encapsulates a group for factories for various
 * representations/classes related the type system, mainly:
 * <ul>
 *  <li> {@link QualifierHierarchy}:
 *      to represent the supported qualifiers in addition to their hierarchy,
 *      mainly, subtyping rules</li>
 *  <li> {@link TypeHierarchy}:
 *      to check subtyping rules between <b>annotated types</b> rather than qualifiers</li>
 *  <li> {@link AnnotatedTypeFactory}:
 *      to construct qualified types enriched with implicit qualifiers
 *      according to the type system rules</li>
 *  <li> {@link BaseTypeVisitor}:
 *      to visit the compiled Java files and check for violations of the type
 *      system rules</li>
 * </ul>
 *
 * <p>
 *
 * Subclasses must specify the set of type qualifiers they support either by
 * annotating the subclass with {@link TypeQualifiers} or by overriding the
 * {@link #getSupportedTypeQualifiers()} method.
 *
 * <p>
 *
 * If the specified type qualifiers are meta-annotated with {@link SubtypeOf},
 * this implementation will automatically construct the type qualifier
 * hierarchy. Otherwise, or if this behavior must be overridden, the subclass
 * may override the {@link #createQualifierHierarchy()} method.
 *
 * @see checkers.quals
 */
public abstract class BaseTypeChecker extends SourceChecker {

    /** To cache the supported type qualifiers */
    private Set<Class<? extends Annotation>> supportedQuals;

    /** To represent the supported qualifiers and their hierarchy */
    private QualifierHierarchy qualHierarchy;

    /** To compare annotated types with respect to qualHierarchy */
    private TypeHierarchy typeHierarchy;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.supportedQuals = this.createSupportedTypeQualifiers();
        this.qualHierarchy = this.createQualifierHierarchy();
        this.typeHierarchy = this.createTypeHierarchy();
    }

    // **********************************************************************
    // Factory Methods, and corresponding getters:
    // The getter methods are separated from the creation methods to simplify
    // caching for subclasses
    // **********************************************************************

    /**
     * If the checker class is annotated with {@link
     * TypeQualifiers}, return an immutable set with the same set
     * of classes as the annotation.  If the class is not so annotated,
     * return an empty set.
     *
     * Subclasses may override this method to return an immutable set
     * of their supported type qualifiers.
     *
     * @return the type qualifiers supported this processor, or an empty
     * set if none
     *
     * @see TypeQualifiers
     */
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Class<?> classType = this.getClass();
        TypeQualifiers typeQualifiersAnnotation =
            classType.getAnnotation(TypeQualifiers.class);
        if (typeQualifiersAnnotation == null)
            return Collections.emptySet();

        Set<Class<? extends Annotation>> typeQualifiers
            = new HashSet<Class<? extends Annotation>>();
        for (Class<? extends Annotation> qualifier :
                typeQualifiersAnnotation.value())
            typeQualifiers.add(qualifier);
        return Collections.unmodifiableSet(typeQualifiers);
    }

    /**
     * Returns an immutable set of the type qualifiers supported by this
     * checker.
     *
     * @see #createSupportedTypeQualifiers()
     *
     * @return the type qualifiers supported this processor, or an empty
     * set if none
     */
    public final Set<Class<? extends Annotation>> getSupportedTypeQualifiers() {
        if (supportedQuals == null)
            supportedQuals = createSupportedTypeQualifiers();
        return supportedQuals;
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * The implementation builds the type qualifier hierarchy for the
     * {@link #getSupportedTypeQualifiers()} using the
     * meta-annotations found in them.  The current implementation returns an
     * instance of {@code GraphQualifierHierarchy}.
     *
     * Subclasses may override this method to express any relationships that
     * cannot be inferred using meta-annotations (e.g. due to lack of
     * meta-annotations).
     *
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected QualifierHierarchy createQualifierHierarchy() {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

        GraphQualifierHierarchy.Factory factory=
            new GraphQualifierHierarchy.Factory();

        for (Class<? extends Annotation> typeQualifier : getSupportedTypeQualifiers()) {
            AnnotationMirror typeQualifierAnno = annoFactory.fromClass(typeQualifier);
            factory.addQualifier(typeQualifierAnno);
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                // polymorphic qualifiers don't need to declared their supertypes
                if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null)
                    continue;
                throw new AssertionError(typeQualifier + " does not specify its super qulifiers");
            }
            Class<? extends Annotation>[] superQualifiers =
                typeQualifier.getAnnotation(SubtypeOf.class).value();
            for (Class<? extends Annotation> superQualifier : superQualifiers) {
                AnnotationMirror superAnno = null;
                if (superQualifier != Unqualified.class)
                    superAnno = annoFactory.fromClass(superQualifier);
                factory.addSubtype(typeQualifierAnno, superAnno);
            }
        }
        QualifierHierarchy hierarchy = factory.build();
        if (hierarchy.getTypeQualifiers().size() < 2) {
            throw new IllegalStateException("Invalid qualifier hierarchy: hierarchy requires at least two annotations: " + hierarchy.getTypeQualifiers());
        }
        return hierarchy;
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * @see #createQualifierHierarchy()
     *
     * @return the {@link QualifierHierarchy} for this checker
     */
    public final QualifierHierarchy getQualifierHierarchy() {
        if (qualHierarchy == null)
            qualHierarchy = createQualifierHierarchy();
        return qualHierarchy;
    }

    /**
     * Creates the type subtyping checker using the current type qualifier
     * hierarchy.
     *
     * Subclasses may override this method to specify new type checking
     * rules beyond the typical java subtyping rules.
     *
     * @return  the type relations class to check type subtyping
     */
    protected TypeHierarchy createTypeHierarchy() {
        return new TypeHierarchy(getQualifierHierarchy());
    }

    /**
     * Returns the appropriate visitor that type checks the compilation unit
     * according to the type system rules.
     *
     * This implementation uses the checker naming convention to create the
     * appropriate visitor.  If no visitor is found, it returns an instance of
     * {@link BaseTypeVisitor}.  It reflectively invokes the constructor that
     * accepts this checker and the compilation unit tree (in that order)
     * as arguments.
     *
     * Subclasses have to override this method to create the appropriate
     * visitor if they do not follow the checker naming convention.
     *
     * @param root  the compilation unit currently being visited
     * @return the type-checking visitor
     */
    @Override
    protected BaseTypeVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = this.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                checkerClass.getName().replace("Checker", "Visitor");
            BaseTypeVisitor<?, ?> result = invokeConstructorFor(classToLoad,
                    new Class<?>[] { this.getClass(), CompilationUnitTree.class },
                    new Object[] { this, root });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If a visitor couldn't be loaded reflectively, return the default.
        return new BaseTypeVisitor<Void, Void>(this, root);
    }

    /**
     * Constructs an instance of the appropriate type factory for the
     * implemented type system.
     *
     * The default implementation uses the checker naming convention to create
     * the appropriate type factory.  If no factory is found, it returns
     * {@link BasicAnnotatedTypeFactory}.  It reflectively invokes the
     * constructor that accepts this checker and compilation unit tree
     * (in that order) as arguments.
     *
     * Subclasses have to override this method to create the appropriate
     * visitor if they do not follow the checker naming convention.
     *
     * @param root  the currently visited compilation unit
     * @return the appropriate type factory
     */
    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {

        // Try to reflectively load the type factory.
        Class<?> checkerClass = this.getClass();
        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                checkerClass.getName().replace("Checker", "AnnotatedTypeFactory");

            AnnotatedTypeFactory result = invokeConstructorFor(classToLoad,
                    new Class<?>[] { this.getClass(), CompilationUnitTree.class },
                    new Object[] { this, root });
            if (result != null) return result;
            checkerClass = checkerClass.getSuperclass();
        }
        return new BasicAnnotatedTypeFactory<BaseTypeChecker>(this, root);
    }

    // **********************************************************************
    // Type Relationship queries
    // **********************************************************************

    /**
     * Tests whether one annotated type is a subtype of another, with
     * respect to the annotations on these types.
     *
     * Subclasses may wish to ignore annotations that are not related to the
     * type qualifiers they check.
     *
     * This implementation follows the subtype rules specified in
     * {@link TypeHierarchy}.  Its behavior is undefined for any annotations
     * not specified by either {@link TypeQualifiers} or the result of
     * {@link #getSupportedTypeQualifiers()}.
     * @param sub the child type
     * @param sup the parent type
     *
     * @return true iff {@code sub} is a subtype of {@code sup}
     */
    // Should other classes simply depend on TypeHierarchy directly?
    public boolean isSubtype(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
        return typeHierarchy.isSubtype(sub, sup);
    }

    /**
     * Tests that the qualifiers present on the useType are valid qualifiers,
     * given the qualifiers on the declaration of the type, declarationType.
     *
     * <p>
     *
     * The check is shallow, as it does not descend into generic or array
     * types (i.e. only performing the validity check on the raw type or
     * outmost array dimension).  {@link BaseTypeVisitor#validateTypeOf(Tree)}
     * would call this for each type argument or array dimention separately.
     *
     * <p>
     *
     * For instance, in the IGJ type system, a {@code @Mutable} is an invalid
     * qualifier for {@link String}, as {@link String} is declared as
     * {@code @Immutable String}.
     *
     * <p>
     *
     * In most cases, {@code useType} simply needs to be a subtype of
     * {@code declarationType}, but there are exceptions.  In IGJ, a variable may be
     * declared {@code @ReadOnly String}, even though {@link String} is
     * {@code @Immutable String};  {@link ReadOnly} is not a subtype of
     * {@link Immutable}.
     *
     * @param declarationType  the type of the class (TypeElement)
     * @param useType   the use of the class (instance type)
     * @return  if the useType is a valid use of elemType
     */
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return isSubtype(useType.getErased(), declarationType.getErased());
    }

    /**
     * Tests whether the variable accessed is an assignable variable or not,
     * given the current scope
     *
     * @param varType   the annotated variable type
     * @param variable  tree used to access the variable
     * @return  true iff variable is assignable in the current scope
     */
    public boolean isAssignable(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror receiverType, Tree variable) {
        return true;
    }


    // **********************************************************************
    // Misc. methods
    // **********************************************************************

    /**
     * Specify 'flow' and 'cast' as supported lint options for all Type checkers
     */
    @Override
    public Set<String> getSupportedLintOptions() {
        Set<String> lintSet = new HashSet<String>(super.getSupportedLintOptions());
        lintSet.add("flow");
        lintSet.add("cast");
        return lintSet;
    }

    /**
     * Invokes the constructor belonging to the class
     * named by {@code name} having the given parameter types on the given
     * arguments. Returns {@code null} if the class cannot be found, or the
     * constructor does not exist or cannot be invoked on the given arguments,
     *
     * @param <T> the type to which the constructor belongs
     * @param name the name of the class to which the constructor belongs
     * @param paramTypes the types of the constructor's parameters
     * @param args the arguments on which to invoke the constructor
     * @return the result of the constructor invocation on {@code args}, or
     *         null if the constructor does not exist or could not be invoked
     */
    @SuppressWarnings("unchecked")
    private static <T> T invokeConstructorFor(String name,
            Class<?>[] paramTypes, Object[] args) {

        // Load the class.
        Class<T> cls = null;
        try {
            cls = (Class<T>) Class.forName(name);
        } catch (Exception e) {
            // no class is found, simply return null
            return null;
        }

        assert cls != null;

        // Invoke the constructor.
        try {
            Constructor<T> ctor = cls.getConstructor(paramTypes);
            return ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }

        // On failure, return null.
        return null;
    }
}
