package org.checkerframework.common.basetype;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.TreePathCacher;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.UserError;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * org.checkerframework.framework.source.SourceVisitor} implementation that type-checks assignments,
 * pseudo-assignments such as parameter passing and method invocation, and method overriding.
 *
 * <p>Most type-checker annotation processor should extend this class, instead of {@link
 * SourceChecker}. Checkers that require annotated types but not subtype checking (e.g. for testing
 * purposes) should extend {@link SourceChecker}. Non-type checkers (e.g. checkers to enforce coding
 * styles) can extend {@link SourceChecker} or {@link AbstractTypeProcessor}; the Checker Framework
 * is not designed for such checkers.
 *
 * <p>It is a convention that, for a type system Foo, the checker, the visitor, and the annotated
 * type factory are named as <i>FooChecker</i>, <i>FooVisitor</i>, and
 * <i>FooAnnotatedTypeFactory</i>. Some factory methods use this convention to construct the
 * appropriate classes reflectively.
 *
 * <p>{@code BaseTypeChecker} encapsulates a group for factories for various representations/classes
 * related the type system, mainly:
 *
 * <ul>
 *   <li>{@link QualifierHierarchy}: to represent the supported qualifiers in addition to their
 *       hierarchy, mainly, subtyping rules
 *   <li>{@link TypeHierarchy}: to check subtyping rules between <b>annotated types</b> rather than
 *       qualifiers
 *   <li>{@link AnnotatedTypeFactory}: to construct qualified types enriched with implicit
 *       qualifiers according to the type system rules
 *   <li>{@link BaseTypeVisitor}: to visit the compiled Java files and check for violations of the
 *       type system rules
 * </ul>
 *
 * <p>Subclasses must specify the set of type qualifiers they support. See {@link
 * AnnotatedTypeFactory#createSupportedTypeQualifiers()}.
 *
 * <p>If the specified type qualifiers are meta-annotated with {@link SubtypeOf}, this
 * implementation will automatically construct the type qualifier hierarchy. Otherwise, or if this
 * behavior must be overridden, the subclass may override the {@link
 * BaseAnnotatedTypeFactory#createQualifierHierarchy()} method.
 *
 * @checker_framework.manual #creating-compiler-interface The checker class
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

    /**
     * The full list of subcheckers that need to be run prior to this one, in the order they need to
     * be run in. This list will only be non-empty for the one checker that runs all other
     * subcheckers. Do not read this field directly. Instead, retrieve it via {@link
     * #getSubcheckers}.
     *
     * <p>If the list still null when {@link #getSubcheckers} is called, then getSubcheckers() will
     * call {@link #instantiateSubcheckers}. However, if the current object was itself instantiated
     * by a prior call to instantiateSubcheckers, this field will have been initialized to an empty
     * list before getSubcheckers() is called, thereby ensuring that this list is non-empty only for
     * one checker.
     */
    private List<BaseTypeChecker> subcheckers = null;

    /**
     * The list of subcheckers that are direct dependencies of this checker. This list will be
     * non-empty for any checker that has at least one subchecker.
     *
     * <p>Does not need to be initialized to null or an empty list because it is always initialized
     * via calls to instantiateSubcheckers.
     */
    private List<BaseTypeChecker> immediateSubcheckers;

    /** Supported options for this checker. */
    private Set<String> supportedOptions;

    /**
     * TreePathCacher to share between instances. Initialized either in instantiateSubcheckers or in
     * getTreePathCacher.
     */
    private TreePathCacher treePathCacher;

    @Override
    protected void setRoot(CompilationUnitTree newRoot) {
        super.setRoot(newRoot);
        if (parentChecker == null) {
            // Only clear the path cache if this is the main checker.
            treePathCacher.clear();
        }
    }

    /**
     * Returns the set of subchecker classes on which this checker depends. Returns an empty set if
     * this checker does not depend on any others.
     *
     * <p>Subclasses should override this method to specify subcheckers. If they do so, they should
     * call the super implementation of this method and add dependencies to the returned set so that
     * checkers required for reflection resolution are included if reflection resolution is
     * requested.
     *
     * <p>Each subchecker of this checker may also depend on other checkers. If this checker and one
     * of its subcheckers both depend on a third checker, that checker will only be instantiated
     * once.
     *
     * <p>Though each checker is run on a whole compilation unit before the next checker is run,
     * error and warning messages are collected and sorted based on the location in the source file
     * before being printed. (See {@link #printMessage(Diagnostic.Kind, String, Tree,
     * CompilationUnitTree)}.)
     *
     * <p>WARNING: Circular dependencies are not supported nor do checkers verify that their
     * dependencies are not circular. Make sure no circular dependencies are created when overriding
     * this method. (In other words, if checker A depends on checker B, checker B cannot depend on
     * checker A.)
     *
     * <p>This method is protected so it can be overridden, but it should only be called internally
     * by the BaseTypeChecker.
     *
     * <p>The BaseTypeChecker will not modify the list returned by this method.
     */
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        if (shouldResolveReflection()) {
            return new LinkedHashSet<>(Collections.singleton(MethodValChecker.class));
        }
        return new LinkedHashSet<>();
    }

    /** Returns whether or not reflection should be resolved. */
    public boolean shouldResolveReflection() {
        // Because this method is indirectly called by getSubcheckers and
        // this.getOptions or this.hasOption
        // also call getSubcheckers, super.getOptions is called here.
        return super.getOptions().containsKey("resolveReflection");
    }

    /**
     * Returns the appropriate visitor that type-checks the compilation unit according to the type
     * system rules.
     *
     * <p>This implementation uses the checker naming convention to create the appropriate visitor.
     * If no visitor is found, it returns an instance of {@link BaseTypeVisitor}. It reflectively
     * invokes the constructor that accepts this checker and the compilation unit tree (in that
     * order) as arguments.
     *
     * <p>Subclasses have to override this method to create the appropriate visitor if they do not
     * follow the checker naming convention.
     *
     * @return the type-checking visitor
     */
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        // Try to reflectively load the visitor.
        Class<?> checkerClass = this.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                    checkerClass
                            .getName()
                            .replace("Checker", "Visitor")
                            .replace("Subchecker", "Visitor");
            BaseTypeVisitor<?> result =
                    invokeConstructorFor(
                            classToLoad,
                            new Class<?>[] {BaseTypeChecker.class},
                            new Object[] {this});
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

    /** Specify supported lint options for all type-checkers. */
    @Override
    public Set<String> getSupportedLintOptions() {
        Set<String> lintSet = new HashSet<>(super.getSupportedLintOptions());
        lintSet.add("cast");
        lintSet.add("cast:redundant");
        lintSet.add("cast:unsafe");

        for (BaseTypeChecker checker : getSubcheckers()) {
            lintSet.addAll(checker.getSupportedLintOptions());
        }

        return Collections.unmodifiableSet(lintSet);
    }

    /**
     * Invokes the constructor belonging to the class named by {@code name} having the given
     * parameter types on the given arguments. Returns {@code null} if the class cannot be found.
     * Otherwise, throws an exception if there is trouble with the constructor invocation.
     *
     * @param <T> the type to which the constructor belongs
     * @param name the name of the class to which the constructor belongs
     * @param paramTypes the types of the constructor's parameters
     * @param args the arguments on which to invoke the constructor
     * @return the result of the constructor invocation on {@code args}, or null if the class does
     *     not exist
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"}) // Intentional abuse
    public static <T> T invokeConstructorFor(String name, Class<?>[] paramTypes, Object[] args) {

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
                if (err instanceof UserError) {
                    UserError ue = (UserError) err;
                    // Don't add another stack frame, just show the message.
                    throw ue;
                }
                throw new BugInCF(
                        "InvocationTargetException when invoking constructor for class "
                                + name
                                + "; Underlying cause: "
                                + err.getMessage(),
                        t);
            } else {
                throw new BugInCF(
                        "Unexpected "
                                + t.getClass().getSimpleName()
                                + " for "
                                + "class "
                                + name
                                + " when invoking the constructor; parameter types: "
                                + Arrays.toString(paramTypes),
                        // + " and args: " + Arrays.toString(args),
                        t);
            }
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
        BaseTypeVisitor<?> visitor = getVisitor();
        // Avoid NPE if this method is called during initialization.
        if (visitor == null) {
            return null;
        }
        return visitor.getTypeFactory();
    }

    @Override
    public AnnotationProvider getAnnotationProvider() {
        return getTypeFactory();
    }

    /**
     * Returns the requested subchecker. A checker of a given class can only be run once, so this
     * returns the only such checker, or null if none was found. The caller must know the exact
     * checker class to request.
     *
     * @param checkerClass the class of the subchecker
     * @return the requested subchecker or null if not found
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
     * Returns the type factory used by a subchecker. Returns null if no matching subchecker was
     * found or if the type factory is null. The caller must know the exact checker class to
     * request.
     *
     * @param checkerClass the class of the subchecker
     * @return the type factory of the requested subchecker or null if not found
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"}) // Intentional abuse
    public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker>
            T getTypeFactoryOfSubchecker(Class<U> checkerClass) {
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
    private List<BaseTypeChecker> instantiateSubcheckers(
            LinkedHashMap<Class<? extends BaseTypeChecker>, BaseTypeChecker>
                    alreadyInitializedSubcheckerMap) {
        LinkedHashSet<Class<? extends BaseTypeChecker>> classesOfImmediateSubcheckers =
                getImmediateSubcheckerClasses();
        if (classesOfImmediateSubcheckers.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<BaseTypeChecker> immediateSubcheckers =
                new ArrayList<>(classesOfImmediateSubcheckers.size());

        for (Class<? extends BaseTypeChecker> subcheckerClass : classesOfImmediateSubcheckers) {
            BaseTypeChecker subchecker = alreadyInitializedSubcheckerMap.get(subcheckerClass);
            if (subchecker != null) {
                // Add the already initialized subchecker to the list of immediate subcheckers so
                // that this checker can refer to it.
                immediateSubcheckers.add(subchecker);
                continue;
            }

            BaseTypeChecker instance;
            try {
                instance = subcheckerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new BugInCF("Could not create an instance of " + subcheckerClass);
            }

            instance.setProcessingEnvironment(this.processingEnv);
            instance.treePathCacher = this.getTreePathCacher();
            // Prevent the new checker from storing non-immediate subcheckers
            instance.subcheckers = Collections.emptyList();
            immediateSubcheckers.add(instance);
            instance.immediateSubcheckers =
                    instance.instantiateSubcheckers(alreadyInitializedSubcheckerMap);
            instance.setParentChecker(this);
            alreadyInitializedSubcheckerMap.put(subcheckerClass, instance);
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
            LinkedHashMap<Class<? extends BaseTypeChecker>, BaseTypeChecker> checkerMap =
                    new LinkedHashMap<>();

            immediateSubcheckers = instantiateSubcheckers(checkerMap);

            subcheckers = Collections.unmodifiableList(new ArrayList<>(checkerMap.values()));
        }

        return subcheckers;
    }

    /** Get the shared TreePathCacher instance. */
    public TreePathCacher getTreePathCacher() {
        if (treePathCacher == null) {
            // In case it wasn't already set in instantiateSubcheckers.
            treePathCacher = new TreePathCacher();
        }
        return treePathCacher;
    }

    /**
     * Sort by position at which the error will be printed, then by the order in which the checkers
     * run, then by kind of message, and finally by the message string.
     */
    private final Comparator<CheckerMessage> checkerMessageComparator =
            new Comparator<CheckerMessage>() {
                @Override
                public int compare(CheckerMessage o1, CheckerMessage o2) {
                    int byPos = InternalUtils.compareDiagnosticPosition(o1.source, o2.source);
                    if (byPos != 0) {
                        return byPos;
                    }

                    // Sort by order in which the checkers are run. (All the subcheckers in
                    // followed by the checker.)
                    int o1Index = BaseTypeChecker.this.getSubcheckers().indexOf(o1.checker);
                    int o2Index = BaseTypeChecker.this.getSubcheckers().indexOf(o2.checker);
                    if (o1Index != o2Index) {
                        if (o1Index == -1) {
                            o1Index = BaseTypeChecker.this.getSubcheckers().size();
                        }
                        if (o2Index == -1) {
                            o2Index = BaseTypeChecker.this.getSubcheckers().size();
                        }
                        return Integer.compare(o1Index, o2Index);
                    }

                    int kind = o1.kind.compareTo(o2.kind);
                    if (kind != 0) {
                        return kind;
                    }

                    return o1.message.compareTo(o2.message);
                }
            };

    // AbstractTypeProcessor delegation
    @Override
    public void typeProcess(TypeElement element, TreePath tree) {
        if (!getSubcheckers().isEmpty()) {
            messageStore = new TreeSet<>(checkerMessageComparator);
        }

        // Errors (or other messages) issued via
        // SourceChecker#message(Diagnostic.Kind, Object, String, Object...)
        // are stored in messageStore until all checkers have processed this compilation unit.
        // All other messages are printed immediately.  This includes errors issued because the
        // checker threw an exception.

        // In order to run the next checker on this compilation unit even if the previous
        // issued errors, the next checker's errsOnLastExit needs to include all errors
        // issued by previous checkers.

        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        Log log = Log.instance(context);

        int nerrorsOfAllPreviousCheckers = this.errsOnLastExit;
        for (BaseTypeChecker subchecker : getSubcheckers()) {
            subchecker.errsOnLastExit = nerrorsOfAllPreviousCheckers;
            subchecker.messageStore = messageStore;
            int errorsBeforeTypeChecking = log.nerrors;

            subchecker.typeProcess(element, tree);

            int errorsAfterTypeChecking = log.nerrors;
            nerrorsOfAllPreviousCheckers += errorsAfterTypeChecking - errorsBeforeTypeChecking;
        }

        this.errsOnLastExit = nerrorsOfAllPreviousCheckers;
        super.typeProcess(element, tree);

        if (!getSubcheckers().isEmpty()) {
            printCollectedMessages(tree.getCompilationUnit());
            // Update errsOnLastExit to reflect the errors issued.
            this.errsOnLastExit = log.nerrors;
        }
    }

    /**
     * Issues a warning about any {@code @SuppressWarnings} that isn't used by this checker, but
     * contains a key that would suppress a warning from this checker.
     *
     * <p>Collects needed warning suppressions for all subcheckers.
     */
    @Override
    protected void warnUnneededSuppressions() {
        if (parentChecker != null || !hasOption("warnUnneededSuppressions")) {
            return;
        }
        Set<Element> elementsSuppress = new HashSet<>(this.elementsWithSuppressedWarnings);
        this.elementsWithSuppressedWarnings.clear();
        Set<String> checkerKeys = new HashSet<>(getSuppressWarningsKeys());
        Set<String> errorKeys = new HashSet<>(messages.stringPropertyNames());
        for (BaseTypeChecker subChecker : subcheckers) {
            elementsSuppress.addAll(subChecker.elementsWithSuppressedWarnings);
            subChecker.elementsWithSuppressedWarnings.clear();
            checkerKeys.addAll(subChecker.getSuppressWarningsKeys());
            errorKeys.addAll(subChecker.messages.stringPropertyNames());
            subChecker.getVisitor().treesWithSuppressWarnings.clear();
        }
        warnUnneedSuppressions(elementsSuppress, checkerKeys, errorKeys);

        getVisitor().treesWithSuppressWarnings.clear();
    }

    /**
     * Stores all messages issued by this checker and its subcheckers for the current compilation
     * unit. The messages are printed after all checkers have processed the current compilation
     * unit. If this checker has no subcheckers and is not a subchecker for any other checker, then
     * messageStore is null and messages will be printed as they are issued by this checker.
     */
    private TreeSet<CheckerMessage> messageStore = null;

    /**
     * If this is a compound checker or a subchecker of a compound checker, then the message is
     * stored until all messages from all checkers for the compilation unit are issued.
     *
     * <p>Otherwise, it prints the message.
     */
    @Override
    protected void printMessage(
            Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root) {
        assert this.currentRoot == root;
        if (messageStore == null) {
            super.printMessage(kind, message, source, root);
        } else {
            CheckerMessage checkerMessage = new CheckerMessage(kind, message, source, this);
            messageStore.add(checkerMessage);
        }
    }

    /**
     * Prints error messages for this checker and all subcheckers such that the errors are ordered
     * by line and column number and then by checker. (See checkerMessageComparator for more precise
     * order.)
     *
     * @param unit current compilation unit
     */
    private void printCollectedMessages(CompilationUnitTree unit) {
        if (messageStore != null) {
            for (CheckerMessage msg : messageStore) {
                super.printMessage(msg.kind, msg.message, msg.source, unit);
            }
        }
    }

    /** Represents a message (e.g., an error message) issued by a checker. */
    private static class CheckerMessage {
        final Diagnostic.Kind kind;
        final String message;
        final Tree source;

        /**
         * The checker that issued this message. The compound checker that depends on this checker
         * uses this to sort the messages.
         */
        final BaseTypeChecker checker;

        private CheckerMessage(
                Diagnostic.Kind kind, String message, Tree source, BaseTypeChecker checker) {
            this.kind = kind;
            this.message = message;
            this.source = source;
            this.checker = checker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CheckerMessage that = (CheckerMessage) o;

            if (kind != that.kind) {
                return false;
            }
            if (!message.equals(that.message)) {
                return false;
            }
            if (source != that.source) {
                return false;
            }
            return checker == that.checker;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, message, source, checker);
        }

        @Override
        public String toString() {
            return "CheckerMessage{"
                    + "kind="
                    + kind
                    + ", checker="
                    + checker.getClass().getSimpleName()
                    + ", message='"
                    + message
                    + '\''
                    + ", source="
                    + source
                    + '}';
        }
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
        if (supportedOptions == null) {
            Set<String> options = new HashSet<>();
            options.addAll(super.getSupportedOptions());

            for (BaseTypeChecker checker : getSubcheckers()) {
                options.addAll(checker.getSupportedOptions());
            }

            options.addAll(
                    expandCFOptions(
                            Arrays.asList(this.getClass()), options.toArray(new String[0])));

            supportedOptions = Collections.unmodifiableSet(options);
        }
        return supportedOptions;
    }

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = new HashMap<>(super.getOptions());

        for (BaseTypeChecker checker : getSubcheckers()) {
            options.putAll(checker.getOptions());
        }

        return options;
    }

    @Override
    protected Object processArg(Object arg) {
        if (arg instanceof Collection) {
            Collection<?> carg = (Collection<?>) arg;
            List<Object> newList = new ArrayList<>(carg.size());
            for (Object o : carg) {
                newList.add(processArg(o));
            }
            return newList;
        } else if (arg instanceof AnnotationMirror && getTypeFactory() != null) {
            return getTypeFactory()
                    .getAnnotationFormatter()
                    .formatAnnotationMirror((AnnotationMirror) arg);
        } else {
            return super.processArg(arg);
        }
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        if (super.shouldAddShutdownHook() || getTypeFactory().getCFGVisualizer() != null) {
            return true;
        }
        for (BaseTypeChecker checker : getSubcheckers()) {
            if (checker.getTypeFactory().getCFGVisualizer() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
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
