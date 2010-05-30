import checkers.nullness.quals.*;

public class AssertIfTrueTest {

    static String killfile_filter;

    public static void fromDir(File1 dbdir){
        if (!dbdir.isDirectory()) {
            throw new Error("Not a directory: " + dbdir);
        }
        File1 /*@NonNull*/ [] files = dbdir.listFiles(killfile_filter);
    }


    ///////////////////////////////////////////////////////////////////////////
    /// Classes copied from the annotated JDK
    ///

    public class File1 {
        @AssertNonNullIfTrue({"list()","list(String)","listFiles()","listFiles(String)","listFiles(Double)"})
            public boolean isDirectory() { throw new RuntimeException("skeleton method"); }

        public String @Nullable [] list() { throw new RuntimeException("skeleton method"); }
        public String @Nullable [] list(@Nullable String FilenameFilter_a1) { throw new RuntimeException("skeleton method"); }
        public File1 @Nullable [] listFiles() { throw new RuntimeException("skeleton method"); }
        public File1 @Nullable [] listFiles(@Nullable String FilenameFilter_a1) { throw new RuntimeException("skeleton method"); }

        public File1 @Nullable [] listFiles(@Nullable Double FileFilter_a1) { throw new RuntimeException("skeleton method"); }
    }

}
