
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.Iterator;
import java.util.Set;

public class Pom {

    // These are now 0-20, 21-40, 41-60, 61-80, 81+ but filenames unchanged for compatibility
    private static final String HEALTH_OVER_80 = "icon-health-80plus";
    private static final String HEALTH_61_TO_80 = "icon-health-60to79";
    private static final String HEALTH_41_TO_60 = "icon-health-40to59";
    private static final String HEALTH_21_TO_40 = "icon-health-20to39";
    private static final String HEALTH_0_TO_20 = "icon-health-00to19";

    private static final String HEALTH_OVER_80_IMG = "health-80plus.png";
    private static final String HEALTH_61_TO_80_IMG = "health-60to79.png";
    private static final String HEALTH_41_TO_60_IMG = "health-40to59.png";
    private static final String HEALTH_21_TO_40_IMG = "health-20to39.png";
    private static final String HEALTH_0_TO_20_IMG = "health-00to19.png";
    private String iconClassName;
    protected transient /*almost final*/ RunMap<RunT> runs = new RunMap<RunT>();
    private transient boolean notLoaded = true;
    private transient long nextUpdate = 0;
    private transient volatile boolean reloadingInProgress;
    private static ReloadThread reloadThread;
    //private static org.jruby.ext.posix.POSIX jnaPosix;
    private boolean ignoreBase;

    //ADDED BY KOBI
    public void runAll() {
        new HealthReport(1, "iconUrl", new Localizable());
        new ViewJob()._getRuns();
        // get();
        limit(null, null);
    }

    //SNIPPET_STARTS
    public class HealthReport implements Serializable, Comparable<HealthReport> {
        private String iconClassName;
        private int score;
        private String iconUrl;
        private transient String description;
        private Localizable localizibleDescription;

        public void setLocalizibleDescription(Localizable localizibleDescription) {
            this.localizibleDescription = localizibleDescription;
        }

        @Override
        public int compareTo(HealthReport o) {
            return 0;
        }

        // hudson.model.HealthReport.HealthReport(int,java.lang.String,org.jvnet.localizer.Localizable)
        /**
         * Create a new HealthReport.
         *
         * @param score       The percentage health score (from 0 to 100 inclusive).
         * @param iconUrl     The path to the icon corresponding to this 's health or <code>null</code> to
         *                    display the default icon corresponding to the current health score.
         *                    <p/>
         *                    If the path begins with a '/' then it will be the absolute path, otherwise the image is
         *                    assumed to be in one of <code>/images/16x16/</code>, <code>/images/24x24/</code> or
         *                    <code>/images/32x32/</code> depending on the icon size selected by the user.
         *                    When calculating the url to display for absolute paths, the getIconUrl(String) method
         *                    will replace /32x32/ in the path with the appropriate size.
         * @param description The health icon's tool-tip.
         */
        public HealthReport(int score, String iconUrl, Localizable description) {
            this.score = score;
            if (score <= 20) {
                this.iconClassName = HEALTH_0_TO_20;
            } else if (score <= 40) {
                this.iconClassName = HEALTH_21_TO_40;
            } else if (score <= 60) {
                this.iconClassName = HEALTH_41_TO_60;
            } else if (score <= 80) {
                this.iconClassName = HEALTH_61_TO_80;
            } else {
                this.iconClassName = HEALTH_OVER_80;
            }
            if (iconUrl == null) {
                if (score <= 20) {
                    this.iconUrl = HEALTH_0_TO_20_IMG;
                } else if (score <= 40) {
                    this.iconUrl = HEALTH_21_TO_40_IMG;
                } else if (score <= 60) {
                    this.iconUrl = HEALTH_41_TO_60_IMG;
                } else if (score <= 80) {
                    this.iconUrl = HEALTH_61_TO_80_IMG;
                } else {
                    this.iconUrl = HEALTH_OVER_80_IMG;
                }
            } else {
                this.iconUrl = iconUrl;
            }
            this.description = null;
            setLocalizibleDescription(description);
        }
    }

