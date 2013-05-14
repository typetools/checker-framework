package java.util;
import checkers.javari.quals.*;

import java.io.IOException;

public abstract class ResourceBundle {
    protected ResourceBundle parent;

    public ResourceBundle() {
        throw new RuntimeException("skeleton method");
    }

    public final String getString(String key) {
        throw new RuntimeException("skeleton method");
    }


    public final String[] getStringArray(String key) {
        throw new RuntimeException("skeleton method");
    }


    public final Object getObject(String key) {
        throw new RuntimeException("skeleton method");
    }

    public Locale getLocale(@ReadOnly ResourceBundle this) {
        throw new RuntimeException("skeleton method");
    }

    protected void setParent(ResourceBundle parent) {
        throw new RuntimeException("skeleton method");
    }

    public static final ResourceBundle getBundle(String baseName) {
        throw new RuntimeException("skeleton method");
    }

    public static final ResourceBundle getBundle(String baseName, Control control) {
        throw new RuntimeException("skeleton method");
    }

    public static final ResourceBundle getBundle(String baseName, Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public static final ResourceBundle getBundle(String baseName, Locale targetLocale, Control control) {
        throw new RuntimeException("skeleton method");
    }

    public static ResourceBundle getBundle(String baseName, Locale locale,
                                           ClassLoader loader) {
        throw new RuntimeException("skeleton method");
    }

    public static ResourceBundle getBundle(String baseName, Locale targetLocale,
                                           ClassLoader loader, Control control) {
        throw new RuntimeException("skeleton method");
    }

    public static final void clearCache() {
        throw new RuntimeException("skeleton method");
    }

    public static final void clearCache(ClassLoader loader) {
        throw new RuntimeException("skeleton method");
    }

    protected abstract Object handleGetObject(String key);

    public abstract Enumeration<String> getKeys();

    public boolean containsKey(String key) {
        throw new RuntimeException("skeleton method");
    }

    public Set<String> keySet() {
        throw new RuntimeException("skeleton method");
    }

    protected Set<String> handleKeySet() {
        throw new RuntimeException("skeleton method");
    }

    public static class Control {
        public static final List<String> FORMAT_DEFAULT;
        public static final List<String> FORMAT_CLASS;
        public static final List<String> FORMAT_PROPERTIES;
        public static final long TTL_DONT_CACHE;
        public static final long TTL_NO_EXPIRATION_CONTROL;

        protected Control() {
            throw new RuntimeException("skeleton method");
        }

        public static final Control getControl(List<String> formats) {
            throw new RuntimeException("skeleton method");
        }

        public static final Control getNoFallbackControl(List<String> formats) {
            throw new RuntimeException("skeleton method");
        }

        public List<String> getFormats(@ReadOnly Control this, String baseName) {
            throw new RuntimeException("skeleton method");
        }

        public List<Locale> getCandidateLocales(@ReadOnly Control this, String baseName, Locale locale) {
            throw new RuntimeException("skeleton method");
        }

        public Locale getFallbackLocale(@ReadOnly Control this, String baseName, Locale locale) {
            throw new RuntimeException("skeleton method");
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
            throw new RuntimeException("skeleton method");
        }

        public long getTimeToLive(@ReadOnly Control this, String baseName, Locale locale) {
            throw new RuntimeException("skeleton method");
        }

        public boolean needsReload(String baseName, Locale locale,
                                   String format, ClassLoader loader,
                                   ResourceBundle bundle, long loadTime) {
            throw new RuntimeException("skeleton method");
        }

        public String toBundleName(@ReadOnly Control this, String baseName, Locale locale) {
            throw new RuntimeException("skeleton method");
        }

        public final String toResourceName(@ReadOnly Control this, String bundleName, String suffix) {
            throw new RuntimeException("skeleton method");
        }
    }

}
