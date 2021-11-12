// Based on a false positive reported on the BibTeX project

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.plumelib.util.EntryReader;
import org.plumelib.util.UtilPlume;

@SuppressWarnings("deprecation")
public final class TryWithResourcesFP {
  public static void main(String[] args) {
    for (String filename : args) {
      File inFile = new File(filename);
      File outFile = new File(inFile.getName()); // in current directory
      // Delete the file to work around a bug.  Files.newBufferedWriter (which is called by
      // UtilPlume.bufferedFileWriter) seems to have a bug where it does not correctly truncate the
      // file first.  If the target file already exists, then characters beyond what is written
      // remain in the file.
      outFile.delete();
      try (PrintWriter out = new PrintWriter(UtilPlume.bufferedFileWriter(outFile.toString()));
          EntryReader er = new EntryReader(filename)) {
      } catch (IOException e) {

      }
    }
  }
}
