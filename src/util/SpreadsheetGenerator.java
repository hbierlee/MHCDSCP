package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import data.GlobalParam;
import data.Solution;
import performanceMeasures.PerformanceMeasuresClass;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 */
public class SpreadsheetGenerator {
	public static void writeSpreadsheet(Solution solution) throws IOException {
		File output = new File(solution.getFolderFileName());
		try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = new FileOutputStream(output)) {

			// Write spreadsheet
			Sheet s = wb.createSheet();
			int rowIndex = 0;

			// Create Header
			Row headerRow = s.createRow(rowIndex++);	
			List<Object> headerRowData = new ArrayList<>();
			headerRowData.addAll(new ArrayList<>(Arrays.asList(
					"folderName", "fileName",
					"setting", 
					"colGenType",
					"originalInstanceName", "minHitSize", "maxHitSize", 
					"numGenes", "numNormalSamples", "numTumorSamples", "numNormalMutations", "numTumorMutations",
					"trainInstanceName", "numNormalSamples", "numTumorSamples",
					"validationInstanceName", "numNormalSamples", "numTumorSamples",
					"testInstanceName", "numNormalSamples", "numTumorSamples",
					"solverType", "objective", "rootnodeObj", "gap", "rootnodeGap", "modelStatus",
					"bestLP", "numRootnodeIterations",
					"timeTotalColGen", "timeAddStartColumns", "timePhase1", "timePhase2", "timeInitModel", "timeMP", "timePP",
					"colGenIter",
					"colGenNumColumnsAdded",
					"countPPStage1",
					"countPPStage2",
					"countPPBNBStage1",
					"countPPBNBStage2",
					"ppVNS_iter",
					"ppVNS_numColumns",
					"numCombinations",
					"combinationList"
					)));

			headerRowData.addAll(PerformanceMeasuresClass.getStringValues("inSample"));
			headerRowData.addAll(PerformanceMeasuresClass.getStringValues("validationSample"));
			headerRowData.addAll(PerformanceMeasuresClass.getStringValues("testSample"));

			headerRowData.addAll(new ArrayList<>(Arrays.asList(
					"TIME_LIMIT_IN_SECONDS", "MIN_HIT_SIZE", "MAX_HIT_SIZE", "TRAIN_TEST_SPLIT", "ALPHA", "BETA", "MAX_SELECT", "NUM_COMBINATIONS"
					)));

			writeCells(headerRow, headerRowData.toArray());

			Row row = s.createRow(rowIndex++);	
			List<Object> rowData = new ArrayList<>();

			rowData.addAll(new ArrayList<>(Arrays.asList(
					solution.getFolderName(),
					solution.getFileName(),
					solution.getSetting(),
					solution.getColGenType(),
					solution.getOriginalInstance().getName(),
					GlobalParam.MIN_HIT_SIZE,
					GlobalParam.MAX_HIT_SIZE,
					solution.getOriginalInstance().getDataNormal().getNumGenes(),
					solution.getOriginalInstance().getDataNormal().getNumSamples(),
					solution.getOriginalInstance().getDataTumor().getNumSamples(),
					solution.getOriginalInstance().getDataNormal().getNumMutations(),
					solution.getOriginalInstance().getDataTumor().getNumMutations(),
					solution.getTrainInstance().getName(),
					solution.getTrainInstance().getDataNormal().getNumSamples(),
					solution.getTrainInstance().getDataTumor().getNumSamples(),
					solution.getValidationInstance()==null ? "" : solution.getValidationInstance().getName(),
					solution.getValidationInstance()==null ? "" : solution.getValidationInstance().getDataNormal().getNumSamples(),
					solution.getValidationInstance()==null ? "" : solution.getValidationInstance().getDataTumor().getNumSamples(),
					solution.getTestInstance()==null ? "" : solution.getTestInstance().getName(),
					solution.getTestInstance()==null ? "" : solution.getTestInstance().getDataNormal().getNumSamples(),
					solution.getTestInstance()==null ? "" : solution.getTestInstance().getDataTumor().getNumSamples(),
					solution.getSolverType(),
					solution.getObjective(),
					solution.getRootnodeObj(),
					solution.getGap(),
					solution.getRootnodeGap(),
					solution.getModelStatus(),
					solution.getBestLP(),
					solution.getNumRootnodeIterations(),
					solution.getTimeTotalColGen(),
					solution.getTimeAddStartColumns(),
					solution.getTimePhase1(),
					solution.getTimePhase2(),
					solution.getTimeInitModel(),
					solution.getTimeMP(),
					solution.getTimePP(),
					solution.getColGenIter(),
					solution.getColGenNumColumnsAdded(),
					solution.getCountPPStage1(),
					solution.getCountPPStage2(),
					solution.getCountPPBNBStage1(),
					solution.getCountPPBNBStage2(),
					solution.getPpVNS_iter(),
					solution.getPpVNS_numColumns(),

					solution.getStringCombinationList().size(),
					solution.getStringCombinationList()
					)));

			rowData.addAll(solution.getInSamplePMC() == null ? PerformanceMeasuresClass.getNANValues() : solution.getInSamplePMC().getValues());
			rowData.addAll(solution.getValidationSamplePMC() == null ? PerformanceMeasuresClass.getNANValues() : solution.getValidationSamplePMC().getValues());
			rowData.addAll(solution.getTestSamplePMC() == null ? PerformanceMeasuresClass.getNANValues() : solution.getTestSamplePMC().getValues());

			rowData.addAll(new ArrayList<>(Arrays.asList(
					solution.getGLOBAL_TIME_LIMIT_IN_SECONDS(),
					solution.getMIN_HIT_SIZE(),
					solution.getMAX_HIT_SIZE(),
					solution.getTRAIN_TEST_SPLIT(),
					solution.getALPHA(),
					solution.getBETA(),
					solution.getMAX_SELECT(),
					solution.getNUM_COMBINATIONS()
					)));

			writeCells(row, rowData.toArray());
			wb.write(os);
		}
	}

	public static void writeSpreadsheetAverage(String fileName, List<Solution> solutionList) throws IOException {
		File output = new File(fileName);
		try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = new FileOutputStream(output)) {

			// Write spreadsheet
			Sheet s = wb.createSheet();
			int rowIndex = 0;

			List<String> headerRowData = new ArrayList<>();
			Map<String, List<Double>> columnValues = new LinkedHashMap<>();

			// initialize header names once
			try (FileInputStream fis = new FileInputStream(solutionList.get(0).getFolderFileName()); Workbook wbInput = new XSSFWorkbook(fis)) {
				Sheet sheet = wbInput.getSheetAt(0);
				Row headerRowInput = sheet.getRow(0);
				for (Cell cell : headerRowInput) {
					headerRowData.add(cell.getStringCellValue());
					columnValues.put(cell.getStringCellValue(), new ArrayList<>());
				}
			}

			// Create Header
			Row headerRow = s.createRow(rowIndex++);	
			writeCells(headerRow, headerRowData.toArray());

			for(Solution solution: solutionList) {
				try (FileInputStream fis = new FileInputStream(solution.getFolderFileName()); Workbook wbInput = new XSSFWorkbook(fis)) {

					Sheet sheet = wbInput.getSheetAt(0);

					for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
						Row row = sheet.getRow(i);
						if (row == null) continue;

						for (int j = 0; j < headerRowData.size(); j++) {
							Cell cell = row.getCell(j);
							String header = headerRowData.get(j);
							if (cell == null) continue;

							if (cell.getCellType() == CellType.NUMERIC) {
								columnValues.get(header).add(cell.getNumericCellValue());
							} else {
								// skip non-numeric values (like strings, filenames, etc.)
							}
						}
					} 
				}
			}

			// Compute averages
			Map<String, Double> averages = columnValues.entrySet().stream()
					.filter(e -> !e.getValue().isEmpty())
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN),
							(a, b) -> a, LinkedHashMap::new
							));

			// Write row
			Row row = s.createRow(rowIndex++);	
			List<Object> rowData = new ArrayList<>();

			for (int j = 0; j < headerRowData.size(); j++) {
				String header = headerRowData.get(j);
				if(header.equals("folderName")) {
					rowData.add(solutionList.get(0).getFolderName());
				}
				else if(header.equals("fileName")) {
					rowData.add(solutionList.get(0).getFileName());
				}
				else if(header.equals("originalInstanceName")) {
					rowData.add(solutionList.get(0).getOriginalInstance().getName());
				}
				else if(header.equals("fold")) {
					rowData.add(solutionList.size());
				}
				else if(averages.containsKey(header)) {
					double value = averages.get(header);
					rowData.add(value);
				}
				else {
					rowData.add("");	
				}
			}

			writeCells(row, rowData.toArray());
			wb.write(os);
		}
	}

	/**
	 * Create a row in spreadsheet
	 */
	private static void writeCells(Row row, Object... objects) {
		for (int i=0; i < objects.length; i++) {
			Cell c = row.createCell(i);
			Object o = objects[i];
			if (o == null) {
				continue;
			}
			else if (o instanceof Number) {
				Number n = (Number) o;
				c.setCellValue(n.doubleValue());
			}
			else {
				c.setCellValue(o.toString());
			}
		}
	}
}
