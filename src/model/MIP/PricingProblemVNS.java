package model.MIP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.Data;
import data.GlobalParam;
import data.Instance;
import ilog.concert.IloException;
import util.Pair;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Solve the pricing problem heuristically using a variable neighbourhood search.
 */
public class PricingProblemVNS {
	private static Logger log = LoggerFactory.getLogger(PricingProblemVNS.class);

	private Instance instance;
	private Data dataTumor;
	private Data dataNormal;
	private int numGenes;

	private List<Integer> sortedIndices;
	private List<Integer> selectFromIndicesList;
	private Set<Integer> selectFromIndicesSet;

	private int maxIter = 5; // iota in paper
	private int kMax = GlobalParam.MAX_HIT_SIZE;
	private int maxSelect = 1000; // gamma3 in paper

	public PricingProblemVNS(Instance instance) throws IloException {
		this.instance = instance;
		this.dataTumor = instance.getDataTumor();
		this.dataNormal = instance.getDataNormal();
		this.numGenes = instance.getDataTumor().getNumGenes();

		// Num times a gene appears
		Map<Integer, Integer> numAppears = new LinkedHashMap<>();
		for(int i = 0; i < dataTumor.getNumGenes(); i++) {
			int count = 0;
			for(int j = 0; j < dataTumor.getNumSamples(); j++) {
				if(dataTumor.getMat()[i][j]) {
					count++;
				}
			}
			numAppears.put(i, count);
		}

		sortedIndices = numAppears.entrySet()
				.stream()
				.sorted(Map.Entry.<Integer, Integer>comparingByValue(Comparator.reverseOrder()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		selectFromIndicesSet = sortedIndices.stream()
				.limit(maxSelect)
				.collect(Collectors.toSet());
	}

	public List<List<Integer>> solvePP(DualValues dualValues, Set<Integer> selectedGeneSet) {
		// These genes are more likely to occur in a column
		selectFromIndicesSet.addAll(selectedGeneSet);
		selectFromIndicesList = new ArrayList<>(selectFromIndicesSet);

		List<List<Integer>> res = new ArrayList<>();

		double bestRC = Double.NEGATIVE_INFINITY;
		for(int hitSize = GlobalParam.MIN_HIT_SIZE; hitSize <= GlobalParam.MAX_HIT_SIZE; hitSize++) {
			Pair<List<Integer>, Double> pair = solvePP(dualValues, hitSize);
			if(pair.second > GlobalParam.EPSILON6) {
				res.add(pair.first);	
				if(pair.second>bestRC) {
					bestRC = pair.second;
				}
			}
		}
		log.info("PP VNS found {} columns. BestRC= {}", res.size(), bestRC);
		return res;
	}

	private int[] constructCombination(int hitSize) {
		Collections.shuffle(selectFromIndicesList, GlobalParam.RANDOM);
		
		int[] combination = new int[hitSize];
		for(int i = 0; i < hitSize; i++) {
			combination[i] = selectFromIndicesList.get(i);
		}
		return combination;
	}
	
	private Pair<List<Integer>, Double> solvePP(DualValues dualValues, int hitSize) {		
		// Step 1
		int[] bestCombination = constructCombination(hitSize);
		double bestRC = calcRC(dualValues, bestCombination);

		// --- Step 2: Main VNS loop
		int iter = 0;
		while (iter < maxIter) {
			int k = 1;
			while (k <= kMax) {
				// (a) Shaking: generate neighbor with neighborhood size k
				int[] shaken = shake(bestCombination, hitSize, k);

				// (b) Local search
				int[] improved = localSearch(dualValues, shaken, hitSize);
				double improvedRC = calcRC(dualValues, improved);

				// (c) Acceptance criterion
				if (improvedRC > bestRC) { 
					bestCombination = improved;
					bestRC = improvedRC;
					k = 1; // reset neighborhood
				} else {
					k++;
				}
			}
			iter++;
		}

		// Convert to list and return
		List<Integer> result = new ArrayList<>();
		for (int g : bestCombination) result.add(g);
		Collections.sort(result);
		return new Pair<>(result, bestRC);
	}

	private int[] shake(int[] solution, int hitSize, int k) {
		// Simple shaking: randomly replace k genes
		int[] newSol = Arrays.copyOf(solution, solution.length);

		for (int i = 0; i < k; i++) {
			int pos = GlobalParam.RANDOM.nextInt(newSol.length);
			int newGene;
			do {
				newGene = GlobalParam.RANDOM.nextInt(selectFromIndicesList.size());
			} while (contains(newSol, newGene));
			newSol[pos] = newGene;
		}
		return newSol;
	}

	private int[] localSearch(DualValues dualValues, int[] solution, int hitSize) {
		int[] best = Arrays.copyOf(solution, solution.length);
		double bestRC = calcRC(dualValues, best);

		for (int i = 0; i < best.length; i++) {
			for (int j = 0; j < selectFromIndicesList.size(); j++) {
				int indGene = selectFromIndicesList.get(j);
				if (!contains(best, indGene)) {
					int[] neighbor = Arrays.copyOf(best, best.length);
					neighbor[i] = indGene;
					double rc = calcRC(dualValues, neighbor);
					if (rc > bestRC) {
						best = neighbor;
						bestRC = rc;
					}
				}
			}
		}
		return best;
	}

	private boolean contains(int[] arr, int val) {
		for (int a : arr) if (a == val) return true;
		return false;
	}

	private double calcRC(DualValues dualValues, int[] combination) {
		int tWords = dataTumor.getGeneBitsets()[0].length;
	    int nWords = dataNormal.getGeneBitsets()[0].length;

	    // Start with all bits SET (all samples covered), then AND down
	    long[] tumorCoverage  = new long[tWords];
	    long[] normalCoverage = new long[nWords];
	    Arrays.fill(tumorCoverage,  ~0L);
	    Arrays.fill(normalCoverage, ~0L);

	    // AND all gene bitsets. Sample stays covered only if ALL genes cover it
	    for (int g : combination) {
	        for (int w = 0; w < tWords; w++)
	            tumorCoverage[w]  &= dataTumor.getGeneBitsets()[g][w];
	        for (int w = 0; w < nWords; w++)
	            normalCoverage[w] &= dataNormal.getGeneBitsets()[g][w];
	    }

	    double RC = -dualValues.getLambd();
	    double[] tumorDuals  = dualValues.getTumorDuals();
	    double[] normalDuals = dualValues.getNormalDuals();

	    for (int s = 0; s < dataTumor.getNumSamples(); s++) {
	        if ((tumorCoverage[s / 64] & (1L << (s % 64))) != 0)
	            RC += tumorDuals[s];
	    }
	    for (int s = 0; s < dataNormal.getNumSamples(); s++) {
	        if ((normalCoverage[s / 64] & (1L << (s % 64))) != 0)
	            RC -= normalDuals[s];
	    }
	    return RC;
	}
}
