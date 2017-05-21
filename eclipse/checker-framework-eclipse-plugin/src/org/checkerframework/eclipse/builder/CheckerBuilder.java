package org.checkerframework.eclipse.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.actions.CheckerManager;
import org.checkerframework.eclipse.actions.CheckerWorker;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.util.MutexSchedulingRule;
import org.checkerframework.eclipse.util.PluginUtil;
import org.checkerframework.eclipse.util.ResourceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;

public class CheckerBuilder extends IncrementalProjectBuilder {
    public static final String BUILDER_ID = "checkers.eclipse.checkerbuilder";

    public CheckerBuilder() {
        super();
    }

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        if (isBuildEnabled()) {
            if (kind == FULL_BUILD) {
                fullBuild();
            } else {
                IResourceDelta delta = getDelta(getProject());
                if (delta == null) {
                    fullBuild();
                } else {
                    incrementalBuild(delta);
                }
            }
        }

        return null;
    }

    private boolean isBuildEnabled() {
        IPreferenceStore store = CheckerPlugin.getDefault().getPreferenceStore();

        return store.getBoolean(CheckerPreferences.PREF_CHECKER_AUTO_BUILD);
    }

    private void incrementalBuild(IResourceDelta delta) {
        CheckerResourceVisitor visitor = new CheckerResourceVisitor();
        try {
            delta.accept(visitor);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        runWorker(
                JavaCore.create(getProject()),
                new LinkedHashSet<String>(visitor.getBuildFiles()),
                CheckerManager.getSelectedClasses());
    }

    private void fullBuild() throws CoreException {
        IJavaProject project = JavaCore.create(getProject());
        Set<String> sourceNames = ResourceUtils.sourceFilesOf(project);

        runWorker(project, sourceNames, CheckerManager.getSelectedClasses());
    }

    private void runWorker(
            IJavaProject project, Set<String> sourceNames, List<String> checkerNames) {
        Job checkerJob =
                new CheckerWorker(
                        project,
                        sourceNames.toArray(new String[] {}),
                        PluginUtil.join(",", checkerNames),
                        true);

        checkerJob.setUser(true);
        checkerJob.setPriority(Job.BUILD);
        checkerJob.setRule(new MutexSchedulingRule());
        checkerJob.schedule();
    }
}
