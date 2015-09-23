package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.After;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with the aid of .jaif files.
 *
 * @author pbsf
 */
public class JaifInferenceTest extends CheckerFrameworkTest {

    public JaifInferenceTest(File testFile) {
        super(testFile, tests.jaifinference.JaifInferenceTestChecker.class,
                "jaif-inference", "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"jaif-inference"};
    }

    @Override
    public List<String> customizeOptions(List<String> previousOptions) {
        final List<String> optionsWithStub = new ArrayList<>(checkerOptions);
        optionsWithStub.add("-AjaifFilesFolder=" + getFullPath(testFile,
                "jaif-files"));
        return optionsWithStub;
    }

    private String getFullPath(final File javaFile, final String folderName) {
        final String dirname = javaFile.getParentFile().getAbsolutePath();
        return dirname + System.getProperty("file.separator") + folderName;
    }

    /**
     * Verifies that the expected .jaif files are similar to the ones generated
     * after the tests' execution. 
     *
     * Note that before starting the tests we copy the expected .jaif files
     * to the final destination. This is done to avoid getting
     * errors in the first test execution, since without doing that it takes
     * two runs to obtain the correct final files.
     */
    @After
    public void verifyJaifFileOutput() {
        try {
            assert (areDirsEqual(new File(getFullPath(testFile,
                    "jaif-files")), new File("build/jaif-files"))):
                        "Output .jaif files are different from the expected ones.";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean areDirsEqual(File dirA,
            File dirB) throws IOException {
        if (dirA == null || dirB == null ||
                !dirA.isDirectory() || !dirB.isDirectory()) {
            return false;
        }
        File[] fileList1 = dirA.listFiles();
        File[] fileList2 = dirB.listFiles();
        if (fileList1.length != fileList2.length) {
            return false;
        }
        Arrays.sort(fileList1);
        Arrays.sort(fileList2);
        return compareArrayOfFiles(fileList1, fileList2);
    }

    private static boolean compareArrayOfFiles(File[] fileArr1,
            File[] fileArr2) throws IOException {
        for (int i = 0; i < fileArr1.length; i++) {
            File f1 = fileArr1[i];
            File f2 = fileArr2[i];
            if (f1 == null || f2 == null || !f1.getName().equals(f2.getName())) {
                System.err.println("Output .jaif folder mismatch.");
                return false;
            }

            if (f1.isDirectory()) {
                if (!areDirsEqual(f1, f2)) {
                    return false;
                }
            } else {
                String cs1 = checksum(f1);
                String cs2 = checksum(f2);
                if (!cs1.equals(cs2)) {
                    System.err.println("File " + f1.getName() +
                            " is different from the expected.");
                    return false;
                }
            }
        }
        return true;
    }

    private static String checksum(File file) {
        try  {
            InputStream fin = new FileInputStream(file);
            java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0)
                    md5er.update(buffer, 0, read);
            } while (read != -1);
            fin.close();
            byte[] digest = md5er.digest();
            if (digest == null)
                return null;
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).
                        substring(1).toUpperCase();
            }
            return strDigest;
        } catch (Exception e)  {
            return null;
        }
    }
}