/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.common.csv;

import com.opencsv.bean.CsvToBeanBuilder;
import edu.mayo.kmdp.language.common.csv.CSVExtractorConfig.CSVExtractorOptions;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVExtractor {

  private static final Logger logger = LoggerFactory.getLogger(CSVExtractor.class);

  public Optional<byte[]> convertExcelToCSV(InputStream source,
      CSVExtractorConfig cfg) {

    if (source == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(this.convertToCSV(new XSSFWorkbook(source),
          cfg.getTyped(CSVExtractorOptions.SHEET),
          cfg.getTyped(CSVExtractorOptions.COLUMNS),
          cfg.getTyped(CSVExtractorOptions.SEPARATOR),
          cfg.getTyped(CSVExtractorOptions.AS_TEXT)));
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  public <T> Optional<List<T>> convertExcelToBeans(final InputStream source,
      final CSVExtractorConfig cfg, final Class<T> klass) {
    return convertExcelToCSV(source, cfg)
        .map(ByteArrayInputStream::new)
        .map(csv -> new ArrayList<>(new CsvToBeanBuilder<T>(new InputStreamReader(csv))
            .withType(klass)
            .withSeparator(getSeparatorStr(cfg).charAt(0))
            .build()
            .parse()));

  }

  private String getSeparatorStr(CSVExtractorConfig cfg) {
    return cfg.getTyped(CSVExtractorOptions.SEPARATOR);
  }

  private byte[] convertToCSV(
      XSSFWorkbook workbook, int index, String columnRange, String separator, boolean asText) {
    final Sheet sheet = workbook.getSheetAt(index);
    final FormulaEvaluator evaluator = new XSSFFormulaEvaluator(workbook);
    int[] colRange = parseColumnRange(columnRange)
        .orElseGet(() -> new int[]{0, sheet.getRow(0).getPhysicalNumberOfCells()});
    return saveCSVFile(IntStream.range(0, sheet.getLastRowNum() + 1)
            .mapToObj(j -> rowToCSV(sheet.getRow(j), colRange, evaluator, asText))
            .collect(Collectors.toList()),
        separator);
  }

  private Optional<int[]> parseColumnRange(String columnRange) {
    if (Util.isEmpty(columnRange)) {
      return Optional.empty();
    }
    String[] s = columnRange.split(":");
    try {
      var low = Integer.parseInt(s[0].trim());
      var hig = Integer.parseInt(s[1].trim());
      return Optional.of(new int[]{low, hig});
    } catch (Exception e) {
      logger.trace(e.getMessage());
      return Optional.empty();
    }
  }

  private byte[] saveCSVFile(final List<List<String>> csvData, final String separator) {
    return csvData.stream()
        .map(line -> String.join(separator, line))
        .collect(Collectors.joining(System.lineSeparator()))
        .getBytes();
  }


  private List<String> rowToCSV(Row row, int[] range, FormulaEvaluator evaluator,
      boolean asText) {
    if (row == null) {
      return Collections.emptyList();
    }
    return IntStream.range(range[0], range[1])
        .mapToObj(row::getCell)
        .map(xell -> xell != null ? evaluate(xell, evaluator, asText) : "")
        .collect(Collectors.toList());
  }

  private String evaluate(Cell cell, FormulaEvaluator evaluator, boolean asText) {
    switch (cell.getCellType()) {
      case BLANK:
        return "";
      case STRING:
        return cell.getStringCellValue();
      case BOOLEAN:
        return cell.getBooleanCellValue() ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
      case NUMERIC:
        Number num = cell.getNumericCellValue();
        return "" + (asText ? "" + num.longValue() : num);
      case FORMULA:
        return evaluator.evaluate(cell).getStringValue();
      default:
        throw new UnsupportedOperationException(
            "Unexpected type of cell " + cell.getCellType().name());
    }
  }

}