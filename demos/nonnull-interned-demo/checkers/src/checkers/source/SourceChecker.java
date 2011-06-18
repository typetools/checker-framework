package checkers.source;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic;

import checkers.quals.*;
import checkers.types.*;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * An abstract annotation processor designed for implementing a
 * source-file checker for a JSR 308 type-checker plugin. It provides an
 * interface to {@code javac}'s annotation processing API, routines for error
 * reporting via the JSR 199 compiler API, and an implementation for using a
 * {@link SourceVisitor} to perform the type-checking.
 *
 * <p>
 *
 * Subclasses should use the {@link SupportedAnnotationTypes} annotation to
 * indicate which annotations that the processor handles.
 *
 * <p>
 *
 * Subclasses must implement the following methods:
 *
 * <ul>
 *  <li>{@link SourceChecker#getMessages} (for type-qualifier specific error messages)
 *  <li>{@link SourceChecker#getSourceVisitor} (for a custom {@link SourceVisitor})
 *  <li>{@link SourceChecker#getFactory} (for a custom {@link AnnotatedTypeFactory})
 *  <li>{@link SourceChecker#getSuppressWarningsKey} (for honoring
 *      @SuppressWarnings annotations)
 * </ul>
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public abstract class SourceChecker extends AbstractProcessor {

    /** Provides access to compiler helpers/internals. */
    protected ProcessingEnvironment env;

    /** Used for scanning a source tree. */
    protected SourceVisitor visitor;

    /** Maps error keys to localized/custom error messages. */
    protected Properties messages;

    /** Used to report error messages and warnings via the compiler. */
    protected Messager messager;

    /** Used as a helper for the {@link SourceVisitor}. */
    protected Trees trees;

    /** The source tree that's being scanned. */
    protected CompilationUnitTree currentRoot;

    /** A regular expression for classes that should be skipped. */
    protected final Pattern skipPattern =
        Pattern.compile(System.getProperty("checkers.skipClasses", getDefaultSkipPattern()));

    /**
     * @return the default pattern for the checkers.skipClasses property
     */
    protected String getDefaultSkipPattern() {
        return "java.*";
    }

    /**
     * @return the {@link ProcessingEnvironment} that was supplied to this
     *         checker
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.env;
    }

    /**
     * @param env the {@link ProcessingEnvironment} for the factory
     * @param root the AST root for the factory
     * @return an {@link AnnotatedTypeFactory} for use by typecheckers
     */
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new AnnotatedTypeFactory(env, root);
    }

    /**
     * Provides the {@link SourceVisitor} that the checker should use to scan
     * input source trees.
     *
     * @param root
     *            the AST root
     * @return a {@link SourceVisitor} to use to scan source trees
     */
    protected abstract SourceVisitor getSourceVisitor(CompilationUnitTree root);

    /**
     * Provides a mapping of error keys to localized or custom error messages.
     *
     * @param fileName the name of the messages file
     * @throws IOException
     *             if the messages couldn't be loaded
     * @return a {@link Properties} that maps error keys to error message text
     */
    protected abstract Properties getMessages(String fileName) throws IOException;

    /**
     * {@inheritDoc}
     *
     * @see javax.annotation.processing.AbstractProcessor#init(javax.annotation.processing.ProcessingEnvironment)
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.env = processingEnv;

        // Grab the Trees and Messager instances now; other utilities
        // (like Types and Elements) can be retrieved by subclasses.
        @Nullable Trees trees = Trees.instance(processingEnv);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.messager = processingEnv.getMessager();

        try {
            this.messages = getMessages("checker.properties");
        } catch (IOException e) {
            System.err.println("messages not found: " + e.getMessage());
            this.messages = new Properties();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set,
     *      javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getRootElements();
        assert elements != null;

        if (elements.isEmpty())
            return false;

        for (Element e : elements) {

            @Nullable TreePath p = trees.getPath(e);
            if (p != null) {
                @Nullable CompilationUnitTree cu = p.getCompilationUnit();
                assert cu != null; /*nninvariant*/
                currentRoot = cu;

                visitor = getSourceVisitor(currentRoot);
                ((SourceVisitor<?, ?>)visitor).scan(p, null);
            }
        }

        return true;
    }

    /**
     * Prints a message (error, warning, note, etc.) via JSR-269.
     *
     * @param kind
     *            the type of message to print
     * @param source
     *            the object from which to obtain source position information
     * @param msgKey
     *            the message key to print
     * @param args
     *            arguments for interpolation in the string corresponding to the
     *            given message key
     * @see Diagnostic
     * @throws IllegalArgumentException
     *             if {@code source} is neither a {@link Tree} nor an
     *             {@link Element}
     */
    protected void message(Diagnostic.Kind kind, Object source, Object msgKey,
            Object... args) {

        assert messages != null : "null messages";

        for (Object arg : args) {
            arg = (arg == null) ? null :
                messages.getProperty(arg.toString(), arg.toString());
        }
        
        if (kind == Diagnostic.Kind.NOTE) {
            System.err.println("(NOTE) " + String.format(msgKey.toString(), args));
            return;
        }

        String fmtString;
        if (this.env.getOptions() != null /*nnbug*/
                && this.env.getOptions().containsKey("nomsgtext"))
            fmtString = "(" + msgKey.toString() + ")";
        else
            fmtString = (@NonNull String)messages.getProperty(msgKey.toString(), "("
                    + msgKey.toString() + ")");
        String messageText = String.format(fmtString, args);

        if (source instanceof Element)
            messager.printMessage(kind, messageText, (Element) source);
        else if (source instanceof Tree)
            messager.printMessageFromTree(kind, messageText, (Tree) source,
                    currentRoot);
        else
            throw new IllegalArgumentException("invalid position source: "
                    + source.getClass().getName());
    }
    
    /**
     * Determines whether one of the annotations in the given set of {@link
     * AnnotationMirror}s is a {@link SuppressWarnings} annotation with the
     * SuppressWarnings key corresponding to this checker. 
     *
     * @param annos the annotations to search
     * @return true if one of {@code annos} is a {@link SuppressWarnings}
     *         annotation with the key returned by {@link
     *         SourceChecker#getSuppressWarningsKey}
     */
    private boolean checkSuppressWarnings(List<? extends AnnotationMirror> annos) {

        AnnotationFactory af = new AnnotationFactory(this.env);
        Types types = env.getTypeUtils();
        @Nullable String swKey = this.getSuppressWarningsKey();

        // For all the method's annotations, check for a @SuppressWarnings
        // annotation. If one is found, check its values for this checker's
        // SuppressWarnings key.
        for (AnnotationMirror am : annos) {
            AnnotationData ad = af.createAnnotation(am);
            
            { // Skip annotations that aren't @SuppressWarnings.
                @Nullable String annoName = AnnotationUtils.annotationName(ad);
                if (!("java.lang.SuppressWarnings".equals(annoName)))
                    continue;
            }

            // Parse the value of the @SuppressWarnings annotation. Return
            // true if it contains the SuppressWarnings key.
            @Nullable Set<String> vals = AnnotationUtils.parseStringArrayValue(ad);
            if (vals != null && vals.contains(swKey)) /*nnbug*/
                return true;
        }

        return false;
    }
    
    /**
     * Determines whether the warnings pertaining to a given tree should be
     * suppressed (namely, if its containing method has a @SuppressWarnings
     * annotation for which one of the values is the key provided by the {@link
     * SourceChecker#getSuppressWarningsKey} method).
     * 
     * @param tree the tree that might be a source of a warning
     * @return true if no warning should be emitted for the given tree because
     *         it is contained by a method with an appropriately-valued
     *         @SuppressWarnings annotation; false otherwise
     */
    private boolean shouldSuppressWarnings(Tree tree) {

        // Don't suppress warnings if there's no key.
        @Nullable String swKey = this.getSuppressWarningsKey();
        if (swKey == null)
            return false;
        
        @Nullable Element thisElt = InternalUtils.symbol(tree);
        if (thisElt != null && checkSuppressWarnings(thisElt.getAnnotationMirrors()))
            return true;
        
        // Get the method tree, and ultimately, its element. 
        @Nullable TreePath path = trees.getPath(this.currentRoot, tree);
        if (path == null) /*nnbug*/
            return false;

        @Nullable MethodTree method = TreeUtils.enclosingMethod(path); 
        if (method == null)
            return false;
        
        @Nullable Element elt = InternalUtils.symbol(method);
        if (elt == null)
            return false;

        if (checkSuppressWarnings(elt.getAnnotationMirrors()))
            return true;

        return false;
    }

    private boolean shouldSuppressWarnings(Element elt) {
        // TODO: for enclosing methods

        if (elt == null)
            return false;

        return checkSuppressWarnings(elt.getAnnotationMirrors());
    }
    
    /**
     * Reports a result. By default, it prints it to the screen via the
     * compiler's internal messeger if the result is non-success; otherwise,
     * the method returns with no side-effects.
     *
     * @param r
     *            the result to report
     * @param src
     *            the position object associated with the result
     */
    public void report(final Result r, final Object src) {

        // TODO: SuppressWarnings checking for Elements
        if (src instanceof Tree && shouldSuppressWarnings((Tree)src))
            return;
        if (src instanceof Element && shouldSuppressWarnings((Element)src))
            return;
        
        if (r.isSuccess())
            return;

        for (Result.DiagMessage msg : r.getDiagMessages()) {
            if (r.isFailure())
                this.message(Diagnostic.Kind.ERROR, src, msg.getMessageKey(), msg.getArgs());
            else if (r.isWarning())
                this.message(Diagnostic.Kind.MANDATORY_WARNING, src, msg.getMessageKey(), msg.getArgs());
            else
                this.message(Diagnostic.Kind.NOTE, src, msg.getMessageKey(), msg.getArgs());
        }
    }
    
    /**
     * Determines whether checking against members of the class with the given
     * name should be performed. This method will return true only when the
     * given class name matches the regular expression specified by the {@code
     * checkers.skipClasses} property.
     *
     * @param className the fully qualified name of the class to check
     * @return true if the members of the class named {@code className} should
     *         not be checked against
     */
    public boolean shouldSkip(String className) {
        Matcher m = skipPattern.matcher(className);
        return m.matches();
    }

    /**
     * Determines the value of the lint option with the given name. Lint
     * options are provided in a similar manner to standard javac lint options:
     * as a list of option names, or option names prepended with a "-",
     * following the "-Alint=" javac switch. If the option name is present
     * without a "-", it returns true; if the option name is present but
     * prepended with a "-" or it is not present at all, this method returns
     * false.
     *
     * @throws IllegalArgumentException if the option name is not recognized
     *         via the {@link SupportedLintOptions} annotation or the {@link
     *         SourceChecker#getSupportedLintOptions} method
     * @param name the name of the lint option to check for
     * @return true if the lint option was given, false if it was not given or
     * was given prepended with a "-"
     *
     * @see SourceChecker#getLintOption(String,boolean)
     */
    public boolean getLintOption(String name) {
        return getLintOption(name, false);
    }

    /**
     * Determines the value of the lint option with the given name. Lint
     * options are provided in a similar manner to standard javac lint options:
     * as a list of option names, or option names prepended with a "-",
     * following the "-Alint=" javac switch. If the option name is present
     * without a "-", this method returns true; if the option name is present
     * but prepended with a "-", it returns false. If the option is not present
     * at all, it returns the value of the {@code def} argument (the default
     * option value).
     *
     * @throws IllegalArgumentException if the option name is not recognized
     *         via the {@link SupportedLintOptions} annotation or the {@link
     *         SourceChecker#getSupportedLintOptions} method
     * @param name the name of the lint option to check for
     * @param def the default option value, returned if the option was not given
     * @return true if the lint option was given, false if it was given
     *         prepended with a "-", or {@code def} if it was not given at all
     *
     * @see SourceChecker#getLintOption(String)
     */
    public boolean getLintOption(String name, boolean def) {

        if (!this.getSupportedLintOptions().contains(name))
            throw new IllegalArgumentException("illegal lint option: " + name);

        Map<String, @Nullable String> options = this.env.getOptions();

        @Nullable String lintString = this.env.getOptions().get("lint");
        if (lintString == null)
            return def;

        Set<String> lintOptions = new HashSet<String>();
        for (String s : lintString.split(","))
            lintOptions.add(s);

        if (lintOptions.contains(String.format("-%s", name)))
            return false;
        else if (lintOptions.contains(name))
            return true;
        else return def;
    }

    /*
     * Force "-Alint" as a recognized processor option for all subtypes of
     * SourceChecker.
     *
     * [This method is provided here, rather than via the @SupportedOptions
     * annotation, so that it may be inherited by subclasses.]
     */
    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        options.add("lint");
        options.add("nomsgtext");
        for (String s : super.getSupportedOptions()) /*nnbug*/
            if (s != null)
                options.add(s);
        return Collections.<@NonNull String>unmodifiableSet(options);
    }

    /*
     * Forces all type checkers to run even on unannotated code
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.<@NonNull String>singleton("*");
    }
    
    /**
     * Returns the lint options recognized by this checker. Lint options are
     * those which can be checked for via {@link SourceChecker#getLintOption}.
     *
     * @return an unmodifiable {@link Set} of the lint options recognized by
     *         this checker
     */
    public Set<String> getSupportedLintOptions() {
        @SuppressWarnings("nonnull") // FIXME: checker bug
        @Nullable SupportedLintOptions sl =
            this.getClass().getAnnotation(SupportedLintOptions.class);

        if (sl == null)
            return Collections.<@NonNull String>emptySet();

        @Nullable String[@Nullable] slValue = sl.value();
        assert slValue != null; /*nninvariant*/

        String[@Nullable] lintArray = slValue;
        Set<String> lintSet = new HashSet<String>(lintArray.length);
        for (String s : lintArray)
            lintSet.add(s);
        return Collections.<@NonNull String>unmodifiableSet(lintSet);

    }

    /**
     * @return the String key that a checker honors for suppressing warnings
     *         and errors that it issues
     */
    protected @Nullable String getSuppressWarningsKey() {
        return null; 
    }
}
