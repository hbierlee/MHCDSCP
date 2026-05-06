package data;

import java.util.Random;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 */
public class GlobalParam {
	public static final Random RANDOM = new Random(123);
	
	public static final double EPSILON6 = 1e-6;
	
	// Limits the time for the price-and-branch heuristic
	public static final boolean USE_GLOBAL_TIME_LIMIT = false;
	public static final double GLOBAL_TIME_LIMIT_IN_SECONDS = Double.MAX_VALUE; // in seconds 

	// Limits the solver time (e.g. CPLEX or ORtools). Default is 30 seconds 
	public static final double SOLVE_IP_TIME_LIMIT_IN_SECONDS = 30; 
	
	public static int MIN_HIT_SIZE = 2;
	public static int MAX_HIT_SIZE = 3;
	
	public static final double TRAIN_TEST_SPLIT = 0.25; // Test data is 25%, train data is 75%
	
	// ColGen parameters
	public static final double ALPHA = 1;
	public static int BETA = 10;
	
	// Pricing problem speedups
	public static final boolean PP_USE_VNS = true;
	public static final boolean PP_SOLVE_REDUCED_MIP = true; // Solve a reduced MIP model
	
	// Random initialisation
	public static final int MAX_SELECT = 100;
	public static final int NUM_COMBINATIONS = 100000;
}
