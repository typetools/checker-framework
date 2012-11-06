package checkers.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import checkers.eclipse.CheckerPlugin;
import checkers.eclipse.marker.MarkerReporter;

/**
 * Utility methods for Eclipse markers.
 */
public final class MarkerUtil
{

    private MarkerUtil()
    {
        throw new AssertionError("Shouldn't be initialized");
    }

    /**
     * Remove all FindBugs problem markers for given resource.
     */
    public static void removeMarkers(IResource res) throws CoreException
    {
        // remove any markers added by our builder
        // This triggers resource update on IResourceChangeListener's
        // (BugTreeView)
        if (CheckerPlugin.DEBUG)
        {
            System.out.println("Removing JSR 308 markers in "
                    + res.getLocation());
        }
        res.deleteMarkers(MarkerReporter.NAME, true, IResource.DEPTH_INFINITE);
    }

    /**
     * @param message  The message to attach to the marker
     * @param project  The project being worked on
     * @param resource Typically a file, the resource being marked
     * @param startLine The line 
     */
    public static void addMarker(String message, IProject project,
            IResource resource, int startLine)
    {
        if (CheckerPlugin.DEBUG)
        {
            System.out.println("Creating marker for " + resource.getLocation()
                    + ": line " + startLine + " " + message);
        }

        try
        {
            project.getWorkspace().run(
                    new MarkerReporter(resource, startLine, message), null, 0,
                    null);
        }catch (CoreException e)
        {
            CheckerPlugin.logException(e, "Core exception on add marker");
        }
    }

}