    //SNIPPET_STARTS
    private class ViewJob {
        // hudson.model.ViewJob._getRuns()
        protected SortedMap<Integer,RunT> _getRuns() {
            if(notLoaded || runs==null) {
                // if none is loaded yet, do so immediately.
                synchronized(this) {
                    if(runs==null)
                        runs = new RunMap<RunT>();
                    if(notLoaded) {
                        notLoaded = false;
                        _reload();
                    }
                }
            }
            if(nextUpdate<System.currentTimeMillis()) {
                if(!reloadingInProgress) {
                    // schedule a new reloading operation.
                    // we don't want to block the current thread,
                    // so reloading is done asynchronously.
                    reloadingInProgress = true;
                    Set<ViewJob> reloadQueue;
                    synchronized (ViewJob.class) {
                        if (reloadThread == null) {
                            reloadThread = new ReloadThread();
                            reloadThread.start();
                        }
                        reloadQueue = reloadThread.reloadQueue;
                    }
                    synchronized(reloadQueue) {
                        reloadQueue.add(this);
                        reloadQueue.notify();
                    }
                }
            }
            return runs;
        }
    }

    // hudson.os.PosixAPI.get()
    //SNIPPET_STARTS
    /*@Deprecated
    public static synchronized org.jruby.ext.posix.POSIX get() {
        if (jnaPosix == null) {
            jnaPosix = org.jruby.ext.posix.POSIXFactory.getPOSIX(new Pom.POSIXHandler() { // Change POSIXHandler signature
                public void error(ERRORS errors, String s) throws PosixException { // changed ERRORS signature, added throws
                    throw new PosixException(s,errors);
                }

                public void unimplementedError(String s) {
                    throw new UnsupportedOperationException(s);
                }

                public void warn(WARNING_ID warning_id, String s, Object... objects) {
                    LOGGER.fine(s);
                }

                public boolean isVerbose() {
                    return true;
                }

                public File getCurrentWorkingDirectory() {
                    return new File(".").getAbsoluteFile();
                }

                public String[] getEnv() {
                    Map<String,String> envs = System.getenv();
                    String[] envp = new String[envs.size()];

                    int i = 0;
                    for (Map.Entry<String,String> e : envs.entrySet()) {
                        envp[i++] = e.getKey()+'+'+e.getValue();
                    }
                    return envp;
                }

                public InputStream getInputStream() {
                    return System.in;
                }

                public PrintStream getOutputStream() {
                    return System.out;
                }

                public int getPID() {
                    // TODO
                    return 0;
                }

                public PrintStream getErrorStream() {
                    return System.err;
                }
            }, true);
        }
        return jnaPosix;
    }*/

