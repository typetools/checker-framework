package org.checkerframework.eclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** The activator class controls the plug-in life cycle */
public class CheckerPlugin extends AbstractUIPlugin {

    /** Controls debugging of the plugin */
    public static boolean DEBUG = false;

    /** The plug-in ID */
    public static final String PLUGIN_ID = "checker.framework.eclipse.plugin";

    public static final String CHECKER_WARNING_IMG_ID = "checker-warning";

    /** The shared instance */
    private static CheckerPlugin plugin;

    /** The console name to use for this plugin */
    private static final String consoleName = "Checker Framework Plugin Console";

    public CheckerPlugin() {
        super();
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        reg.put(CHECKER_WARNING_IMG_ID, getImageDescriptor("icons/sample_decorator.gif"));
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static CheckerPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Log an exception.
     *
     * @param e the exception
     * @param message message describing how/why the exception occurred
     */
    public static void logException(Throwable e, String message) {
        getDefault().logMessage(IStatus.ERROR, message, e);
    }

    public void logMessage(int severity, String message, Throwable e) {
        if (DEBUG) {
            String what =
                    (severity == IStatus.ERROR) ? (e != null ? "Exception" : "Error") : "Warning";
            System.out.println(what + " in Checker Framework plugin: " + message);
            if (e != null) {
                e.printStackTrace();
            }
        }
        IStatus status = new Status(severity, CheckerPlugin.PLUGIN_ID, 0, message, e);
        getLog().log(status);
    }

    /**
     * Returns the SWT Shell of the active workbench window or {@code null} if no workbench window
     * is active.
     *
     * @return the SWT Shell of the active workbench window, or {@code null} if no workbench window
     *     is active
     */
    public static Shell getShell() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }
        return window.getShell();
    }

    /** @return active window instance, never null */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        if (Display.getCurrent() != null) {
            return getDefault().getWorkbench().getActiveWorkbenchWindow();
        }
        // need to call from UI thread
        final IWorkbenchWindow[] window = new IWorkbenchWindow[1];
        Display.getDefault()
                .syncExec(
                        new Runnable() {
                            @Override
                            public void run() {
                                window[0] = getDefault().getWorkbench().getActiveWorkbenchWindow();
                            }
                        });
        return window[0];
    }

    /**
     * Get the MessageConsole for the plugin
     *
     * @return a MessageConsole (will create if it doesn't exist)
     */
    public static MessageConsole findConsole() {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (IConsole element : existing) {
            if (consoleName.equals(element.getName())) {
                return (MessageConsole) element;
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(consoleName, null);
        conMan.addConsoles(new IConsole[] {myConsole});
        return myConsole;
    }
}
