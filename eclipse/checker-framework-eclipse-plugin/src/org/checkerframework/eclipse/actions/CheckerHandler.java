package org.checkerframework.eclipse.actions;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class CheckerHandler extends AbstractHandler {

    protected IJavaElement element(final ISelection selection) {
        throw new UnsupportedOperationException("This is only used by unused code at the moment!");
    }

    /**
     * Takes the current selection. Determines the target project using the first element in the
     * selection. Return each element in the selection (top-level or nested) that is part of the
     * target project
     *
     * @param selection Current user selection
     * @return A list of JavaElements that are in the same project and in the given selection
     */
    protected @Nullable List<IJavaElement> selectionToJavaElements(final ISelection selection) {
        // ITreeSelection

        final List<IJavaElement> elements;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            elements = toSingleProjectElements(structuredSelection.toArray());
        } else {
            elements = new ArrayList<IJavaElement>();
        }

        return elements;
    }

    protected List<IJavaElement> toSingleProjectElements(final Object[] elements) {
        final List<IJavaElement> javaElements = new ArrayList<IJavaElement>();

        IJavaProject project = null;
        for (final Object element : elements) {
            if (element instanceof IJavaProject) {
                // If the project is in the selection return only it
                if (project == null || element.equals(project)) {
                    javaElements.clear();
                    javaElements.add((IJavaProject) element);
                    break;
                }

            } else if (element instanceof IJavaElement) {
                final IJavaElement jEl = (IJavaElement) element;

                if (project == null) {
                    project = jEl.getJavaProject();
                    javaElements.add(jEl);

                    // Only add those elements that are in project
                } else if (!projectsEqual(jEl.getJavaProject(), project)) {
                    javaElements.add(jEl);
                }
            }
        }

        return javaElements;
    }

    //TODO: There must be a better way to do this
    protected boolean projectsEqual(final IJavaProject project1, final IJavaProject project2) {
        return project1.getPath().equals(project2);
    }

    /**
     * Retrieve the selection from the menu or otherwise when called from elsewhere
     *
     * @param event
     * @return the current selection
     */
    protected ISelection getSelection(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);

        /* use the current selection when not called from popup menu */
        if (selection == null) selection = HandlerUtil.getCurrentSelection(event);

        return selection;
    }
}