    // hudson.util.Iterators.limit(java.util.Iterator<? extends T>,hudson.util.Iterators.CountingPredicate<? super T>)
    /**
     * Returns the elements in the base iterator until it hits any element that doesn't satisfy the filter.
     * Then the rest of the elements in the base iterator gets ignored.
     *
     * @since 1.485
     */
    //SNIPPET_STARTS
    public static <T> Iterator<T> limit(final Iterator<? extends T> base, final CountingPredicate<? super T> filter) {
        return new Iterator<T>() {
            private T next;
            private boolean end;
            private int index=0;
            public boolean hasNext() {
                fetch();
                return next!=null;
            }

            public T next() {
                fetch();
                T r = next;
                next = null;
                return r;
            }

            private void fetch() {
                if (next==null && !end) {
                    if (base.hasNext()) {
                        next = base.next();
                        if (!filter.apply(index++,next)) {
                            next = null;
                            end = true;
                        }
                    } else {
                        end = true;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // jenkins.util.AntClassLoader.loadClass(java.lang.String,boolean)
    /**
     * Loads a class with this class loader.
     *
     * This class attempts to load the class in an order determined by whether
     * or not the class matches the system/loader package lists, with the
     * loader package list taking priority. If the classloader is in isolated
     * mode, failure to load the class in this loader will result in a
     * ClassNotFoundException.
     *
     * @param classname The name of the class to be loaded.
     *                  Must not be <code>null</code>.
     * @param resolve <code>true</code> if all classes upon which this class
     *                depends are to be loaded.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     * on the system classpath (when not in isolated mode) or this loader's
     * classpath.
     */
    //SNIPPET_STARTS
    protected synchronized Class loadClass(String classname, boolean resolve)
            throws ClassNotFoundException {
        // 'sync' is needed - otherwise 2 threads can load the same class
        // twice, resulting in LinkageError: duplicated class definition.
        // findLoadedClass avoids that, but without sync it won't work.

        Class theClass = findLoadedClass(classname);
        if (theClass != null) {
            return theClass;
        }
        if (isParentFirst(classname)) {
            try {
                theClass = findBaseClass(classname);
                log("Class " + classname + " loaded from parent loader " + "(parentFirst)",
                        Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                theClass = findClass(classname);
                log("Class " + classname + " loaded from ant loader " + "(parentFirst)",
                        Project.MSG_DEBUG);
            }
        } else {
            try {
                theClass = findClass(classname);
                log("Class " + classname + " loaded from ant loader", Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                if (ignoreBase) {
                    throw cnfe;
                }
                theClass = findBaseClass(classname);
                log("Class " + classname + " loaded from parent loader", Project.MSG_DEBUG);
            }
        }
        if (resolve) {
            resolveClass(theClass);
        }
        return theClass;
    }
    //SNIPPETS_END

    private Class findClass(String classname) throws ClassNotFoundException{
        return null;
    }

    private boolean isParentFirst(String classname) {
        return false;
    }

    private Class findBaseClass(String classname) throws ClassNotFoundException{
        return null;
    }

    private void log(String s, Object msgDebug) {

    }

    private void resolveClass(Class theClass) {

    }

    private Class findLoadedClass(String classname) {
        return null;
    }

    private static class ReloadThread {
        public Set<ViewJob> reloadQueue;

        public void start() {

        }
    }

    private void _reload() {

    }
    private class Localizable {
    }

    private class RunT {
    }

    private class RunMap<T> implements SortedMap {
        @Override
        public Comparator comparator() {
            return null;
        }

        @Override
        public SortedMap subMap(Object fromKey, Object toKey) {
            return null;
        }

        @Override
        public SortedMap headMap(Object toKey) {
            return null;
        }

        @Override
        public SortedMap tailMap(Object fromKey) {
            return null;
        }

        @Override
        public Object firstKey() {
            return null;
        }

        @Override
        public Object lastKey() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map m) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Set keySet() {
            return null;
        }

        @Override
        public Collection values() {
            return null;
        }

        @Override
        public Set<Entry> entrySet() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class LOGGER {
        public static void fine(String s) {

        }
    }



    private static class PosixException extends Throwable {
        public PosixException(String s, ERRORS errors) {
        }
    }

    private static class Errno { }

    private static class ERRORS extends Errno {
    }

    public static class POSIXHandler<WARNING_ID> /*implements org.jruby.ext.posix.POSIXHandler*/ {
        //@Override
        public void error(Errno errno, String s) {

        }

        //@Override
        public void unimplementedError(String s) {

        }

        //@Override
        public void warn(WARNING_ID warning_id, String s, Object... objects) {

        }

        //@Override
        public boolean isVerbose() {
            return false;
        }

        //@Override
        public File getCurrentWorkingDirectory() {
            return null;
        }

        //@Override
        public String[] getEnv() {
            return new String[0];
        }

        //@Override
        public InputStream getInputStream() {
            return null;
        }

        //@Override
        public PrintStream getOutputStream() {
            return null;
        }

        //@Override
        public int getPID() {
            return 0;
        }

        //@Override
        public PrintStream getErrorStream() {
            return null;
        }

        public void debug(String s, Object jdbcUrl) {

        }

        public void debug(String no_events_to_process_) {

        }

        public void error(String exception_while_persisting_to_hbase_, SQLException e) {

        }

        public void error(String s, Throwable e) {

        }

        public void info(String format) {

        }

        public void error(String s, String keyGeneratorType, Object values) {

        }
    }

    private static class CountingPredicate<T> {
        public boolean apply(int i, T next) {
            return false;
        }
    }

    private static class Project {
        public static final Object MSG_DEBUG = "";
    }
}
