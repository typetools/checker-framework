package org.checkerframework.eclipse.actions;

import com.sun.tools.javac.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.javac.CheckersRunner;
import org.checkerframework.eclipse.javac.CommandlineJavacRunner;
import org.checkerframework.eclipse.javac.JavacError;
import org.checkerframework.eclipse.javac.JavacRunner;
import org.checkerframework.eclipse.util.*;
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

// TODO: RENAME THIS TO CHECKER JOB
public class CheckerWorker extends Job {
    private final IJavaProject project;
    private final String checkerNames;
    private String[] sourceFiles;

    private final String javacJreVersion = "1.8.0";

    private final boolean useJavacRunner;
    private final boolean hasQuals;

    /**
     * This constructor is intended for use from an incremental builder that has a list of updated
     * source files to check
     *
     * @param project
     * @param sourceFiles
     * @param checkerNames
     */
    public CheckerWorker(
            IJavaProject project, String[] sourceFiles, String checkerNames, boolean hasQuals) {
        super("Running checker on " + sourceFiles.toString());
        this.project = project;
        this.sourceFiles = sourceFiles;
        this.checkerNames = checkerNames;
        this.useJavacRunner = shouldUseJavacRunner();
        this.hasQuals = hasQuals;
    }

    public CheckerWorker(List<IJavaElement> elements, String checkerNames, boolean hasQuals) {
        super("Running checker on " + PluginUtil.join(",", elements));
        this.project = elements.get(0).getJavaProject();
        this.checkerNames = checkerNames;
        this.useJavacRunner = shouldUseJavacRunner();

        this.hasQuals = hasQuals;
        try {
            this.sourceFiles = ResourceUtils.sourceFilesOf(elements).toArray(new String[] {});
        } catch (CoreException e) {
            CheckerPlugin.logException(e, e.getMessage());
        }
    }

    private boolean shouldUseJavacRunner() {
        // int expectedLength = "1.x.x".length();
        // final String jreVersion =
        //  System.getProperties().getProperty("java.runtime.version").substring(0, expectedLength);
        // return jreVersion.equals(javacJreVersion);
        return false;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            work(monitor);
        } catch (Throwable e) {
            CheckerPlugin.logException(e, "Analysis exception");
            return Status.CANCEL_STATUS;
        }

        return Status.OK_STATUS;
    }

    private void work(final IProgressMonitor pm) throws CoreException {
        if (checkerNames != null) {
            pm.beginTask(
                    "Running checker(s) "
                            + checkerNames.toString()
                            + " on "
                            + sourceFiles.toString(),
                    10);
        } else {
            pm.beginTask("Running custom single checker " + " on " + sourceFiles.toString(), 10);
        }

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

    private List<JavacError> runChecker() throws JavaModelException {
        final Pair<String, String> classpaths = classPathOf(project);

        final CheckersRunner runner;
        if (useJavacRunner) {
            runner =
                    new JavacRunner(
                            sourceFiles,
                            checkerNames.split(","),
                            classpaths.fst + File.pathSeparator + classpaths.snd,
                            hasQuals);
        } else {
            runner =
                    new CommandlineJavacRunner(
                            sourceFiles,
                            checkerNames.split(","),
                            classpaths.fst,
                            classpaths.snd,
                            hasQuals);
        }
        runner.run();

        return runner.getErrors();
    }

    /**
     * Mark errors for this project in the appropriate files
     *
     * @param project
     */
    private void markErrors(IJavaProject project, List<JavacError> errors) {
        for (JavacError error : errors) {
            if (error.file == null) {
                continue;
            }

            IResource file = ResourceUtils.getFile(project, error.file);
            if (file == null) continue;
            MarkerUtil.addMarker(
                    error.message,
                    project.getProject(),
                    file,
                    error.lineNumber,
                    error.errorKey,
                    error.errorArguments,
                    error.startPosition,
                    error.endPosition);
        }
    }

    private Pair<List<String>, List<String>> pathOf(IClasspathEntry cp, IJavaProject project)
            throws JavaModelException {
        int entryKind = cp.getEntryKind();
        switch (entryKind) {
            case IClasspathEntry.CPE_SOURCE:
                return new Pair<List<String>, List<String>>(
                        Arrays.asList(new String[] {ResourceUtils.outputLocation(cp, project)}),
                        new ArrayList<String>());

            case IClasspathEntry.CPE_LIBRARY:
                return new Pair<List<String>, List<String>>(
                        Arrays.asList(new String[] {Paths.absolutePathOf(cp)}),
                        new ArrayList<String>());

            case IClasspathEntry.CPE_PROJECT:
                return projectPathOf(cp);

            case IClasspathEntry.CPE_CONTAINER:
                List<String> resultPaths = new ArrayList<String>();
                List<String> resultBootPaths = new ArrayList<String>();
                IClasspathContainer c = JavaCore.getClasspathContainer(cp.getPath(), project);
                if (c.getKind() == IClasspathContainer.K_DEFAULT_SYSTEM
                        || c.getKind() == IClasspathContainer.K_SYSTEM) {
                    for (IClasspathEntry entry : c.getClasspathEntries()) {
                        if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                            resultBootPaths.add(
                                    entry.getPath().makeAbsolute().toFile().getAbsolutePath());
                        }
                    }
                } else {
                    for (IClasspathEntry entry : c.getClasspathEntries()) {
                        if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                            resultPaths.add(
                                    entry.getPath().makeAbsolute().toFile().getAbsolutePath());
                        }
                    }
                }
                return new Pair<List<String>, List<String>>(resultPaths, resultBootPaths);

            case IClasspathEntry.CPE_VARIABLE:
                return pathOf(JavaCore.getResolvedClasspathEntry(cp), project);
        }

        return new Pair<List<String>, List<String>>(
                new ArrayList<String>(), new ArrayList<String>());
    }

    /**
     * Returns the project's classpath in a format suitable for javac
     *
     * @param project
     * @return the project's classpath as a string
     * @throws JavaModelException
     */
    private Pair<String, String> classPathOf(IJavaProject project) throws JavaModelException {
        Pair<List<String>, List<String>> paths = classPathEntries(project);

        return new Pair<String, String>(
                PluginUtil.join(File.pathSeparator, paths.fst),
                PluginUtil.join(File.pathSeparator, paths.snd));
    }

    private Pair<List<String>, List<String>> classPathEntries(IJavaProject project)
            throws JavaModelException {

        final Pair<List<String>, List<String>> results =
                new Pair<List<String>, List<String>>(
                        new ArrayList<String>(), new ArrayList<String>());

        for (IClasspathEntry cp : project.getResolvedClasspath(true)) {
            Pair<List<String>, List<String>> paths = pathOf(cp, project);
            results.fst.addAll(paths.fst);
            results.snd.addAll(paths.snd);
        }

        return results;
    }

    private Pair<List<String>, List<String>> projectPathOf(IClasspathEntry entry)
            throws JavaModelException {
        final IProject project =
                ResourceUtils.workspaceRoot().getProject(entry.getPath().toOSString());
        return classPathEntries(JavaCore.create(project));
    }
}
