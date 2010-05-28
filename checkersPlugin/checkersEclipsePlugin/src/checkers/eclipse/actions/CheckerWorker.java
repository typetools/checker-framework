package checkers.eclipse.actions;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;

import checkers.eclipse.*;
import checkers.eclipse.javac.*;
import checkers.eclipse.util.*;
import checkers.eclipse.util.Paths.ClasspathBuilder;

public class CheckerWorker extends Job {

    private final IJavaProject project;
    private final String checkerName;

    public CheckerWorker(IJavaProject project, String checkerName) {
        super("Running checker on " + project.getElementName());
        this.project = project;
        this.checkerName = checkerName;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            work(monitor);
        } catch (Throwable e) {
            Activator.logException(e, "Analysis exception");
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    private void work(IProgressMonitor pm) throws CoreException {
        pm.beginTask(
                "Running checker " + checkerName + " on "
                        + project.getElementName(), 10);

        pm.setTaskName("Removing old markers");
        MarkerUtil.removeMarkers(project.getResource());
        pm.worked(1);

        pm.setTaskName("Running checker");
        List<JavacError> callJavac = runChecker(project, checkerName);
        pm.worked(6);

        pm.setTaskName("Updating problem list");
        markErrors(project, callJavac);
        pm.worked(3);

        pm.done();
    }

    private List<JavacError> runChecker(IJavaProject project, String checkerName)
            throws CoreException, JavaModelException {
        List<String> javaFileNames = ResourceUtils.sourceFilesOf(project);
        String cp = classPathOf(project);

        // XXX it is very annoying that we run commandline javac rather than
        // directly. But otherwise there's a classpath hell.
        List<JavacError> callJavac = new CommandlineJavacRunner().callJavac(
                javaFileNames, checkerName, cp);
        return callJavac;
    }

    private void markErrors(IJavaProject project, List<JavacError> errors) {
        for (JavacError error : errors) {
            IResource file = ResourceUtils.getFile(project, error.file);
            if (file == null)
                continue;
            MarkerUtil.addMarker(error.message, project.getProject(),
                    file, error.lineNumber);
        }
    }

    private String pathOf(IClasspathEntry cp, IJavaProject project)
            throws JavaModelException {
        int entryKind = cp.getEntryKind();
        switch (entryKind) {
        case IClasspathEntry.CPE_SOURCE:
            return ResourceUtils.outputLocation(cp, project);
        case IClasspathEntry.CPE_LIBRARY:
            return Paths.absolutePathOf(cp);
        case IClasspathEntry.CPE_PROJECT:
            // TODO unimplemented!
            break;
        case IClasspathEntry.CPE_CONTAINER:
            IClasspathContainer c = JavaCore.getClasspathContainer(
                    cp.getPath(), project);
            if (c.getKind() == IClasspathContainer.K_DEFAULT_SYSTEM
                    || c.getKind() == IClasspathContainer.K_SYSTEM)
                break;
            for (IClasspathEntry entry : c.getClasspathEntries()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    return entry.getPath().makeAbsolute().toFile()
                            .getAbsolutePath();
                } else {
                    // TODO unimplemented!
                }
            }
            return "";
        case IClasspathEntry.CPE_VARIABLE:
            // TODO unimplemented!
            return "";
        }
        return "";
    }

    // Returns the project's classpath in a format suitable for javac
    private String classPathOf(IJavaProject project) throws JavaModelException {
        ClasspathBuilder classpath = new ClasspathBuilder();

        for (IClasspathEntry cp : project.getRawClasspath()) {
            String path = pathOf(cp, project);
            classpath.append(path);
        }

        classpath.append(CommandlineJavacRunner.checkersJARlocation());
        return classpath.toString();
    }
}
