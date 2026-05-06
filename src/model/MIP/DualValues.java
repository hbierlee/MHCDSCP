package model.MIP;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Store the dual values needed in column generation approach.
 */
public class DualValues {
	private double[] tumorDuals, normalDuals;
	private double lambd;
	private double largestCombinationDual;
	
	public DualValues(double[] tumorDuals, double[] normalDuals, double lambd, double largestCombinationDual) {
		this.tumorDuals = tumorDuals;
		this.normalDuals = normalDuals;
		this.lambd = lambd;
		this.largestCombinationDual = largestCombinationDual;
	}

	public double getLargestCombinationDual() {
		return largestCombinationDual;
	}

	public double[] getTumorDuals() {
		return tumorDuals;
	}

	public double[] getNormalDuals() {
		return normalDuals;
	}
	
	public double getLambd() {
		return lambd;
	}
}
