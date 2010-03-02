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
public abstract class RunCheckerAction implements IObjectActionDelegate{

    /** The current selection. */
    protected ISelection selection;

    /** true if this action is used from editor */
    protected boolean usedInEditor;

    private final Class<?> checkerClass;

    protected RunCheckerAction(Class<?> checker){
        super();
        this.checkerClass = checker;
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart){
        // do nothing
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    @Override
    public void run(IAction action){
        if (!selection.isEmpty()){
            if (selection instanceof IStructuredSelection){
                IStructuredSelection sSelection = (IStructuredSelection) selection;

                if (selection.isEmpty()){
                    return;
                }

                work((IJavaProject) sSelection.getFirstElement());
            }
        }
    }

    private void work(final IJavaProject project){
        Job runChecker = new Job("Running checker on " + project.getElementName() + "...") {

            @Override
            protected IStatus run(IProgressMonitor monitor){
                try{
                    JSR308Worker worker = new JSR308Worker(monitor);
                    worker.work(project, checkerClass);
                }catch (CoreException e){
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

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection newSelection){
        if (!usedInEditor){
            this.selection = newSelection;
        }
    }
}
