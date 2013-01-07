package checkers.eclipse.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static class Holder { //TODO: REMOVE
        private static final CheckerManager INSTANCE = new CheckerManager();
    }

    /**
     * get the static instance of the manager
     * 
     * @return the static instance
     */
    public static CheckerManager getInstance() {
        return Holder.INSTANCE;
    }

    public List<CheckerInfo> getCheckerInfos() {
    	return CheckerInfo.checkers;
    }

    /**
     * Check to see which classes have been selected and return those that have
     * been selected
     * 
     * @return a list of classes to run
     */
    public List<String> getSelectedNames()
    {
        List<String> selected = new ArrayList<String>();

        IPreferenceStore store = getPrefStore();


        for (CheckerInfo processor : CheckerInfo.checkers) {
            String label = processor.getLabel();
            boolean selection = store.getBoolean(label);
            if (selection)
                selected.add(processor.getClassName());
        }

        return selected;
    }

    /**
     * Check to see which classes have been selected and return the quals paths
     * of those that are selected.
     * 
     * @return a list of quals paths to use as imports
     */
    public List<String> getSelectedQuals() {
        List<String> selected = new ArrayList<String>();

        IPreferenceStore store = getPrefStore();

        for (CheckerInfo processor : CheckerInfo.checkers) {
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
