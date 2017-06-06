package org.checkerframework.eclipse.actions;

import org.checkerframework.eclipse.CheckerPlugin;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;

public class EnableNatureHandler extends ProjectNatureHandler {
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = getSelection(event);
        IJavaElement element = element(selection);

        try {
            IProject project = element.getJavaProject().getProject();
            IProjectDescription desc = project.getDescription();
            String[] natures = desc.getNatureIds();
            boolean hasNature = hasNature(natures);

            if (!hasNature) setNature(project, desc, natures);

        } catch (CoreException e) {
            CheckerPlugin.logException(e, e.getMessage());
        }

        return null;
    }
}
