package checkers.basetype;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.sun.source.tree.CompilationUnitTree;

/*>>>
import checkers.igj.quals.*;
*/
import checkers.quals.PolymorphicQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.*;
import checkers.util.*;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
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
 * should extend {@link SourceChecker}.
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
 * methods uses this convention to construct the appropriate classes
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

    /** To cache the supported type qualifiers. */
    private Set<Class<? extends Annotation>> supportedQuals;

    /** To represent the supported qualifiers and their hierarchy. */
    private QualifierHierarchy qualHierarchy;

    /** To compare annotated types with respect to qualHierarchy. */
    private TypeHierarchy typeHierarchy;

    @Override
    public void initChecker() {
        super.initChecker();
        this.supportedQuals = this.createSupportedTypeQualifiers();
        this.qualHierarchy = this.getQualifierHierarchy();
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

        Set<Class<? extends Annotation>> typeQualifiers = new HashSet<Class<? extends Annotation>>();
        for (Class<? extends Annotation> qualifier : typeQualifiersAnnotation.value()) {
            typeQualifiers.add(qualifier);
        }
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

    /** Factory method to easily change what Factory is used to
     * create a QualifierHierarchy.
     */
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    /** Factory method to easily change what QualifierHierarchy is
     * created.
     * Needs to be public only because the GraphFactory must be able to call this method.
     * No external use of this method is necessary.
     */
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, null);
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
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = this.createQualifierHierarchyFactory();
        Elements elements = processingEnv.getElementUtils();

        for (Class<? extends Annotation> typeQualifier : getSupportedTypeQualifiers()) {
            AnnotationMirror typeQualifierAnno = AnnotationUtils.fromClass(elements, typeQualifier);
            assert typeQualifierAnno!=null : "Loading annotation \"" + typeQualifier + "\" failed!";
            factory.addQualifier(typeQualifierAnno);
            // Polymorphic qualifiers can't declare their supertypes.
            // An error is raised if one is present.
            if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null) {
                if (typeQualifier.getAnnotation(SubtypeOf.class) != null) {
                    // This is currently not supported. At some point we might add
                    // polymorphic qualifiers with upper and lower bounds.
                    errorAbort("BaseTypeChecker: " + typeQualifier + " is polymorphic and specifies super qualifiers. " +
                        "Remove the @checkers.quals.SubtypeOf or @checkers.quals.PolymorphicQualifier annotation from it.");
                }
                continue;
            }
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                errorAbort("BaseTypeChecker: " + typeQualifier + " does not specify its super qualifiers. " +
                    "Add an @checkers.quals.SubtypeOf annotation to it.");
            }
            Class<? extends Annotation>[] superQualifiers =
                typeQualifier.getAnnotation(SubtypeOf.class).value();
            for (Class<? extends Annotation> superQualifier : superQualifiers) {
                AnnotationMirror superAnno = null;
                superAnno = AnnotationUtils.fromClass(elements, superQualifier);
                factory.addSubtype(typeQualifierAnno, superAnno);
            }
        }

        QualifierHierarchy hierarchy = factory.build();
        if (hierarchy.getTypeQualifiers().size() < 1) {
            errorAbort("BaseTypeChecker: invalid qualifier hierarchy: hierarchy requires at least one annotation: " + hierarchy.getTypeQualifiers());
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
        return new TypeHierarchy(this, getQualifierHierarchy());
    }

    public final TypeHierarchy getTypeHierarchy() {
        if (typeHierarchy == null)
            typeHierarchy = createTypeHierarchy();
        return typeHierarchy;
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
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = this.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                checkerClass.getName().replace("Checker", "Visitor")
                .replace("Subchecker", "Visitor");
            BaseTypeVisitor<?> result = invokeConstructorFor(classToLoad,
                    new Class<?>[] { checkerClass, CompilationUnitTree.class },
                    new Object[] { this, root });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If a visitor couldn't be loaded reflectively, return the default.
        return new BaseTypeVisitor<BaseTypeChecker>(this, root);
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
                checkerClass.getName().replace("Checker", "AnnotatedTypeFactory")
                .replace("Subchecker", "AnnotatedTypeFactory");

            AnnotatedTypeFactory result = invokeConstructorFor(classToLoad,
                    new Class<?>[] { checkerClass, CompilationUnitTree.class },
                    new Object[] { this, root });
            if (result != null) return result;
            checkerClass = checkerClass.getSuperclass();
        }
        return new BasicAnnotatedTypeFactory<BaseTypeChecker>(this, root);
    }


    // **********************************************************************
    // Misc. methods
    // **********************************************************************

    /**
     * Specify 'flow' and 'cast' as supported lint options for all Type checkers.
     *
     * WMD: the above comment talks about 'flow', but I don't find a use of it as
     * a lint option. I added a new key 'flow:inferFromAsserts'.
     * Maybe 'flow' should be used in BasicAnnotatedTypeFactory with/instead of
     * FLOW_BY_DEFAULT.
     */
    @Override
    public Set<String> getSupportedLintOptions() {
        Set<String> lintSet = new HashSet<String>(super.getSupportedLintOptions());
        lintSet.add("cast");
        lintSet.add("cast:redundant");
        lintSet.add("cast:unsafe");
        lintSet.add("flow:inferFromAsserts");
        // Temporary option to make array subtyping invariant,
        // which will be the new default soon.
        lintSet.add("arrays:invariant");
        // Temporary option to make casts stricter, in particular when casting
        // to an array or generic type. This will be the new default soon.
        lintSet.add("cast:strict");

        return Collections.unmodifiableSet(lintSet);
    }

    /**
     * Invokes the constructor belonging to the class
     * named by {@code name} having the given parameter types on the given
     * arguments. Returns {@code null} if the class cannot be found, or the
     * constructor does not exist or cannot be invoked on the given arguments.
     *
     * @param <T> the type to which the constructor belongs
     * @param name the name of the class to which the constructor belongs
     * @param paramTypes the types of the constructor's parameters
     * @param args the arguments on which to invoke the constructor
     * @return the result of the constructor invocation on {@code args}, or
     *         null if the constructor does not exist or could not be invoked
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructorFor(String name,
            Class<?>[] paramTypes, Object[] args) {

        // Load the class.
        Class<T> cls = null;
        try {
            cls = (Class<T>) Class.forName(name);
        } catch (Exception e) {
            // no class is found, simply return null
            return null;
        }

        assert cls != null : "reflectively loading " + name + " failed";

        // Invoke the constructor.
        try {
            Constructor<T> ctor = cls.getConstructor(paramTypes);
            return ctor.newInstance(args);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                Throwable err = t.getCause();
                String msg;
                if (err instanceof CheckerError) {
                    msg = err.getMessage();
                } else {
                    msg = err.toString();
                }
                SourceChecker.errorAbort("InvocationTargetException when invoking constructor for class " + name +
                        "; Underlying cause: " + msg, t);
            } else {
                SourceChecker.errorAbort("Unexpected " + t.getClass().getSimpleName() + " for " +
                        "class " + name +
                        " when invoking the constructor; parameter types: " + Arrays.toString(paramTypes),
                        // + " and args: " + Arrays.toString(args),
                        t);
            }
            return null; // dead code
        }
    }

}
