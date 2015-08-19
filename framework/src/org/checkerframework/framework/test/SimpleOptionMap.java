package org.checkerframework.framework.test;

import org.checkerframework.framework.util.PluginUtil;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jburke on 7/7/15.
 */
public class SimpleOptionMap {
    private Map<String, String> options = new LinkedHashMap<>();

    public void setOptions(Map<String, String> options) {
        this.options.clear();
        this.options.putAll(options);
    }

    public void addToPathOption(String key, String toAppend) {
        if (toAppend == null) {
            throw new IllegalArgumentException("Null string appended to sourcePath.");
        }

        String path = options.get(key);

        if (toAppend.startsWith(File.pathSeparator)) {
            if (path == null || path.isEmpty()) {
                path = toAppend.substring(1, toAppend.length());
            } else {
                path += toAppend;
            }
        } else {
            if (path == null || path.isEmpty()) {
                path = toAppend;
            } else {
                path += File.pathSeparator + toAppend;
            }
        }

        addOption(key, path);
    }

    public void addOption(String option) {
        this.options.put(option, null);
    }

    public void addOption(String option, String value) {
        this.options.put(option, value);
    }


    public void addOptionIfValueNonEmpty(String option, String value) {
        if (value != null && !value.isEmpty()) {
            addOption(option, value);
        }
    }

    public void addOptions(Map<String, String> options) {
        this.options.putAll(options);
    }

    public void addOptions(Iterable<String> newOptions) {
        Iterator<String> optIter = newOptions.iterator();
        while(optIter.hasNext()) {
            String opt = optIter.next();
            if (this.options.get(opt) != null) {
                if (!optIter.hasNext()) {
                    throw new RuntimeException("Expected a value for option: " + opt
                            + " in option list: " + PluginUtil.join(", ", newOptions));
                }
                this.options.put(opt, optIter.next());

            } else {
                this.options.put(opt, null);

            }
        }
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public List<String> getOptionsAsList() {
        return TestUtilities.optionMapToList(options);
    }
}
