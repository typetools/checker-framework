package checkers.eclipse.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import checkers.eclipse.*;
import checkers.eclipse.util.*;

/**
 * Superclass of all checker actions.
 */
public abstract class RunCheckerAction implements IObjectActionDelegate {

    private final String checkerName;

    /** The current selection. */
    protected ISelection selection;

    /** true if this action is used from editor */
    protected boolean usedInEditor;

    protected RunCheckerAction(Class<?> checker) {
        this(checker.getCanonicalName());
    }

    protected RunCheckerAction(String checkerName) {
        super();
        this.checkerName = checkerName;
    }

    @Override
    public void selectionChanged(IAction action, ISelection newSelection) {
        if (!usedInEditor) {
            this.selection = newSelection;
        }
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // do nothing
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    @Override
    public void run(IAction action) {
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            work((IJavaProject) sSelection.getFirstElement());
        }
    }

    private void work(final IJavaProject project) {
        String jobName = "Running checker on " + project.getElementName();

        Job runChecker = new Job(jobName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    CheckerWorker worker = new CheckerWorker(monitor);
                    worker.work(project, checkerName);
                } catch (Throwable e) {
                    Activator.logException(e, "Analysis exception");
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };

        runChecker.setUser(true);
        runChecker.setPriority(Job.BUILD);
        runChecker.setRule(new MutexSchedulingRule());
        runChecker.schedule();
    }
}
