package jsr308.actions;

import java.lang.reflect.*;
import java.util.*;

import jsr308.*;
import jsr308.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

/**
 * Remove all bug markers for the currently selected project.
 * 
 * XXX copied from FindBugs.
 */
public class ClearMarkersAction implements IObjectActionDelegate{

    /** The current selection. */
    private ISelection currentSelection;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    public final void setActivePart(final IAction action, final IWorkbenchPart targetPart){
        // noop
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public final void selectionChanged(final IAction action, final ISelection selection){
        this.currentSelection = selection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public final void run(final IAction action){
        if (!currentSelection.isEmpty()){
            if (currentSelection instanceof IStructuredSelection){
                IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
                work(structuredSelection);
            }
        }
    }

    /**
     * Clear the markers on each project in the given selection, displaying a progress monitor.
     * 
     * @param selection
     */
    private void work(final IStructuredSelection selection){
        try{
            IRunnableWithProgress r = new IRunnableWithProgress() {
                public void run(IProgressMonitor pm) throws InvocationTargetException{
                    try{
                        @SuppressWarnings("unchecked")
                        Iterator<IAdaptable> it = selection.iterator();
                        while (it.hasNext()){
                            IAdaptable adaptable = it.next();
                            Object resource = adaptable.getAdapter(IResource.class);
                            IResource res = (resource instanceof IResource ? (IResource) resource : null);
                            if (res != null){
                                pm.subTask("Clearing JSR 308 markers from " + res.getName());
                                MarkerUtil.removeMarkers(res);
                            }
                        }

                    }catch (CoreException ex){
                        Activator.getDefault().logException(ex, "CoreException on clear markers");
                        throw new InvocationTargetException(ex);

                    }catch (RuntimeException ex){
                        Activator.getDefault().logException(ex, "RuntimeException on clear markers");
                        throw ex;
                    }
                }
            };

            ProgressMonitorDialog progress = new ProgressMonitorDialog(Activator.getShell());
            progress.run(true, true, r);
        }catch (InvocationTargetException e){
            Activator.getDefault().logException(e, "InvocationTargetException on clear markers");
        }catch (InterruptedException e){
            Activator.getDefault().logException(e, "InterruptedException on clear markers");
        }
    }

}
