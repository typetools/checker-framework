package checkers.eclipse.actions;

import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import checkers.eclipse.util.*;

/**
 * Superclass of all checker actions.
 */
public abstract class RunCheckerAction implements IObjectActionDelegate {

    private final String checkerName;

    /** The current selection. */
    protected IStructuredSelection selection;

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
        if (!usedInEditor && (newSelection instanceof IStructuredSelection)) {
            this.selection = (IStructuredSelection) newSelection;
        } else
            this.selection = null;
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // do nothing
    }

    private IJavaProject project() {
        if (selection != null && !selection.isEmpty())
            return (IJavaProject) selection.getFirstElement();
        return null;
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    @Override
    public void run(IAction action) {
        IJavaProject project = project();
        if (project != null) {
            Job checkerJob = new CheckerWorker(project, checkerName);
            checkerJob.setUser(true);
            checkerJob.setPriority(Job.BUILD);
            checkerJob.setRule(new MutexSchedulingRule());
            checkerJob.schedule();
        }
    }
}
