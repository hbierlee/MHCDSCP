package performanceMeasures;

import java.util.ArrayList;
import java.util.List;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Store the TP, TN, FP, FN and the performance measures
 */
public class PerformanceMeasuresClass {
	private int TP, TN, FP, FN;
	private double MCC, specificity, sensitivity, F1;

	public PerformanceMeasuresClass(int TP, int TN, int FP, int FN) {
		this.TP = TP;
		this.TN = TN;
		this.FP = FP;
		this.FN = FN;
		
		MCC = PerformanceMeasures.calcMCC(this);
		specificity = PerformanceMeasures.calcSpecificity(this);
		sensitivity = PerformanceMeasures.calcSensitivity(this);
		F1 = PerformanceMeasures.calcF1(this);
	}
	
	public static List<String> getStringValues(String s) {
		List<String> values = new ArrayList<>();
		values.add("MCC"+"_"+s);
		values.add("specificity"+"_"+s);
		values.add("sensitivity"+"_"+s);
		values.add("F1"+"_"+s);
		values.add("avg"+"_"+s);
		return values;
	}
	
	public static List<Double> getNANValues() {
		List<Double> values = new ArrayList<>();
		for(int i = 0; i < getStringValues("").size(); i++) {
			values.add(Double.NaN);
		}
		return values;
	}
	
	public List<Double> getValues() {
		List<Double> values = new ArrayList<>();
		values.add(MCC);
		values.add(specificity);
		values.add(sensitivity);
		values.add(F1);
		values.add(PerformanceMeasures.calcAvgMeasure(this));
		return values;
	}

	public double getMCC() {
		return MCC;
	}

	public double getSpecificity() {
		return specificity;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public double getF1() {
		return F1;
	}

	public int getTP() {
		return TP;
	}

	public int getFP() {
		return FP;
	}

	public int getTN() {
		return TN;
	}

	public int getFN() {
		return FN;
	}
}