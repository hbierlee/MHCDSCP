package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import util.Pair;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Contains tumor and normal data.
 * In particular, when doing a train/test split this is either the train or the test split.
 */
public class Instance {
	private String name, writeToFileName;
	private Data dataNormal;
	private Data dataTumor;
	
	public Instance(String name, Data dataNormal, Data dataTumor) {
		this.name = name;
		this.writeToFileName = name+"_"+GlobalParam.MIN_HIT_SIZE+"-"+GlobalParam.MAX_HIT_SIZE;
		this.dataNormal = dataNormal;
		this.dataTumor = dataTumor;
	}

	public String getWriteToFileName() {
		return writeToFileName;
	}

	public String getName() {
		return name;
	}

	public Data getDataNormal() {
		return dataNormal;
	}

	public Data getDataTumor() {
		return dataTumor;
	}
	
	public double calculateObjectiveTumor(Collection<List<Integer>> combinationList) {
		// Tumor samples
	    int numTumorCovered = 0;
 		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
 			boolean coversSample = false;
 			for(List<Integer> combination: combinationList) {
 				if(coversSample) {
 					break;
 				}
 				coversSample = dataTumor.geneCombnationCoversSample(combination, i);
 			}
 			if(coversSample) {
 				numTumorCovered++;
 			}
 		}
 		return numTumorCovered;
	}
	
	public double calculateObjectiveNormal(Collection<List<Integer>> combinationList) {
 		// Normal samples
 		// Count the number of times a sample is covered
 		int numNormalCovered = 0;
 		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
 			int coversSample = 0;
 			for(List<Integer> combination: combinationList) {
 				if(dataNormal.geneCombnationCoversSample(combination, i)) {
 					coversSample++;
 				}
 				// If you reach this, it means that combination covered sample i. In other words, all genes in combination were also mutated in sample.
 			}
 			if(coversSample>0) {
 				numNormalCovered+=coversSample;
 			}
 		}
 		return numNormalCovered;
	}
	
	public double calculateObjective(Collection<List<Integer>> combinationList) {		
 		double objective = calculateObjectiveTumor(combinationList) - GlobalParam.ALPHA * calculateObjectiveNormal(combinationList);
 		return objective;
	}

	public static Instance readFromFile(String folderName, String fileName) {
		String[] fileNames = {"-normal", "-tumor"};
		Data dataNormal = readData(folderName+"/"+fileName+"/"+fileName+fileNames[0]);
		Data dataTumor = readData(folderName+"/"+fileName+"/"+fileName+fileNames[1]);
		
		return new Instance(fileName, dataNormal, dataTumor);
	}
	
	public static Data readData(String fileName) {
		try (BufferedReader br = new BufferedReader(new FileReader(fileName+".csv"))) {
			String line;
			String[] values;
			
			// Header
			line = br.readLine();
			values = line.split(",");
			int numGenes = Integer.valueOf(values[0]);
			int numSamples = Integer.valueOf(values[1]);
			
			// Read in data
			boolean[][] mat = new boolean[numGenes][numSamples];
			String[] geneNames = new String[numGenes];
			int i = 0;
            while ((line = br.readLine()) != null) {
                // Split the line by comma (basic CSV parsing)
                values = line.split(",");
                
                // GeneName
                geneNames[i] = values[1];
                
                // Matrix
                for(int j = 0; j < numSamples; j++) {
                	mat[i][j] = Integer.valueOf(values[j+2])==1 ? true:false;
                }
                i++;
            }
            
            Data data = new Data(fileName, numGenes, numSamples, geneNames, mat);
            System.out.println(data);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
		return null;
	}
	
	/**
	 * pair.first = train
	 * pair.second = test
	 * 
	 * @param instance
	 * @param percTest
	 * @return
	 */
	public static Pair<Instance, Instance> splitTrainTestInstance(Instance instance, double percTest) {
		Pair<Data, Data> tumorPair = Data.splitTrainTestData(instance.getDataTumor(), percTest);
		Pair<Data, Data> normalPair = Data.splitTrainTestData(instance.getDataNormal(), percTest);
		
		Instance trainInstance = new Instance(instance.getName()+"_train", normalPair.first, tumorPair.first);
		Instance testInstance = new Instance(instance.getName()+"_test", normalPair.second, tumorPair.second);
		
		return new Pair<>(trainInstance, testInstance);
		
	}
	
	public static Pair<String, Pair<Integer, Integer>> readInstanceSettingAndSetGlobalParam(String fileName) {
		Pair<String, Pair<Integer, Integer>> setting = readInstanceSetting(fileName);
		
		GlobalParam.MIN_HIT_SIZE = setting.second.first;
		GlobalParam.MAX_HIT_SIZE = setting.second.second;
		System.out.println("Set MIN_HIT_SIZE: " + GlobalParam.MIN_HIT_SIZE);
		System.out.println("Set MAX_HIT_SIZE: " + GlobalParam.MAX_HIT_SIZE);
		return setting;
	}
	
	public static Pair<String, Pair<Integer, Integer>> readInstanceSetting(String fileName) {
		fileName = fileName.replace(".txt", "");
		String[] values = fileName.split("_");
		String name;
		String setting;
		if(values.length==2) {
			name = values[0];
			setting = values[1];
		}
		else {
			name = values[0] + "_" + values[1];
			setting = values[2];
		}
		
		if(setting.contains("-")) {
			String[] values2 = setting.split("-");
			return new Pair<>(name, new Pair<>(Integer.valueOf(values2[0]), Integer.valueOf(values2[1])));
		}
		else {
			return new Pair<>(name, new Pair<>(Integer.valueOf(setting), Integer.valueOf(setting)));
		}
	}
}
