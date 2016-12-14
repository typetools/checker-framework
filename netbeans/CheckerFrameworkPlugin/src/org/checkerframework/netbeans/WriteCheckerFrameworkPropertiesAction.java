package org.checkerframework.netbeans;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex.ExceptionAction;

public class WriteCheckerFrameworkPropertiesAction implements ExceptionAction<Void> {
    private final EditableProperties ep;
    private final FileObject projPropsFO;
    private final InputStream is;
    private final StringBuilder selection;
    private final String checkerPath;
    private final String checkerQualPath;

    public WriteCheckerFrameworkPropertiesAction(
            FileObject projectProperties,
            String inCheckerPath,
            String inCheckerQualPath,
            StringBuilder inSelection)
            throws FileNotFoundException {
        ep = new EditableProperties(true);
        projPropsFO = projectProperties;
        is = projPropsFO.getInputStream();
        selection = inSelection;
        checkerPath = inCheckerPath;
        checkerQualPath = inCheckerQualPath;
    }

    /**
     * run() method for the WriteCheckerFrameworkPropertiesAction saves the selected checkers to run
     * in the project's properties file.
     *
     * @return nothing
     * @throws Exception
     */
    @Override
    public Void run() throws Exception {
        try {
            ep.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        ep.setProperty("annotation.processing.enabled", "true");
        ep.setProperty("annotation.processing.processors.list", selection.toString());

        //@todo: make this not overwrite other libraries added to the project in question...
        ep.setProperty("file.reference.checker-qual.jar", checkerQualPath);
        ep.setProperty("file.reference.checker.jar", checkerPath);
        ep.setProperty("javac.classpath", "${file.reference.checker-qual.jar}");
        ep.setProperty("javac.processorpath", "${javac.classpath}:${file.reference.checker.jar}");
        OutputStream os = null;
        FileLock lock = null;
        try {
            lock = projPropsFO.lock();
            os = projPropsFO.getOutputStream(lock);
            ep.store(os);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            if (os != null) {
                os.close();
            }
        }
        return null;
    }
}
