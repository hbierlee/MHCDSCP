package performanceMeasures;

import java.util.List;

import data.Data;
import data.Instance;

/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Calculates the performance measures
 * 
 * PerformanceMeasures:
 * - Specificity
 * - Sensitivity
 * - F1 score
 * - MCC
 */
public class PerformanceMeasures {
	public static PerformanceMeasuresClass getPerformanceMeasuresClass(boolean[] yTrue, boolean[] yPred) {
		int TP = 0, TN = 0, FP = 0, FN = 0;

		for (int i = 0; i < yTrue.length; i++) {
			if (yTrue[i] && yPred[i]) TP++;
			else if (!yTrue[i] && !yPred[i]) TN++;
			else if (!yTrue[i] && yPred[i]) FP++;
			else if (yTrue[i] && !yPred[i]) FN++;
		}
		return new PerformanceMeasuresClass(TP, TN, FP, FN);
	}

	// Specificity (True Negative Rate)
	public static double calcSpecificity(PerformanceMeasuresClass pmc) {
		if (pmc.getTN() + pmc.getFP() == 0) return 0.0;
		return (double) pmc.getTN() / (pmc.getTN() + pmc.getFP());
	}

	// Sensitivity (Recall or True Positive Rate)
	public static double calcSensitivity(PerformanceMeasuresClass pmc) {
		if (pmc.getTP() + pmc.getFN() == 0) return 0.0;
		return (double) pmc.getTP() / (pmc.getTP() + pmc.getFN());
	}
	
	// Precision (Positive Predictive Value)
	public static double calcPrecision(PerformanceMeasuresClass pmc) {
		if (pmc.getTP() + pmc.getFP() == 0) return 0.0;
		return (double) pmc.getTP() / (pmc.getTP() + pmc.getFP());
	}
	
	// F1 Score
	public static double calcF1(PerformanceMeasuresClass pmc) {
		double precision = calcPrecision(pmc);
		double recall = calcSensitivity(pmc);
		if (precision + recall == 0) return 0.0;
		return 2.0 * (precision * recall) / (precision + recall);
	}

	public static double calcMCC(PerformanceMeasuresClass pmc) {
		double numerator = (pmc.getTP() * pmc.getTN()) - (pmc.getFP() * pmc.getFN());
		double denominator = Math.sqrt(
				(double)(pmc.getTP() + pmc.getFP()) * (double)(pmc.getTP() + pmc.getFN()) * (double)(pmc.getTN() + pmc.getFP()) * (double)(pmc.getTN() + pmc.getFN())
				);

		if (denominator == 0) return 0.0;
		return numerator / denominator;
	}

	public static PerformanceMeasuresClass getOutSamplePerformanceMeasuresClass(Instance instance, List<List<Integer>> combinations) {
		Data dataTumor = instance.getDataTumor();
		Data dataNormal = instance.getDataNormal();

		boolean[] yTrue = new boolean[dataTumor.getNumSamples() + dataNormal.getNumSamples()];
		boolean[] yPred = new boolean[yTrue.length];
		int idx = 0;

		// Tumor samples
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			yTrue[idx] = true; // true tumor
			boolean coversSample = false;
			for(List<Integer> combination: combinations) {
				if(coversSample) {
					break;
				}
				coversSample = dataTumor.geneCombnationCoversSample(combination, i);
			}
			yPred[idx] = coversSample;
			idx++;
		}

		// Normal samples
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			yTrue[idx] = false; // true normal
			boolean coversSample = false;
			for(List<Integer> combination: combinations) {
				if(coversSample) {
					break;
				}
				coversSample = dataNormal.geneCombnationCoversSample(combination, i);
				// If you reach this, it means that combination covered sample i. In other words, all genes in combination were also mutated in sample.
			}
			yPred[idx] = coversSample;
			idx++;
		}

		PerformanceMeasuresClass pmc = getPerformanceMeasuresClass(yTrue, yPred);
		return pmc;
	}
	
	public static double calcAvgMeasure(PerformanceMeasuresClass pmc) {
		return (pmc.getMCC() + pmc.getSpecificity() + pmc.getSensitivity() + pmc.getF1()) / 4.0;
	}
	
	public static void print(PerformanceMeasuresClass pmc) {
		PerformanceMeasures.print(pmc, "Out-sample");
	}
	
	public static void print(PerformanceMeasuresClass pmc, String s) {
		System.out.println(s+" MCC = " + pmc.getMCC());
		System.out.println(s+" specificity = " + pmc.getSpecificity());
		System.out.println(s+" sensitivity = " + pmc.getSensitivity());
		System.out.println(s+" F1 = " + pmc.getF1());
		System.out.println(s+" avg = " + PerformanceMeasures.calcAvgMeasure(pmc));
	}
}
