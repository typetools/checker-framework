package utilMDE;

import java.io.*;
import java.util.Iterator;

// This code was originally taken from
// http://www.onjava.com/pub/a/onjava/2005/04/20/javaIAN5.html, then
// modified.
// It's been modified enough that I should just re-write from scratch.

// A problem with this class is that it is difficult to give good error
// messages; for instance, it doesn't provide a good method for computing
// the current line number, or the file name.  Perhaps have a pointer from
// the TextFile to its corresponding iterator (and only permit one iterator
// at a time to exist).  Then one could add a close method, and perhaps
// other methods as well.
// Idea: why doesn'this implement both Iterable and Iterator?  Then the
// iterator() method could just returh "this".

/**
 * TextFile implements Iterable&lt;String&gt; allowing text files to be
 * iterated line by line with the for/in loop. You might use it with code
 * like this:
 *
 * <pre>
 *   TextFile textfile = new TextFile(filename, "UTF-8");
 *   int lineNumber = 0;
 *   for (String line : textfile)
 *       System.out.printf("%6d: %s%n", ++lineNumber, line);
 * </pre>
 * The iterator makes no attempt to detect
 * concurrent modifications to the underlying file. If you want to do that
 * yourself, take a look at java.nio.channels.FileLock.
 **/

public class TextFile implements Iterable<String> {
    InputStream is;
    String charsetName;

    public TextFile(String filename)
	throws IOException
    {
        this(filename, null);
    }

    public TextFile(String filename, String charsetName)
	throws IOException
    {
        this(new File(filename), charsetName);
    }

    public TextFile(File f)
	throws IOException
    {
        this(f, null);
    }

    public TextFile(File f, String charsetName)
	throws IOException
    {
	if (!f.exists())
	    throw new FileNotFoundException(f.getPath());
	if (!f.canRead())
	    throw new IOException("Can't read: " +
				  f.getPath());
        // Not "this(new FileInputStream(f), charsetName);"
        // because a call to "this" must be the first thing in a constructor.
        this.is = new FileInputStream(f);
        this.charsetName = charsetName;
    }

    public TextFile(InputStream is)
    {
        this(is, null);
    }

    public TextFile(InputStream is, String charsetName)
    {
        this.is = is;
	this.charsetName = charsetName;
    }

    public Iterator<String> iterator() {
	try {
	    return new TextFileIterator(is, charsetName);
	}
	catch(IOException e) {
	    throw new IllegalArgumentException(e);
	}
    }


    static class TextFileIterator
	implements Iterator<String>
    {
	LineNumberReader in;
	String nextline;
	boolean closed = false;

	public TextFileIterator(File f, String charsetName)
	    throws IOException
	{
	    this(new FileInputStream(f), charsetName);
        }

	public TextFileIterator(InputStream is, String charsetName)
	    throws IOException
	{
	    Reader isr;
            if (charsetName == null) {
                isr = new InputStreamReader(is);
            } else {
                isr = new InputStreamReader(is, charsetName);
            }
	    in = new LineNumberReader(isr);
	    getNextLine();
	}

	public boolean hasNext() {
	    return nextline != null;
	}

	public String next() {
	    String returnValue = nextline;
	    getNextLine();
	    return returnValue;
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}

	void getNextLine() {
	    if (!closed) {
		try { nextline = in.readLine(); }
		catch(IOException e) {
		    throw new IllegalArgumentException(e);
		}
		if (nextline == null) {
		    try { in.close(); }
		    catch(IOException ignored) {}
		    closed = true;
		}
	    }
	}
    }
}
