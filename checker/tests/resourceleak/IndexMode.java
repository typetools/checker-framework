// A test for a new false positive issued in release 3.36.0 but not 3.35.0.
// Reported as part of https://github.com/typetools/checker-framework/issues/6077.

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class IndexMode
{
  public static final IndexMode NOT_INDEXED = new IndexMode(Mode.PREFIX, true, false, NonTokenizingAnalyzer.class, 0);

  private static class NonTokenizingAnalyzer {

  }

  private static final String INDEX_MODE_OPTION = "mode";
  private static final String INDEX_ANALYZED_OPTION = "analyzed";
  private static final String INDEX_ANALYZER_CLASS_OPTION = "analyzer_class";
  private static final String INDEX_IS_LITERAL_OPTION = "is_literal";
  private static final String INDEX_MAX_FLUSH_MEMORY_OPTION = "max_compaction_flush_memory_in_mb";
  private static final double INDEX_MAX_FLUSH_DEFAULT_MULTIPLIER = 0.15;
  private static final long DEFAULT_MAX_MEM_BYTES = (long) (1073741824 * INDEX_MAX_FLUSH_DEFAULT_MULTIPLIER); // 1G default for memtable

  public final Mode mode;
  public final boolean isAnalyzed, isLiteral;
  public final Class analyzerClass;
  public final long maxCompactionFlushMemoryInBytes;

  private IndexMode(Mode mode, boolean isLiteral, boolean isAnalyzed, Class analyzerClass, long maxMemBytes)
  {
    this.mode = mode;
    this.isLiteral = isLiteral;
    this.isAnalyzed = isAnalyzed;
    this.analyzerClass = analyzerClass;
    this.maxCompactionFlushMemoryInBytes = maxMemBytes;
  }

  public static class ColumnMetadata {
    public AbstractType<?> cellValueType() { return null; }
  }

  public static class AbstractType<T> {

  }

  public static class UTF8Type extends AbstractType<UTF8Type> {

  }

  public static class AsciiType extends AbstractType<AsciiType> {

  }

  public static IndexMode getMode(ColumnMetadata column, Map<String, String> indexOptions) throws ConfigurationException
  {
    if (indexOptions == null || indexOptions.isEmpty())
      return IndexMode.NOT_INDEXED;

    Mode mode;

    try
    {
      mode = indexOptions.get(INDEX_MODE_OPTION) == null
          ? Mode.PREFIX
          : Mode.mode(indexOptions.get(INDEX_MODE_OPTION));
    }
    catch (IllegalArgumentException e)
    {
      throw new ConfigurationException("Incorrect index mode: " + indexOptions.get(INDEX_MODE_OPTION));
    }

    boolean isAnalyzed = false;
    Class analyzerClass = null;
    try
    {
      if (indexOptions.get(INDEX_ANALYZER_CLASS_OPTION) != null)
      {
        analyzerClass = Class.forName(indexOptions.get(INDEX_ANALYZER_CLASS_OPTION));
        isAnalyzed = indexOptions.get(INDEX_ANALYZED_OPTION) == null
            ? true : Boolean.parseBoolean(indexOptions.get(INDEX_ANALYZED_OPTION));
      }
      else if (indexOptions.get(INDEX_ANALYZED_OPTION) != null)
      {
        isAnalyzed = Boolean.parseBoolean(indexOptions.get(INDEX_ANALYZED_OPTION));
      }
    }
    catch (ClassNotFoundException e)
    { }

    boolean isLiteral = false;
    try
    {
      String literalOption = indexOptions.get(INDEX_IS_LITERAL_OPTION);
      AbstractType<?> validator = column.cellValueType();

      isLiteral = literalOption == null
          ? (validator instanceof UTF8Type || validator instanceof AsciiType)
          : Boolean.parseBoolean(literalOption);
    }
    catch (Exception e)
    {
    }

    long maxMemBytes = indexOptions.get(INDEX_MAX_FLUSH_MEMORY_OPTION) == null
        ? DEFAULT_MAX_MEM_BYTES
        : 1048576L * Long.parseLong(indexOptions.get(INDEX_MAX_FLUSH_MEMORY_OPTION));

    if (maxMemBytes > 100L * 1073741824)
    {
      maxMemBytes = DEFAULT_MAX_MEM_BYTES;
    }
    return new IndexMode(mode, isLiteral, isAnalyzed, analyzerClass, maxMemBytes);
  }

  public enum Mode {
    PREFIX, OTHER;

    static Mode mode(String s) { return OTHER; }
  }

  public static class ConfigurationException extends Exception {
    public ConfigurationException(String s) {

    }
  }
}
