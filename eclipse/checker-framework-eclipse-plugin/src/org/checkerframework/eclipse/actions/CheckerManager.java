package org.checkerframework.eclipse.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.util.PluginUtil;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class manages the current checkers that can be run. Also keeps track of custom checkers that
 * are made by the user.
 *
 * @author asumu
 */
public class CheckerManager {

    /** Singleton constructor, should only be called once for the instance */
    private CheckerManager() {}

    public static List<CheckerInfo> getCheckerInfos() {
        return CheckerInfo.getCheckers();
    }

    /**
     * For each processor in processors, check to see if we recognize that processor and add its
     * implicit imports to the "selected" list. Return selected.
     *
     * @return a list of quals paths to use as imports
     */
    public static List<String> getSelectedQuals(final String[] processors) {
        List<String> selected = new ArrayList<String>();

        final Map<String, CheckerInfo> pathToChecker = CheckerInfo.getPathToCheckerInfo();

        for (String processor : processors) {
            final String trimmedProc = processor.trim();
            if (pathToChecker.containsKey(trimmedProc)) {
                final String qualsPath = pathToChecker.get(trimmedProc).getQualsPath();
                if (qualsPath != null) {
                    selected.add(pathToChecker.get(trimmedProc).getQualsPath());
                }
            }
        }

        return selected;
    }

    public static IPreferenceStore getPrefStore() {
        return CheckerPlugin.getDefault().getPreferenceStore();
    }

    public static String[] getStoredCustomClasses() {
        final IPreferenceStore store = getPrefStore();
        final String storedItems = store.getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES);

        if (storedItems != null && !storedItems.equals("")) {
            return storedItems.split(",");
        }

        return new String[] {};
    }

    public static void storeCustomClasses(final String[] customClasses) {
        final IPreferenceStore store = getPrefStore();
        final String classString = PluginUtil.join(",", customClasses);

        store.setValue(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES, classString);
    }

    public static void storeSelectedClasses(final List<String> selectedClasses) {
        final IPreferenceStore store = getPrefStore();

        String toStore = "";
        if (!selectedClasses.isEmpty()) {
            toStore = PluginUtil.join(";", selectedClasses);
        }

        store.setValue(CheckerPreferences.PREF_CHECKER_SELECTED_CHECKERS, toStore);
    }

    public static List<String> getSelectedClasses() {
        final IPreferenceStore store = getPrefStore();
        String selectedStr = store.getString(CheckerPreferences.PREF_CHECKER_SELECTED_CHECKERS);
        if (selectedStr == null || selectedStr.trim().isEmpty()) {
            return new ArrayList<String>();
        }

        return Arrays.asList(selectedStr.split(";"));
    }
}
