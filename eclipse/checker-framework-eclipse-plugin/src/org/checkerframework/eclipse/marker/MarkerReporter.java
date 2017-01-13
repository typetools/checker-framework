package org.checkerframework.eclipse.marker;

import java.util.Iterator;
import java.util.List;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/** Creates a JSR308 marker in a runnable window. */
public class MarkerReporter implements IWorkspaceRunnable {
    public static final String NAME = CheckerPlugin.PLUGIN_ID + ".marker";
    public static final String ERROR_KEY = "errorKey";
    public static final String NUM_ERROR_ARGUMENTS = "numErrorArguments";
    public static final String ERROR_ARGUMENTS = "errorArguments";
    private static final String DETAIL_SEPARATOR = "$$";
    private static final String DETAIL_SEPARATOR_REGEX = "\\$\\$";

    // Typically a Java File
    private final IResource resource;

    private final int startLine;
    private final String message;
    private final int startPosition;
    private final int endPosition;

    private final String errorKey;
    private final List<String> errorArguments;

    public MarkerReporter(
            IResource resource,
            int startLine,
            String errorKey,
            List<String> errorArguments,
            String message,
            int startPosition,
            int endPosition) {
        this.startLine = startLine;
        this.resource = resource;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.errorKey = errorKey;
        this.errorArguments = errorArguments;
        this.message = message;
    }

    @Override
    public void run(IProgressMonitor monitor) throws CoreException {

        boolean reportAsError =
                !CheckerPlugin.getDefault()
                        .getPreferenceStore()
                        .getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS);

        if (CheckerPlugin.DEBUG) {
            System.out.println("Creating marker for " + resource.getLocation());
        }

        IMarker marker = resource.createMarker(NAME);
        if (CheckerPlugin.DEBUG) {
            System.out.println("Setting attibutes for marker in " + resource.getLocation());
        }

        marker.setAttribute(IMarker.LINE_NUMBER, startLine);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(
                IMarker.SEVERITY,
                reportAsError ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
        marker.setAttribute(IMarker.CHAR_START, startPosition);
        marker.setAttribute(IMarker.CHAR_END, endPosition);

        marker.setAttribute(ERROR_KEY, errorKey);
        marker.setAttribute(NUM_ERROR_ARGUMENTS, errorArguments.size());
        Iterator<String> iterator = errorArguments.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String errorArgument = iterator.next();
            marker.setAttribute(ERROR_ARGUMENTS + i, errorArgument);
            ++i;
        }
    }
}
