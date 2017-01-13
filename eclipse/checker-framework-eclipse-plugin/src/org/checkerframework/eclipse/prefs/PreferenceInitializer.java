package org.checkerframework.eclipse.prefs;

import org.checkerframework.eclipse.CheckerPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public PreferenceInitializer() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefs = CheckerPlugin.getDefault().getPreferenceStore();
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_PREFS_SET, false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_ARGS, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_LINT, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_WARNS, true);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_FILENAMES, false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT, false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS, false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_AUTO_BUILD, /*true*/ false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS, false);
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_ERROR_FILTER_REGEX, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_JDK_PATH, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES, "");
        prefs.setDefault(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD, false);
    }
}
