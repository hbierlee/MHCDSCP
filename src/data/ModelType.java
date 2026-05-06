package data;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 */
public enum ModelType {
	priceAndBranchHeuristic, // First solve the root node to optimality, then solve a MIP
	greedyRestrictedHeuristic, // Generate greedy random columns, then solve a MIP/CP
}
