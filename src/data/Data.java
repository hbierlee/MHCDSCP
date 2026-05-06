package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.Pair;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Represents tumor data or normal data.
 */
public class Data {
	private String name;
	private int numGenes;
	private int numSamples;
	private int numMutations;
	private String[] geneNames;
	private boolean[][] mat;
	
	private long[][] geneBitsets; // Precompute. For faster computations
	
	public Data(String name, int numGenes, int numSamples, String[] geneNames, boolean[][] mat) {
		this.name = name;
		this.numGenes = numGenes;
		this.numSamples = numSamples;
		this.geneNames = geneNames;
		this.mat = mat;
		
		this.numMutations = 0;
		for(int i = 0 ; i < numGenes; i++) {
			for(int j = 0; j < numSamples; j++) {
				if(this.mat[i][j]) {
					this.numMutations++;
				}
			}
		}
		initBitsets();
	}
	
	private void initBitsets() {
		geneBitsets = new long[numGenes][(numSamples  + 63) / 64];

		for (int g = 0; g < numGenes; g++) {
		    for (int i = 0; i < numSamples; i++) {
		        if (mat[g][i]) {
		        	geneBitsets[g][i / 64] |= (1L << (i % 64));
		        }
		    }
		}
	}
	
	public long[][] getGeneBitsets() {
		return geneBitsets;
	}

	public int getNumMutations() {
		return numMutations;
	}

	public boolean geneCombnationCoversSample(List<Integer> combination, int i) {
		for (int geneIdx : combination) {
			if (!mat[geneIdx][i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean geneCombinationCoversSample(int[] combination, int i) {
		for (int geneIdx : combination) {
			if (!mat[geneIdx][i]) {
				return false;
			}
		}
		return true;
	}

	public String getName() {
		return name;
	}

	public int getNumGenes() {
		return numGenes;
	}

	public int getNumSamples() {
		return numSamples;
	}

	public String[] getGeneNames() {
		return geneNames;
	}

	public boolean[][] getMat() {
		return mat;
	}
	
	public static List<List<String>> convertToOriginalName(Data data, List<List<Integer>> combinationList) {
		List<List<String>> sol = new ArrayList<>();
		for(List<Integer> combination: combinationList) {
			List<String> temp = new ArrayList<>();
			for(Integer i: combination) {
				temp.add(data.getGeneNames()[i]);
			}
			sol.add(temp);
		}
		return sol;
	}
	
	public static Pair<Data, Data> splitTrainTestData(Data data, double percTest) {
		int numSamples = data.getNumSamples();
	    int numGenes = data.getNumGenes();
	    String[] geneNames = data.getGeneNames();
	    boolean[][] mat = data.getMat();
		
	    int numTest = (int) Math.round(numSamples * percTest);
	    int numTrain = numSamples - numTest;
	    
	    // Randomly shuffle sample indices
	    List<Integer> indices = new ArrayList<>();
	    for (int i = 0; i < numSamples; i++) {
	        indices.add(i);
	    }
	    Collections.shuffle(indices, GlobalParam.RANDOM);
	    
	    // Split indices into train/test lists
	    List<Integer> trainIdx = indices.subList(0, numTrain);
	    List<Integer> testIdx = indices.subList(numTrain, numSamples);
	    
	    // Create train and test matrices
	    boolean[][] trainMat = new boolean[numGenes][numTrain];
	    boolean[][] testMat = new boolean[numGenes][numTest];

	    for (int g = 0; g < numGenes; g++) {
	        for (int i = 0; i < numTrain; i++) {
	            trainMat[g][i] = mat[g][trainIdx.get(i)];
	        }
	        for (int i = 0; i < numTest; i++) {
	            testMat[g][i] = mat[g][testIdx.get(i)];
	        }
	    }

	    Data trainData = new Data(data.getName() + "_train", numGenes, numTrain, geneNames, trainMat);
	    Data testData = new Data(data.getName() + "_test", numGenes, numTest, geneNames, testMat);
	    
	    return new Pair<>(trainData, testData);
	}
	
	@Override
	public String toString() {
		return "Data [name=" + name + ", numGenes=" + numGenes + ", numSamples=" + numSamples + ", numMutations="
				+ numMutations + "]";
	}
}
