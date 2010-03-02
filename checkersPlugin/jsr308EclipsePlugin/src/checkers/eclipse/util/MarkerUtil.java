package checkers.eclipse.util;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import checkers.eclipse.*;
import checkers.eclipse.marker.*;

/**
 * Utility methods for Eclipse markers.
 */
public final class MarkerUtil{

    /**
     * don't instantiate an utility class
     */
    private MarkerUtil(){
        super();
    }

    /**
     * Remove all FindBugs problem markers for given resource.
     * 
     * @param res
     *            the resource
     * @throws CoreException
     */
    public static void removeMarkers(IResource res) throws CoreException{
        // remove any markers added by our builder
        // This triggers resource update on IResourceChangeListener's (BugTreeView)
        if (Activator.DEBUG){
            System.out.println("Removing JSR 308 markers in " + res.getLocation());
        }
        res.deleteMarkers(MarkerReporter.NAME, true, IResource.DEPTH_INFINITE);
    }

    public static void addMarker(String message, IProject project, IResource resource, int startLine){
        if (Activator.DEBUG){
            System.out.println("Creating marker for " + resource.getLocation() + ": line " + startLine + " " + message);
        }
        try{
            project.getWorkspace().run(new MarkerReporter(resource, startLine, message), null, // scheduling rule (null if there are no scheduling restrictions)
                    0, // flags (could specify IWorkspace.AVOID_UPDATE)
                    null); // progress monitor (null if progress reporting is not desired)
        }catch (CoreException e){
            Activator.getDefault().logException(e, "Core exception on add marker");
        }
    }

}
