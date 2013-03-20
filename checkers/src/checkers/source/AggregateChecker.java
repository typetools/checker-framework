package checkers.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

/**
 * An aggregate checker that packages multiple checkers together.  The
 * resulting checker invokes the individual checkers together on the processed
 * files.
 *
 * This class delegates {@code AbstractTypeProcessor} responsibilities to each
 * of the checkers.
 *
 * Checker writers need to subclass this class and only override
 * {@link #getSupportedCheckers()} to indicate the classes of the checkers
 * to be bundled.
 */
public abstract class AggregateChecker extends SourceChecker {

    protected List<SourceChecker> checkers;

    /**
     * Returns the list of supported checkers to be run together.
     * Subclasses need to override this method.
     */
    protected abstract Collection<Class<? extends SourceChecker>> getSupportedCheckers();

    public AggregateChecker() {
        Collection<Class<? extends SourceChecker>> checkerClasses = getSupportedCheckers();

        checkers = new ArrayList<SourceChecker>(checkerClasses.size());
        for (Class<? extends SourceChecker> checkerClass : checkerClasses) {
            try {
                SourceChecker instance = checkerClass.newInstance();
                checkers.add(instance);
            } catch (Exception e) {
                System.err.println("Couldn't instantiate an instance of " + checkerClass);
            }
        }
    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);
        for (SourceChecker checker : checkers) {
            checker.setProcessingEnvironment(env);
        }
    }

    @Override
    public void initChecker() {
        super.initChecker();
        for (SourceChecker checker : checkers) {
            // Each checker should "support" all possible lint options - otherwise
            // subchecker A would complain about an lint option for subchecker B.
            checker.setSupportedLintOptions(this.getSupportedLintOptions());
            checker.initChecker();
        }
    }

    // Same functionality as the same field in SourceChecker
    int errsOnLastExit = 0;

    // AbstractTypeProcessor delegation
    @Override
    public final void typeProcess(TypeElement element, TreePath tree) {
        Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
        Log log = Log.instance(context);
        if (log.nerrors > this.errsOnLastExit) {
            // If there is a Java error, do not perform any
            // of the component type checks, but come back
            // for the next compilation unit.
            this.errsOnLastExit = log.nerrors;
            return;
        }
        for (SourceChecker checker : checkers) {
            checker.errsOnLastExit = this.errsOnLastExit;
            checker.typeProcess(element, tree);
            this.errsOnLastExit = checker.errsOnLastExit;
        }
    }

    @Override
    public void typeProcessingOver() {
        for (SourceChecker checker : checkers) {
            checker.typeProcessingOver();
        }
    }

    @Override
    public final Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        for (SourceChecker checker : checkers) {
            options.addAll(checker.getSupportedOptions());
        }
        return options;
    }

    @Override
    public final Set<String> getSupportedLintOptions() {
        Set<String> lints = new HashSet<String>();
        for (SourceChecker checker : checkers) {
            lints.addAll(checker.getSupportedLintOptions());
        }
        return lints;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        errorAbort("AggregateChecker.createSourceVisitor should never be called!");
        return null;
    }
}