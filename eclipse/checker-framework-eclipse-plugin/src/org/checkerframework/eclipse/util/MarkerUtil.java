package org.checkerframework.eclipse.util;

import java.util.Arrays;
import java.util.List;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.marker.MarkerReporter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/** Utility methods for Eclipse markers. */
public final class MarkerUtil {

    private MarkerUtil() {
        throw new AssertionError("Shouldn't be initialized");
    }

    /** Remove all FindBugs problem markers for given resource. */
    public static void removeMarkers(IResource res) throws CoreException {
        // remove any markers added by our builder
        // This triggers resource update on IResourceChangeListener's
        // (BugTreeView)
        if (CheckerPlugin.DEBUG) {
            System.out.println("Removing Checker Framework markers in " + res.getLocation());
        }
        res.deleteMarkers(MarkerReporter.NAME, true, IResource.DEPTH_INFINITE);
    }

    /**
     * @param message The message to attach to the marker
     * @param project The project being worked on
     * @param resource Typically a file, the resource being marked
     * @param startLine The line
     * @param startPosition The offset of the beginning of the code snippet related to the message
     * @param endPosition The offset of the end of the code snippet related to the message
     */
    public static void addMarker(
            String message,
            IProject project,
            IResource resource,
            int startLine,
            String errorKey,
            List<String> errorArguments,
            int startPosition,
            int endPosition) {
        if (CheckerPlugin.DEBUG) {
            System.out.println(
                    "Creating marker for "
                            + resource.getLocation()
                            + ": line "
                            + startLine
                            + ": error key "
                            + errorKey
                            + ": error arguments "
                            + Arrays.toString(errorArguments.toArray(new String[] {}))
                            + " : start position "
                            + startPosition
                            + " : end position "
                            + endPosition
                            + " "
                            + message);
        }

        try {
            project.getWorkspace()
                    .run(
                            new MarkerReporter(
                                    resource,
                                    startLine,
                                    errorKey,
                                    errorArguments,
                                    message,
                                    startPosition,
                                    endPosition),
                            null,
                            0,
                            null);
        } catch (CoreException e) {
            CheckerPlugin.logException(e, "Core exception on add marker");
        }
    }
}
