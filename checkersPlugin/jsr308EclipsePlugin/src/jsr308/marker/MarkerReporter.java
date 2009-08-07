package jsr308.marker;

import jsr308.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Creates a JSR308 marker in a runnable window.
 */
public class MarkerReporter implements IWorkspaceRunnable{
    private final IResource resource;
    private final int startLine;
    private final String message;

    public MarkerReporter(IResource resource, int startLine, String message){
        this.startLine = startLine;
        this.resource = resource;
        this.message = message;
    }

    public void run(IProgressMonitor monitor) throws CoreException{

        String markerType = getMarkerType();

        if (markerType == null){
            return;
        }

        if (Activator.DEBUG){
            System.out.println("Creating marker for " + resource.getLocation());
        }

        // This triggers resource update on IResourceChangeListener's (BugTreeView)
        IMarker marker = resource.createMarker(markerType);

        setAttributes(marker);
    }

    private String getMarkerType(){
        return JSR308Marker.NAME;
    }

    /**
     * @param marker
     * @throws CoreException
     */
    private void setAttributes(IMarker marker) throws CoreException{
        if (Activator.DEBUG){
            System.out.println("Setting attibutes for marker in " + resource.getLocation());
        }

        marker.setAttribute(IMarker.LINE_NUMBER, startLine);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    }
}
