package org.checkerframework.eclipse.prefs;

/**
 * This class keeps static information about preferences for the checker plugin
 *
 * @author asumu
 */
public final class CheckerPreferences {
    // TODO: SHORTEN THESE AND CREATE BETTER NAMES

    /** A key for determining if individual class prefs should be checked */
    public static final String PREF_CHECKER_PREFS_SET = "checker_prefs";

    /** A key for additional arguments to pass the checker */
    public static final String PREF_CHECKER_ARGS = "checker_args";

    /** Key for classes to skip in processing */
    public static final String PREF_CHECKER_A_SKIP_CLASSES = "checker_a_skip_classes";

    /** Key for -Alint options */
    public static final String PREF_CHECKER_A_LINT = "checker_a_lint";

    /** Key for -Awarns */
    public static final String PREF_CHECKER_A_WARNS = "checker_a_warns";

    /** Key for -Afilenames */
    public static final String PREF_CHECKER_A_FILENAMES = "checker_a_filenames";

    /** Key for -Anomsgtext */
    public static final String PREF_CHECKER_A_NO_MSG_TEXT = "checker_no_msg_text";

    /** Key for -Ashowchecks */
    public static final String PREF_CHECKER_A_SHOW_CHECKS = "checker_show_checks";

    /** Key for automated build */
    public static final String PREF_CHECKER_AUTO_BUILD = "checker_auto_build";

    /** Key for implicit annotation imports */
    public static final String PREF_CHECKER_IMPLICIT_IMPORTS = "checker_implicit_imports";

    /** Key for warning/error filter regex */
    public static final String PREF_CHECKER_ERROR_FILTER_REGEX = "checker_error_filter_regex";

    /** Key for JDK executable path */
    public static final String PREF_CHECKER_JDK_PATH = "checker_jdk_path";

    /** Key for custom class string */
    public static final String PREF_CHECKER_CUSTOM_CLASSES = "checker_custom_classes";

    /** Key for option to use custom classes during autobuild */
    public static final String PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD =
            "checker_custom_class_autobuild";

    /** Key for option to use custom classes during autobuild */
    public static final String PREF_CHECKER_SELECTED_CHECKERS = "checker_selected_checkers";

    /** Where or not the plugin should output extra diagnostic info */
    public static final String PREF_CHECKER_VERBOSE = "checker_verbose";
}
