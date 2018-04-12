package ciir.yggdrasil.galago;

import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.index.mem.FlushToDisk;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.FSUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
* @author jfoley.
*/
public class TemporaryGalagoIndex implements Closeable {
  private File path;
  private LocalRetrieval retrieval;

  public TemporaryGalagoIndex(MemoryIndex input) throws Exception {
    this.path = FileUtility.createTemporaryDirectory();
    FlushToDisk.flushMemoryIndex(input, path.getAbsolutePath());
    this.retrieval = new LocalRetrieval(path.getAbsolutePath());
  }

  public void saveTo(String output) {
    try {
      Process exec = Runtime.getRuntime().exec(new String[] {"/bin/cp", "-R", path.getAbsolutePath(), output});
      int status = exec.waitFor();
      System.err.println(status);
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public LocalRetrieval getRetrieval() {
    return retrieval;
  }

  public DiskIndex getIndex() {
    return (DiskIndex) retrieval.getIndex();
  }

  @Override
  public void close() throws IOException {
    retrieval.close();
    FSUtil.deleteDirectory(path);
    path = null;
    retrieval = null;
  }

  public static MemoryIndex memoryIndexBuilder() {
    try {
      return new MemoryIndex(Parameters.parseArray("makecorpus", true));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
