package org.checkerframework.common.basetype;

import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ErrorReporter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * org.checkerframework.framework.source.SourceVisitor} implementation that
 * type-checks assignments, pseudo-assignments such as parameter passing
 * and method invocation, and method overriding.
 * <p>
 *
 * Most type-checker annotation processor should extend this class, instead of
 * {@link SourceChecker}.
 * Checkers that require annotated types but not subtype checking (e.g. for
 * testing purposes) should extend {@link SourceChecker}.
 * Non-type checkers (e.g. checkers to enforce coding styles) can extend
 * {@link SourceChecker} or {@link AbstractTypeProcessor}; the Checker
 * Framework is not designed for such checkers.
 * <p>
 *
 * It is a convention that, for a type system Foo, the checker, the visitor,
 * and the annotated type factory are named as  <i>FooChecker</i>,
 * <i>FooVisitor</i>, and <i>FooAnnotatedTypeFactory</i>.  Some factory
 * methods use this convention to construct the appropriate classes
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
 * Subclasses must specify the set of type qualifiers they support. See
 * {@link AnnotatedTypeFactory#createSupportedTypeQualifiers()}.
 *
 * <p>
 *
 * If the specified type qualifiers are meta-annotated with {@link SubtypeOf},
 * this implementation will automatically construct the type qualifier
 * hierarchy. Otherwise, or if this behavior must be overridden, the subclass
 * may override the {@link BaseAnnotatedTypeFactory#createQualifierHierarchy()} method.
 *
 * @see org.checkerframework.framework.qual
 *
 * @checker_framework.manual #writing-compiler-interface The checker class
 */
public abstract class BaseTypeChecker extends SourceChecker implements BaseTypeContext {

    @Override
    public void initChecker() {
        // initialize all checkers and share options as necessary
        for (BaseTypeChecker checker : getSubcheckers()) {
            checker.initChecker();
            // We need to add all options that are activated for the set of subcheckers to
            // the individual checkers.
            checker.addOptions(super.getOptions());
            // Each checker should "support" all possible lint options - otherwise
            // subchecker A would complain about a lint option for subchecker B.
            checker.setSupportedLintOptions(this.getSupportedLintOptions());
        }

        super.initChecker();
    }

     /*
      * The full list of subcheckers that need to be run prior to this one,
      * in the order they need to be run in.  This list will only be
      * non-empty for the one checker that runs all other subcheckers.  Do
      * not read this field directly. Instead, retrieve it via {@link
      * #getSubcheckers}.
      * <p>
      *
      * If the list still null when {@link #getSubcheckers} is called, then
      * getSubcheckers() will call {@link #instantiateSubcheckers}.
      * However, if the current object was itself instantiated by a prior
      * call to instantiateSubcheckers, this field will have been
      * initialized to an empty list before getSubcheckers() is called,
      * thereby ensuring that this list is non-empty only for one checker.
      */
     private List<BaseTypeChecker> subcheckers = null;

     /*
      * The list of subcheckers that are direct dependencies of this checker.
      * This list will be non-empty for any checker that has at least one subchecker.
      *
      * Does not need to be initialized to null or an empty list because it is always
      * initialized via calls to instantiateSubcheckers.
      */
     private List<BaseTypeChecker> immediateSubcheckers;

     /**
      * Returns the set of subchecker classes this checker depends on. Returns an empty set if this checker does not depend on any others.
      * Subclasses need to override this method if they have dependencies.
      *
      * Each subclass of BaseTypeChecker must declare all dependencies it relies on if there are any.
      * Each subchecker of this checker is in turn free to change its own dependencies.
      * It's OK for various checkers to declare a dependency on the same subchecker, since
      * the BaseTypeChecker will ensure that each subchecker is instantiated only once.
      *
      * WARNING: Circular dependencies are not supported. We do not check for their absence. Make sure no circular dependencies
      * are created when overriding this method.
      *
      * This method is protected so it can be overridden, but it is only intended to be called internally by the BaseTypeChecker.
      * Please override this method but do not call it from classes other than BaseTypeChecker. Subclasses that override
      * this method should call super and added dependencies so that checkers required for reflection resolution are included
      * if reflection resolution is requested.
      *
      * The BaseTypeChecker will not modify the list returned by this method.
      */
     protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        if (shouldResolveReflection()) {
            return new LinkedHashSet<Class<? extends BaseTypeChecker>>(
                    Collections.singleton(MethodValChecker.class));
        }
         return new LinkedHashSet<Class<? extends BaseTypeChecker>>();
     }

    /**
     * Returns whether or not reflection should be resolved
     */
    public boolean shouldResolveReflection() {
        // Because this method is indirectly called by getSubcheckers and
        // this.getOptions or this.hasOption
        // also call getSubcheckers, super.getOptions is called here.
        return super.getOptions().containsKey("resolveReflection");

    }

     /**
     * Returns the appropriate visitor that type-checks the compilation unit
     * according to the type system rules.
     * <p>
     * This implementation uses the checker naming convention to create the
     * appropriate visitor.  If no visitor is found, it returns an instance of
     * {@link BaseTypeVisitor}.  It reflectively invokes the constructor that
     * accepts this checker and the compilation unit tree (in that order)
     * as arguments.
     * <p>
     * Subclasses have to override this method to create the appropriate
     * visitor if they do not follow the checker naming convention.
     *
     * @return the type-checking visitor
     */
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        // Try to reflectively load the visitor.
        Class<?> checkerClass = this.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                    checkerClass.getName().replace("Checker", "Visitor")
                            .replace("Subchecker", "Visitor");
            BaseTypeVisitor<?> result = invokeConstructorFor(classToLoad,
                    new Class<?>[]{BaseTypeChecker.class},
                    new Object[]{this});
            if (result != null) {
                return result;
            }
            checkerClass = checkerClass.getSuperclass();
        }

        // If a visitor couldn't be loaded reflectively, return the default.
        return new BaseTypeVisitor<BaseAnnotatedTypeFactory>(this);
    }


    // **********************************************************************
    // Misc. methods
    // **********************************************************************

    /**
     * Specify supported lint options for all type-checkers.
     */
    @Override
    public Set<String> getSupportedLintOptions() {
        Set<String> lintSet = new HashSet<String>(super.getSupportedLintOptions());
        lintSet.add("cast");
        lintSet.add("cast:redundant");
        lintSet.add("cast:unsafe");

         for (BaseTypeChecker checker : getSubcheckers()) {
             lintSet.addAll(checker.getSupportedLintOptions());
         }

        return Collections.unmodifiableSet(lintSet);
    }

    /**
     * Invokes the constructor belonging to the class
     * named by {@code name} having the given parameter types on the given
     * arguments. Returns {@code null} if the class cannot be found, or the
     * constructor does not exist or cannot be invoked on the given arguments.
     *
     * @param <T>        the type to which the constructor belongs
     * @param name       the name of the class to which the constructor belongs
     * @param paramTypes the types of the constructor's parameters
     * @param args       the arguments on which to invoke the constructor
     * @return the result of the constructor invocation on {@code args}, or
     * null if the constructor does not exist or could not be invoked
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
                    CheckerError ce = (CheckerError) err;
                    if (ce.userError) {
                        // Don't add another stack frame, just show the message.
                        throw ce;
                    } else {
                        msg = err.getMessage();
                    }
                } else {
                    msg = err.toString();
                }
                ErrorReporter.errorAbort("InvocationTargetException when invoking constructor for class " + name +
                        "; Underlying cause: " + msg, t);
            } else {
                ErrorReporter.errorAbort("Unexpected " + t.getClass().getSimpleName() + " for " +
                                "class " + name +
                                " when invoking the constructor; parameter types: " + Arrays.toString(paramTypes),
                        // + " and args: " + Arrays.toString(args),
                        t);
            }
            return null; // dead code
        }
    }


    @Override
    public BaseTypeContext getContext() {
        return this;
    }

    @Override
    public BaseTypeChecker getChecker() {
        return this;
    }

    @Override
    public BaseTypeVisitor<?> getVisitor() {
        return (BaseTypeVisitor<?>) super.getVisitor();
    }

    @Override
    public GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory() {
        return getVisitor().getTypeFactory();
    }

    @Override
    public AnnotationProvider getAnnotationProvider() {
        return getTypeFactory();
    }

    /**
     * Returns the requested subchecker.
     * A checker of a given class can only be run once, so this returns the
     * only such checker, or null if none was found.
     * The caller must know the exact checker class to request.
     *
     * @param checkerClass The class of the subchecker.
     * @return The requested subchecker or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseTypeChecker> T getSubchecker(Class<T> checkerClass) {
        for (BaseTypeChecker checker : immediateSubcheckers) {
            if (checker.getClass().equals(checkerClass)) {
                return (T) checker;
            }
        }

        return null;
    }

    /**
     * Returns the type factory used by a subchecker.
     * Returns null if no matching subchecker was found or if the
     * type factory is null.
     * The caller must know the exact checker class to request.
     *
     * @param checkerClass The class of the subchecker.
     * @return The type factory of the requested subchecker or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker> T getTypeFactoryOfSubchecker(Class<U> checkerClass) {
        BaseTypeChecker checker = getSubchecker(checkerClass);
        if (checker != null) {
            return (T) checker.getTypeFactory();
        }

        return null;
    }

    /*
     * Performs a depth first search for all checkers this checker depends on.
     * The depth first search ensures that the collection has the correct order the checkers need to be run in.
     *
     * Modifies the alreadyInitializedSubcheckerMap map by adding all recursively newly instantiated subcheckers' class objects and instances.
     * A LinkedHashMap is used because, unlike HashMap, it preserves the order in which entries were inserted.
     *
     * Returns the unmodifiable list of immediate subcheckers of this checker.
     */
    private List<BaseTypeChecker> instantiateSubcheckers(LinkedHashMap<Class<? extends BaseTypeChecker>, BaseTypeChecker> alreadyInitializedSubcheckerMap) {
        LinkedHashSet<Class<? extends BaseTypeChecker>> classesOfImmediateSubcheckers = getImmediateSubcheckerClasses();
        ArrayList<BaseTypeChecker> immediateSubcheckers = new ArrayList<BaseTypeChecker>();

        for (Class<? extends BaseTypeChecker> subcheckerClass : classesOfImmediateSubcheckers) {
            BaseTypeChecker subchecker = alreadyInitializedSubcheckerMap.get(subcheckerClass);
            if (subchecker != null) {
                // Add the already initialized subchecker to the list of immediate subcheckers so that this checker can refer to it.
                immediateSubcheckers.add(subchecker);
                continue;
            }

            try {
                BaseTypeChecker instance = subcheckerClass.newInstance();
                instance.setProcessingEnvironment(this.processingEnv);
                instance.subcheckers = Collections.unmodifiableList(new ArrayList<BaseTypeChecker>()); // Prevent the new checker from storing non-immediate subcheckers
                immediateSubcheckers.add(instance);
                instance.immediateSubcheckers = instance.instantiateSubcheckers(alreadyInitializedSubcheckerMap);
                instance.setParentChecker(this);
                alreadyInitializedSubcheckerMap.put(subcheckerClass, instance);
            } catch (Exception e) {
                ErrorReporter.errorAbort("Could not create an instance of " + subcheckerClass);
            }
        }

        return Collections.unmodifiableList(immediateSubcheckers);
    }

    /*
     * Get the list of all subcheckers (if any). via the instantiateSubcheckers method.
     * This list is only non-empty for the one checker that runs all other subcheckers.
     * These are recursively instantiated via instantiateSubcheckers the first time
     * the method is called if subcheckers is null.
     * Assumes all checkers run on the same thread.
     */
    private List<BaseTypeChecker> getSubcheckers() {
        if (subcheckers == null) {
            // Instantiate the checkers this one depends on, if any.
            LinkedHashMap<Class<? extends BaseTypeChecker>, BaseTypeChecker> checkerMap = new LinkedHashMap<Class<? extends BaseTypeChecker>, BaseTypeChecker>();

            immediateSubcheckers = instantiateSubcheckers(checkerMap);

            subcheckers = Collections.unmodifiableList(new ArrayList<BaseTypeChecker>(checkerMap.values()));
        }

        return subcheckers;
    }

    // AbstractTypeProcessor delegation
    @Override
    public void typeProcess(TypeElement element, TreePath tree) {

        // If Java has issued errors, don't run any checkers on this compilation unit.
        // If a sub checker issued errors, run the next checker on this compilation unit.

        // Log.nerrors counts the number of Java and checker errors have been issued.
        // super.typeProcess does not typeProcess if log.nerrors > errorsOnLastExit

        // In order to run the next checker on this compilation unit even if the previous
        // issued errors, the next checker's errsOnLastExit needs to include all errors
        // issued by previous checkers.

        // To prevent any checkers from running if a Java error was issued for this compilation unit,
        // errsOnLastExit should not include any Java errors.
        Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
        Log log = Log.instance(context);
        // Start with this.errsOnLastExit which will account for errors seen by
        // by a previous checker run in an aggregate checker.
        int nerrorsOfAllPreviousCheckers = this.errsOnLastExit;
        for (BaseTypeChecker checker : getSubcheckers()) {
            checker.errsOnLastExit = nerrorsOfAllPreviousCheckers;
            int errorsBeforeTypeChecking = log.nerrors;

            checker.typeProcess(element, tree);

            int errorsAfterTypeChecking = log.nerrors;
            nerrorsOfAllPreviousCheckers += errorsAfterTypeChecking - errorsBeforeTypeChecking;
        }
        this.errsOnLastExit = nerrorsOfAllPreviousCheckers;
        super.typeProcess(element, tree);
    }

    @Override
    public void typeProcessingOver() {
        for (BaseTypeChecker checker : getSubcheckers()) {
            checker.typeProcessingOver();
        }

        super.typeProcessingOver();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        options.addAll(super.getSupportedOptions());

        for (BaseTypeChecker checker : getSubcheckers()) {
            options.addAll(checker.getSupportedOptions());
        }

        options.addAll(expandCFOptions(Arrays.asList(this.getClass()), options.toArray(new String[0])));

        return Collections.<String>unmodifiableSet(options);
    }

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = new HashMap<String, String>(super.getOptions());

        for (BaseTypeChecker checker : getSubcheckers()) {
            options.putAll(checker.getOptions());
        }

        return options;
    }

    @Override
    protected Object processArg(Object arg) {
        if (arg instanceof Collection) {
            List<Object> newList = new LinkedList<>();
            for (Object o : ((Collection<?>)arg)) {
                newList.add(processArg(o));
            }
            return newList;
        } else if (arg instanceof AnnotationMirror) {
            return getTypeFactory().getAnnotationFormatter().formatAnnotationMirror((AnnotationMirror)arg);
        } else {
            return super.processArg(arg);
        }
    }

    protected boolean shouldAddShutdownHook() {
        if (super.shouldAddShutdownHook() ||
                getTypeFactory().getCFGVisualizer() != null) {
            return true;
        }
        for (BaseTypeChecker checker : getSubcheckers()) {
            if (checker.getTypeFactory().getCFGVisualizer() != null) {
                return true;
            }
        }
        return false;
    }

    protected void shutdownHook() {
        super.shutdownHook();

        CFGVisualizer<?, ?, ?> viz = getTypeFactory().getCFGVisualizer();
        if (viz != null) {
            viz.shutdown();
        }

        for (BaseTypeChecker checker : getSubcheckers()) {
            viz = checker.getTypeFactory().getCFGVisualizer();
            if (viz != null) {
                viz.shutdown();
            }
        }
    }
}
