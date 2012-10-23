package checkers.eclipse.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import checkers.eclipse.CheckerPlugin;

/**
 * This class manages the current checkers that can be run. Also keeps track of
 * custom checkers that are made by the user.
 * 
 * @author asumu
 * 
 */
public class CheckerManager
{
    private List<CheckerInfo> processors;

    /**
     * Singleton constructor, should only be called once for the instance
     */
    private CheckerManager()
    {
        // add built-in checkers
        processors = new ArrayList<CheckerInfo>();
        processors.add(CheckerInfo.NULLNESS_INFO);
        processors.add(CheckerInfo.LINEAR_INFO);
        processors.add(CheckerInfo.LOCK_INFO);
        processors.add(CheckerInfo.FENUM_INFO);
        processors.add(CheckerInfo.INTERNING_INFO);
        processors.add(CheckerInfo.I18N_INFO);
        processors.add(CheckerInfo.REGEX_INFO);
        processors.add(CheckerInfo.IGJ_INFO);
        processors.add(CheckerInfo.TAINTING_INFO);
        processors.add(CheckerInfo.JAVARI_INFO);
    }

    private static class Holder
    {
        private static final CheckerManager INSTANCE = new CheckerManager();
    }

    /**
     * get the static instance of the manager
     * 
     * @return the static instance
     */
    public static CheckerManager getInstance()
    {
        return Holder.INSTANCE;
    }

    /**
     * Get the list of checkers currently registered with the manager
     * 
     * @return a list of labels for processors
     */
    public List<String> getCheckerLabels()
    {
        ArrayList<String> results = new ArrayList<String>();
        for (CheckerInfo processor : processors)
        {
            results.add(processor.getLabel());
        }

        return results;
    }

    /**
     * Get the list of checker classes to call from the compiler
     * 
     * @return list of class names
     */
    public List<String> getClassNames()
    {
        ArrayList<String> results = new ArrayList<String>();
        for (CheckerInfo processor : processors)
        {
            results.add(processor.getClassName());
        }

        return results;
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

        IPreferenceStore store = CheckerPlugin.getDefault()
                .getPreferenceStore();

        for (CheckerInfo processor : processors)
        {
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
    public List<String> getSelectedQuals()
    {
        List<String> selected = new ArrayList<String>();

        IPreferenceStore store = CheckerPlugin.getDefault()
                .getPreferenceStore();

        for (CheckerInfo processor : processors)
        {
            String label = processor.getLabel();
            boolean selection = store.getBoolean(label);
            if (selection)
                selected.add(processor.getQualsPath());
        }

        return selected;
    }
}
