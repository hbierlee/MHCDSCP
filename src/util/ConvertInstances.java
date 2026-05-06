package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import data.Data;
import data.GlobalParam;
import ilog.concert.IloException;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Converts partially processed data to binary matrix
 * 
 * Inputs:
 * - Normal gene sample list
 * - Tumor data
 * 
 * Output:
 * - Binary matrix containing tumor data
 * - Binary matrix containing normal data
 */
public class ConvertInstances {
	private static boolean SHUFFLE = false;

	public static void main(String[] args) throws IloException, IOException {
		String inputNormalFile = "data/convertInstances/inputNormal/exampleNormalGeneList.txt";
		String inputFolder = "data/convertInstances/input";
		String outputFolder = "data/convertInstances/output";

		Data normalData = readNormalFile(new File(inputNormalFile));

		File inputDir = new File(inputFolder);
		for (File f : inputDir.listFiles()) {
			if(f.isDirectory()) {
				continue;
			}

			readFromFileAndCreateInstance(outputFolder, f, normalData);
		}
	}

	private static Data readNormalFile(File f) throws IOException {
		String line;

		Map<String, Integer> geneNames = new LinkedHashMap<>();
		List<String> geneNamesList = new ArrayList<>();
		Map<String, Integer> sampleNames = new LinkedHashMap<>();

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\\s+");

				String geneName = values[0];
				String sampleName = values[1];

				if(!geneNames.containsKey(geneName)) {
					geneNames.put(geneName, geneNames.size());
					geneNamesList.add(geneName);
				}
				if(!sampleNames.containsKey(sampleName)) {
					sampleNames.put(sampleName, sampleNames.size());
				}
			}
		}

		boolean[][] mat = new boolean[geneNames.size()][sampleNames.size()];

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\\s+");

				String geneName = values[0];
				String sampleName = values[1];

				mat[geneNames.get(geneName)][sampleNames.get(sampleName)] = true;
			}
		}

		String name = "normalSample";
		int numGenes = geneNames.size();
		int numSamples = sampleNames.size();
		String[] geneNamesArray = new String[numGenes];
		for(int i = 0; i < numGenes; i++) {
			geneNamesArray[i] = geneNamesList.get(i);
		}
		
		Data normalData = new Data(name, numGenes, numSamples, geneNamesArray, mat);

		System.out.println("NormalData " + ", numGenes: " + numGenes + ", numSamples: " + numSamples + ", numMutations: " + normalData.getNumMutations());
		return normalData;
	}

	private static void readFromFileAndCreateInstance(String outputFolder, File f, Data normalData) {
		String line;
		String[] originalValues;
		List<String> cleaned;
		String[] values;

		Map<Integer, Integer> countGenes = new LinkedHashMap<>();
		Map<Integer, String> geneMap = new LinkedHashMap<>();
		Map<String, Integer> geneMap2 = new LinkedHashMap<>();

		Map<String, Integer> normalSampleGenes = new LinkedHashMap<>();
		for(int i = 0; i < normalData.getNumGenes(); i++) {
			String s = normalData.getGeneNames()[i];
			normalSampleGenes.put(s, i);
		}

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String name = f.getName().replace(".maf2dat.tsv.matrix", "");
			name = name.replace(".txt", "");

			// Header
			line = br.readLine();
			originalValues = line.split("\\s+");

			cleaned = new ArrayList<>();
			for (String s : originalValues) {
				if (!s.isBlank()) {       // ignores " ", "   ", "\t", ""
					cleaned.add(s);
				}
			}
			values = cleaned.toArray(new String[0]);

			int numGenes = Integer.valueOf(values[0]);
			int numSamples = Integer.valueOf(values[1]);

			int[][] mat = new int[numGenes][numSamples];

			System.out.println(name + ", numGenes: " + numGenes +", numSamples: " + numSamples);

			int maxMutated = 0;
			while ((line = br.readLine()) != null) {
				// Split the line by comma (basic CSV parsing)
				originalValues = line.split("\\s+");

				cleaned = new ArrayList<>();
				for (String s : originalValues) {
					if (!s.isBlank()) {       // ignores " ", "   ", "\t", ""
						cleaned.add(s);
					}
				}
				values = cleaned.toArray(new String[0]);

				int geneID = Integer.valueOf(values[0]);
				int sampleID = Integer.valueOf(values[1]);
				String stringGeneID = values[3];
				if(!countGenes.containsKey(geneID)) {
					countGenes.put(geneID, 0);
					geneMap.put(geneID, stringGeneID);
					geneMap2.put(stringGeneID, geneID);
				}
				int mutated = Integer.valueOf(values[2]);
				if(mutated>=1) {
					mat[geneID][sampleID] = 1;
					countGenes.replace(geneID, countGenes.get(geneID)+1);	
				}
				if(mutated > maxMutated) {
					maxMutated = mutated;
				}
			}
			System.out.println("maxMutated: " + maxMutated);

			int countNonZero = 0;
			int countNonZeroAndInNormalSample = 0;
			int countMutations = 0;
			Set<String> finalGeneNames = new LinkedHashSet<>();
			for(Entry<Integer, Integer> entry: countGenes.entrySet()) {
				String geneName = geneMap.get(entry.getKey());
				if(!normalSampleGenes.containsKey(geneName) && entry.getValue()==0) {
					continue;
				}
				finalGeneNames.add(geneName);
				countNonZero++;
				countNonZeroAndInNormalSample++;
				countMutations += entry.getValue();
			}
			System.out.println("numGenes: " + countGenes.size() + ", nonZeroGenes: " + countNonZero + ", countNonZeroAndInNormalSample: " + countNonZeroAndInNormalSample + ", countMutations: " + countMutations);

			{
				String outNameNormal = outputFolder + "/" + name + "/" + name +"-normal.csv";
				List<String> outRows = new ArrayList<>();
				StringBuilder row = new StringBuilder();
				row.append(countNonZeroAndInNormalSample).append(",").append(normalData.getNumSamples());
				outRows.add(row.toString());

				int i = 0; 
				for(String s: finalGeneNames) {
					row = new StringBuilder();
					row.append(i).append(",").append(s);

					for(int j = 0; j < normalData.getNumSamples(); j++) {
						if(normalSampleGenes.containsKey(s)) {
							row.append(",").append(normalData.getMat()[normalSampleGenes.get(s)][j] ? 1:0);
						}
						else {
							row.append(",").append(0);
						}
					}
					outRows.add(row.toString());
					i++;
				}

				Path outPath = Paths.get(outputFolder, name, name + "-normal.csv");
				Files.createDirectories(outPath.getParent());

				try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outNameNormal))) {
					for (String s : outRows) bw.write(s + "\n");
				}
			}

			{
				List<Integer> indices = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
				if(SHUFFLE) {
				Collections.shuffle(indices, GlobalParam.RANDOM);
				}
				String outNameTumor = outputFolder + "/" + name + "/" + name +"-tumor.csv";
				List<String> outRows = new ArrayList<>();
				StringBuilder row = new StringBuilder();
				row.append(countNonZeroAndInNormalSample).append(",").append(numSamples);
				outRows.add(row.toString());

				int i = 0; 
				for(String s: finalGeneNames) {
					row = new StringBuilder();
					row.append(i).append(",").append(s);

					for(int j: indices) {
						row.append(",").append(mat[geneMap2.get(s)][j]);
					}
					outRows.add(row.toString());
					i++;
				}

				Path outPath = Paths.get(outputFolder, name, name + "-tumor.csv");
				Files.createDirectories(outPath.getParent());

				try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outNameTumor))) {
					for (String s : outRows) bw.write(s + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
