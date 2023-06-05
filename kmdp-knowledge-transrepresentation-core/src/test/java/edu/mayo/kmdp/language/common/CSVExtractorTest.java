/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.language.common.csv.CSVExtractor;
import edu.mayo.kmdp.language.common.csv.CSVExtractorConfig;
import edu.mayo.kmdp.language.common.csv.CSVExtractorConfig.CSVExtractorOptions;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class CSVExtractorTest {


  @Test
  void testCSVExtraction() {
    Optional<byte[]> csv = new CSVExtractor()
        .convertExcelToCSV(CSVExtractorTest.class.getResourceAsStream("/CSVExtractTestSheet.xlsx"),
            new CSVExtractorConfig().with(CSVExtractorOptions.SHEET, 2));
    assertTrue(csv.isPresent());

    try {
      CSVParser rdr = new CSVParser(new StringReader(new String(csv.orElse(new byte[0]))),
          CSVFormat.DEFAULT);
      Iterator<CSVRecord> items = rdr.iterator();

      // skip head
      assertTrue(items.hasNext());
      items.next();

      CSVRecord line = items.next();
      assertEquals(6, line.size());
      assertEquals("OntoDehydration", line.get(2));
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

}
