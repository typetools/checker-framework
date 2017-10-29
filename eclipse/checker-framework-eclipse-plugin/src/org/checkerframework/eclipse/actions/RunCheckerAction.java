package org.checkerframework.eclipse.actions;

import java.util.List;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.util.MutexSchedulingRule;
import org.checkerframework.eclipse.util.PluginUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;

/** RunCheckerAction is an action handler that determines what */
// TODO: Rename to RunCheckerHandler
// TODO: Remove all subclasses and just parameterize RunCheckerAction (perhaps take a list of
// TODO: checkers, or if no checkers are specified use custom checkers)
public abstract class RunCheckerAction extends CheckerHandler {
    private final String checkerName;
    protected boolean usePrefs;
    protected boolean useCustom;
    protected boolean useSingleCustom;
    protected boolean hasQuals;

    /** true if this action is used from editor */
    protected boolean usedInEditor;

    protected RunCheckerAction() {
        super();
        this.checkerName = null;
        this.usePrefs = true;
        this.useCustom = false;
        this.useSingleCustom = false;
        this.hasQuals = true;
    }

    protected RunCheckerAction(String checkerName) {
        this(checkerName, true);
    }

    protected RunCheckerAction(String checkerName, boolean hasQuals) {
        super();
        this.checkerName = checkerName;
        this.useCustom = false;
        this.usePrefs = false;
        this.useSingleCustom = false;
        this.hasQuals = hasQuals;
    }

    /**
     * If constructed with a no-arg constructor, then we get the list of classes to use from the
     * preferences system
     */
    private List<String> getClassNameFromPrefs() {
        return CheckerManager.getSelectedClasses();
    }

    /** */
    public Object execute(ExecutionEvent event) {
        ISelection selection = getSelection(event);
        List<IJavaElement> elements = selectionToJavaElements(selection);

        if (!elements.isEmpty()) {
            Job checkerJob;
            String customClasses =
                    CheckerPlugin.getDefault()
                            .getPreferenceStore()
                            .getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES);

            // Depending on how this runner was created, we will either:
            // * just run one particular checker
            // * use the custom configured checkers
            // * run "selected" checkers using the action or auto build

            final String actualNames;

            if (!usePrefs && !useCustom && !useSingleCustom) {
                actualNames = checkerName;
            } else if (!usePrefs && !useSingleCustom) {
                actualNames = customClasses;
            } else if (useSingleCustom) {
                actualNames = event.getParameter("checker-framework-eclipse-plugin.checker");
            } else {
                List<String> names = getClassNameFromPrefs();
                actualNames = PluginUtil.join(",", names);
            }

            checkerJob = new CheckerWorker(elements, actualNames, hasQuals);

            checkerJob.setUser(true);
            checkerJob.setPriority(Job.BUILD);
            checkerJob.setRule(new MutexSchedulingRule());
            checkerJob.schedule();
        }

        return null;
    }
}
