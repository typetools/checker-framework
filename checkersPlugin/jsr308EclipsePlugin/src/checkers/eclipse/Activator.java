package checkers.eclipse;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.console.*;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.*;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin{

    /** Controls debugging of the plugin */
    public static boolean DEBUG = false;

    /** The plug-in ID */
    public static final String PLUGIN_ID = "checkers.eclipse";

    /** The shared instance */
    private static Activator plugin;

    public Activator(){
        // empty
    }

    @Override
    public void start(BundleContext context) throws Exception{
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception{
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault(){
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path){
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Log an exception.
     * 
     * @param e
     *            the exception
     * @param message
     *            message describing how/why the exception occurred
     */
    public void logException(Throwable e, String message){
        logMessage(IStatus.ERROR, message, e);
    }

    public void logMessage(int severity, String message, Throwable e){
        if (DEBUG){
            String what = (severity == IStatus.ERROR) ? (e != null ? "Exception" : "Error") : "Warning";
            System.out.println(what + " in JSR 308 plugin: " + message);
            if (e != null){
                e.printStackTrace();
            }
        }
        IStatus status = new Status(severity, Activator.PLUGIN_ID, 0, message, e);
        getLog().log(status);
    }

    /**
     * Returns the SWT Shell of the active workbench window or <code>null</code> if no workbench window is active.
     * 
     * @return the SWT Shell of the active workbench window, or <code>null</code> if no workbench window is active
     */
    public static Shell getShell(){
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window == null){
            return null;
        }
        return window.getShell();
    }

    /**
     * @return active window instance, never null
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow(){
        if (Display.getCurrent() != null){
            return getDefault().getWorkbench().getActiveWorkbenchWindow();
        }
        // need to call from UI thread
        final IWorkbenchWindow[] window = new IWorkbenchWindow[1];
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run(){
                window[0] = getDefault().getWorkbench().getActiveWorkbenchWindow();
            }
        });
        return window[0];
    }

    public static MessageConsole findConsole(){
        String name = "Checkers Plugins";
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }
}
