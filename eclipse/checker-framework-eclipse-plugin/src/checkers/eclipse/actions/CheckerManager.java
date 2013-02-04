package checkers.eclipse.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import checkers.eclipse.prefs.CheckerPreferences;
import checkers.eclipse.util.JavaUtils;
import org.eclipse.jface.preference.IPreferenceStore;

import checkers.eclipse.CheckerPlugin;

/**
 * This class manages the current checkers that can be run. Also keeps track of
 * custom checkers that are made by the user.
 * 
 * @author asumu
 * 
 */
public class CheckerManager {

    /**
     * Singleton constructor, should only be called once for the instance
     */
    private CheckerManager() {
    }

    public static List<CheckerInfo> getCheckerInfos() {
    	return CheckerInfo.getCheckers();
    }

    /**
     * Check to see which classes have been selected and return the quals paths
     * of those that are selected.
     * 
     * @return a list of quals paths to use as imports
     */
    public static List<String> getSelectedQuals() {
        List<String> selected = new ArrayList<String>();

        IPreferenceStore store = getPrefStore();

        for (CheckerInfo processor : CheckerInfo.getCheckers()) {
            String label = processor.getLabel();
            boolean selection = store.getBoolean(label);
            if (selection)
                selected.add(processor.getQualsPath());
        }

        return selected;
    }

    public static IPreferenceStore getPrefStore() {
        return CheckerPlugin.getDefault().getPreferenceStore();
    }

    public static String [] getStoredCustomClasses() {
        final IPreferenceStore store = getPrefStore();
        final String storedItems = store.getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES);

        if(storedItems != null && !storedItems.equals("")) {
            return storedItems.split(",");
        }

        return new String[]{};
    }


    public static void storeCustomClasses(final String [] customClasses) {
        final IPreferenceStore store = getPrefStore();
        final String classString = JavaUtils.join(",", customClasses);

        store.setValue(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES, classString);
    }


    public static void storeSelectedClasses(final List<String> selectedClasses) {
        final IPreferenceStore store = getPrefStore();

        String toStore = "";
        if(!selectedClasses.isEmpty()) {
            toStore = JavaUtils.join(";", selectedClasses);
        }

        store.setValue(CheckerPreferences.PREF_CHECKER_SELECTED_CHECKERS, toStore);
    }

    public static List<String> getSelectedClasses() {
        final IPreferenceStore store = getPrefStore();
        String selectedStr = store.getString(CheckerPreferences.PREF_CHECKER_SELECTED_CHECKERS);
        if(selectedStr == null || selectedStr.trim().isEmpty()) {
            return new ArrayList<String>();
        }

        return Arrays.asList(selectedStr.split(";"));
    }

}
