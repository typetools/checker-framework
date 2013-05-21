package checkers.eclipse.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import checkers.eclipse.CheckerPlugin;

/**
 * Creates a JSR308 marker in a runnable window.
 */
public class MarkerReporter implements IWorkspaceRunnable
{
    public static final String NAME = CheckerPlugin.PLUGIN_ID + ".marker";
    public static final String ERROR_KEY           = "errorKey";
    public static final String NUM_ERROR_ARGUMENTS = "numErrorArguments";
    public static final String ERROR_ARGUMENTS     = "errorArguments";
    private static final String DETAIL_SEPARATOR = "$$";
    private static final String DETAIL_SEPARATOR_REGEX = "\\$\\$";

    //Typically a Java File
    private final IResource resource;
    
    
    private final int startLine;
    private final String message;
    private final int startPosition;
    private final int endPosition;

    private final String errorKey;
    private final int numErrorArguments;
    private final String errorArguments;


    public MarkerReporter(IResource resource, int startLine, String message, int startPosition, int endPosition)
    {
        this.startLine = startLine;
        this.resource = resource;
        this.startPosition = startPosition;
        this.endPosition = endPosition;

        final String [] details = message.split(DETAIL_SEPARATOR_REGEX);
        if(details.length < 3) {
            throw new RuntimeException("Marker reporter expects at least 3 arguments separated by "
                    + DETAIL_SEPARATOR + ".\n" +
                    "Message was: " + message + " line was " + startLine);
        }

        errorKey = details[0].substring(1).substring(0, details[0].length()-2); //strip off ()

        numErrorArguments = Integer.parseInt(details[1].trim());

        if(numErrorArguments > 0) {
            String errorArgs = details[0];

            for(int i = 1; i < numErrorArguments; i++) {
                errorArgs += DETAIL_SEPARATOR + details[i+2];
            }
            errorArguments = errorArgs;
        } else {
            errorArguments = "";
        }

        this.message = details[details.length - 1];
    }



    @Override
    public void run(IProgressMonitor monitor) throws CoreException
    {

        if (CheckerPlugin.DEBUG)
        {
            System.out.println("Creating marker for " + resource.getLocation());
        }

        IMarker marker = resource.createMarker(NAME);
        if (CheckerPlugin.DEBUG)
        {
            System.out.println("Setting attibutes for marker in "
                    + resource.getLocation());
        }

        marker.setAttribute(IMarker.LINE_NUMBER, startLine);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
        marker.setAttribute(IMarker.CHAR_START, startPosition);
        marker.setAttribute(IMarker.CHAR_END, endPosition);

        marker.setAttribute(ERROR_KEY,           errorKey);
        marker.setAttribute(NUM_ERROR_ARGUMENTS, numErrorArguments);
        marker.setAttribute(ERROR_ARGUMENTS,     errorArguments);
    }

}
