package checkers.eclipse.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import checkers.eclipse.CheckerPlugin;
import checkers.eclipse.javac.CommandlineJavacRunner;
import checkers.eclipse.javac.JavacError;
import checkers.eclipse.util.JavaUtils;
import checkers.eclipse.util.MarkerUtil;
import checkers.eclipse.util.Paths;
import checkers.eclipse.util.ResourceUtils;

import com.sun.tools.javac.util.Pair;

public class CheckerWorker extends Job
{

    private final IJavaProject project;
    private final String checkerNames;
    private String[] sourceFiles;

    /**
     * This constructor is intended for use from an incremental builder that has
     * a list of updated source files to check
     * 
     * @param project
     * @param sourceFiles
     * @param checkerNames
     */
    public CheckerWorker(IJavaProject project, String[] sourceFiles,
            String checkerNames)
    {
        super("Running checker on " + sourceFiles.toString());
        this.project = project;
        this.sourceFiles = sourceFiles;
        this.checkerNames = checkerNames;
    }

    public CheckerWorker(IJavaElement element, String checkerNames)
    {
        super("Running checker on " + element.getElementName());
        this.project = element.getJavaProject();
        this.checkerNames = checkerNames;

        try
        {
            this.sourceFiles = ResourceUtils.sourceFilesOf(element).toArray(
                    new String[] {});
        }catch (CoreException e)
        {
            CheckerPlugin.logException(e, e.getMessage());
        }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        try
        {
            work(monitor);
        }catch (Throwable e)
        {
            CheckerPlugin.logException(e, "Analysis exception");
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    private void work(IProgressMonitor pm) throws CoreException
    {
        pm.beginTask("Running checkers " + checkerNames.toString() + " on "
                + sourceFiles.toString(), 10);

        pm.setTaskName("Removing old markers");
        MarkerUtil.removeMarkers(project.getResource());
        pm.worked(1);

        pm.setTaskName("Running checker");
        List<JavacError> callJavac = runChecker();
        pm.worked(6);

        pm.setTaskName("Updating problem list");
        markErrors(project, callJavac);
        pm.worked(3);

        pm.done();
    }

    private List<JavacError> runChecker() throws JavaModelException
    {
        Pair<String, String> classpaths = classPathOf(project);
        CommandlineJavacRunner runner = new CommandlineJavacRunner(sourceFiles,
                checkerNames, classpaths.fst, classpaths.snd);

        runner.run();

        return runner.getErrors();
    }

    /**
     * Mark errors for this project in the appropriate files
     * 
     * @param project
     * @param callJavac
     */

    private void markErrors(IJavaProject project, List<JavacError> errors)
    {
        for (JavacError error : errors)
        {
            IResource file = ResourceUtils.getFile(project, error.file);
            if (file == null)
                continue;
            MarkerUtil.addMarker(error.message, project.getProject(), file,
                    error.lineNumber);
        }
    }

    private Pair<List<String>, List<String>> pathOf(IClasspathEntry cp,
            IJavaProject project) throws JavaModelException
    {
        int entryKind = cp.getEntryKind();
        switch (entryKind)
        {
        case IClasspathEntry.CPE_SOURCE:
            return new Pair<List<String>, List<String>>(
                    Arrays.asList(new String[] { ResourceUtils.outputLocation(
                            cp, project) }), new ArrayList<String>());
        case IClasspathEntry.CPE_LIBRARY:
            return new Pair<List<String>, List<String>>(
                    Arrays.asList(new String[] { Paths.absolutePathOf(cp) }),
                    new ArrayList<String>());
        case IClasspathEntry.CPE_PROJECT:
            return projectPathOf(cp);
        case IClasspathEntry.CPE_CONTAINER:
            List<String> resultPaths = new ArrayList<String>();
            List<String> resultBootPaths = new ArrayList<String>();
            IClasspathContainer c = JavaCore.getClasspathContainer(
                    cp.getPath(), project);
            if (c.getKind() == IClasspathContainer.K_DEFAULT_SYSTEM
                    || c.getKind() == IClasspathContainer.K_SYSTEM)
            {
                for (IClasspathEntry entry : c.getClasspathEntries())
                {
                    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
                    {
                        resultBootPaths.add(entry.getPath().makeAbsolute()
                                .toFile().getAbsolutePath());
                    }
                }
            }
            else
            {
                for (IClasspathEntry entry : c.getClasspathEntries())
                {
                    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
                    {
                        resultPaths.add(entry.getPath().makeAbsolute().toFile()
                                .getAbsolutePath());
                    }
                }
            }
            return new Pair<List<String>, List<String>>(resultPaths,
                    resultBootPaths);
        case IClasspathEntry.CPE_VARIABLE:
            return pathOf(JavaCore.getResolvedClasspathEntry(cp), project);
        }
        return new Pair<List<String>, List<String>>(new ArrayList<String>(),
                new ArrayList<String>());
    }

    /**
     * Returns the project's classpath in a format suitable for javac
     * 
     * @param project
     * @return the project's classpath as a string
     * @throws JavaModelException
     */
    private Pair<String, String> classPathOf(IJavaProject project)
            throws JavaModelException
    {
        Pair<List<String>, List<String>> paths = classPathEntries(project);

        return new Pair<String, String>(JavaUtils.join(File.pathSeparator,
                paths.fst), JavaUtils.join(File.pathSeparator, paths.snd));
    }

    private Pair<List<String>, List<String>> classPathEntries(
            IJavaProject project) throws JavaModelException
    {
        Pair<List<String>, List<String>> results = new Pair<List<String>, List<String>>(
                new ArrayList<String>(), new ArrayList<String>());

        for (IClasspathEntry cp : project.getRawClasspath())
        {
            Pair<List<String>, List<String>> paths = pathOf(cp, project);
            results.fst.addAll(paths.fst);
            results.snd.addAll(paths.snd);
        }

        return results;
    }

    private Pair<List<String>, List<String>> projectPathOf(IClasspathEntry entry)
            throws JavaModelException
    {
        IProject project = ResourceUtils.workspaceRoot().getProject(
                entry.getPath().toOSString());
        return classPathEntries(JavaCore.create(project));
    }
}
