package ciir.yggdrasil.util;

import au.com.bytecode.opencsv.CSVReader;
import ciir.jfoley.chai.io.IO;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.json.JSONUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jfoley.
 */
public class TSVLoader {
  /*public static List<Parameters> withHeader(String file) {
    final List<Parameters> tsvData = new ArrayList<>();

    IO.forEachLine(new File(file), new IO.StringFunctor() {
      List<String> columnTitles = new ArrayList<>();

      @Override
      public void process(String input) {
        String[] data = input.split("\t");
        if (columnTitles.isEmpty()) {
          columnTitles.addAll(Arrays.asList(data));
          return;
        }
        Parameters p = Parameters.create();
        for (int i = 0; i < data.length; i++) {
          p.put(columnTitles.get(i), JSONUtil.parseString(data[i]));
        }
        tsvData.add(p);
      }
    });

    return tsvData;
  }*/

  public static List<Parameters> withHeader(String file) {
    try (CSVReader reader = new CSVReader(IO.openReader(file), '\t')) {

      String[] columnTitles = reader.readNext();
      if (columnTitles == null) return Collections.emptyList();

      ArrayList<Parameters> output = new ArrayList<>();

      while(true) {
        String[] data = reader.readNext();
        if(data == null) break;
        Parameters p = Parameters.create();
        for (int i = 0; i < data.length; i++) {
          p.put(columnTitles[i], JSONUtil.parseString(data[i]));
        }
        output.add(p);
      }

      return output;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
