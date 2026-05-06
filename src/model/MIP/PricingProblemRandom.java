package model.MIP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import data.Data;
import data.GlobalParam;
import data.Instance;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Generates greedy random columns
 */
public class PricingProblemRandom {
	private Instance instance;	

	public PricingProblemRandom(Instance instance) {
		this.instance = instance;
	}

	public Set<List<Integer>> solve() {
		Data dataTumor = instance.getDataTumor();

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

		// maxSelect
		List<Integer> topIndices = numAppears.entrySet()
				.stream()
				.sorted(Map.Entry.<Integer, Integer>comparingByValue(Comparator.reverseOrder()))
				.limit(GlobalParam.MAX_SELECT)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		Set<List<Integer>> sortedCombinations = new LinkedHashSet<>();

		int stuckInLoopIter = 0;
		double prev = -1;
		while (sortedCombinations.size() < GlobalParam.NUM_COMBINATIONS) {
			if(sortedCombinations.size()==prev) {
				stuckInLoopIter++;
				if(stuckInLoopIter>1000) {
					break;
				}
			}
			else {
				stuckInLoopIter = 0;
			}
			prev = sortedCombinations.size();
			
			// Randomly select combination size in [MIN_HIT_SIZE, MAX_HIT_SIZE]
			int size = GlobalParam.MIN_HIT_SIZE +
					GlobalParam.RANDOM.nextInt(GlobalParam.MAX_HIT_SIZE - GlobalParam.MIN_HIT_SIZE + 1);

			// Shuffle a copy of topIndices
			List<Integer> shuffled = new ArrayList<>(topIndices);
			Collections.shuffle(shuffled, GlobalParam.RANDOM);

			// Take the first 'size' elements as a combination
			List<Integer> combination = shuffled.subList(0, Math.min(size, shuffled.size()));
			Collections.sort(combination);

			// Only add a combination if it is present in at least 1 tumor sample
			for (int i = 0; i < dataTumor.getNumSamples(); i++) {
				boolean coversSample = true;
				for (int geneIdx : combination) {
					if (!dataTumor.getMat()[geneIdx][i]) {
						coversSample = false;
						break;
					}
				}

				if (coversSample) {
					sortedCombinations.add(combination);
					break;
				}
			}

		}
		System.out.println("Number of unique sortedCombinations: " + sortedCombinations.size());
		return sortedCombinations;
	}
}
