package model.CP;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import data.Data;
import data.GlobalParam;
import data.Instance;
import data.Solution;
import model.MIP.PricingProblemRandom;
import performanceMeasures.PerformanceMeasures;
import performanceMeasures.PerformanceMeasuresClass;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Solves constraint programming formulation.
 */
public class CPModel {
	private Instance instance;
	private Data dataTumor, dataNormal;
	private Set<List<Integer>> startCombinations;

	private CpModel model;
	private CpSolver solver;
	private CpSolverStatus status;
	private BoolVar[] tumorVars;
	private IntVar[] normalVars;
	private Map<List<Integer>, BoolVar> combinationVars;

	private Solution bestSolution;

	private long startTimeTotal;
	private double timeInitModel, timeAddStartColumns, timePhase2, timeTotalColGen;

	public CPModel(Instance instance) {
		startTimeTotal = System.currentTimeMillis();

		long startTime = System.currentTimeMillis();
		PricingProblemRandom ppr = new PricingProblemRandom(instance);
		startCombinations = ppr.solve();
		timeAddStartColumns = (System.currentTimeMillis() - startTime) / 1000.0f;
		System.out.println("timeAddStartColumns (s): " + timeAddStartColumns);

		this.instance = instance;
		this.dataTumor = instance.getDataTumor();
		this.dataNormal = instance.getDataNormal();

		Loader.loadNativeLibraries();
		model = new CpModel();

		initModel();
	}

	public void solve() {
		solver = new CpSolver();
		solver.getParameters().setMaxTimeInSeconds(GlobalParam.SOLVE_IP_TIME_LIMIT_IN_SECONDS);
		solver.getParameters().setLogSearchProgress(true);

		long startTime = System.currentTimeMillis();
		status = solver.solve(model);
		timePhase2 = (System.currentTimeMillis() - startTime) / 1000.0f;
		timeTotalColGen = (System.currentTimeMillis() - startTimeTotal) / 1000.0f;

		System.out.println("timePhase2: " + timePhase2);
		System.out.println("timeTotalColGen: " + timeTotalColGen);
		System.out.println("status: " + status);

		bestSolution = new Solution(instance, solver.objectiveValue());
		bestSolution.setRootnodeObj(solver.bestObjectiveBound());
		bestSolution.setModelStatus(String.valueOf(status));
		bestSolution.setCombinationList(createCombinations());
		bestSolution.setStringCombinationList(Data.convertToOriginalName(instance.getDataNormal(), bestSolution.getCombinationList()));
		bestSolution.setInSamplePMC(calcPMC_basedOnCurrentVars());
	}

	public PerformanceMeasuresClass calcPMC_basedOnCurrentVars() {
		boolean[] yTrue = new boolean[dataTumor.getNumSamples() + dataNormal.getNumSamples()];
		boolean[] yPred = new boolean[yTrue.length];
		int idx = 0;

		// Tumor samples
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			yTrue[idx] = true; // true tumor
			double val = solver.value(tumorVars[i]);
			yPred[idx] = (val > 0.1) ? true : false;
			idx++;
		}

		// Normal samples
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			yTrue[idx] = false; // true normal
			double val = Double.NaN;
			val = solver.value(normalVars[i]);
			yPred[idx] = (val > 0.1) ? true : false;
			idx++;
		}

		PerformanceMeasuresClass pmc = PerformanceMeasures.getPerformanceMeasuresClass(yTrue, yPred);
		return pmc;
	}

	private void initModel() {
		long startTime = System.currentTimeMillis();

		initCombinationVariables();
		initTumorVariables();
		initNormalVariables();
		initObjective();
		initTumorCoverConstraintsMaxOperator2();
		initNormalCoverConstraints();
		initMaxNumCombinationsConstraints();

		timeInitModel = (System.currentTimeMillis() - startTime) / 1000.0f;
		System.out.println("timeInitModel (s): " + timeInitModel);
	}

	private void initCombinationVariables() {
		combinationVars = new LinkedHashMap<>();

		int i = 0;
		for(List<Integer> combination: startCombinations) {
			combinationVars.put(combination, model.newBoolVar("c_"+(i++)));
		}
	}

	private void initTumorVariables() {
		tumorVars = new BoolVar[dataTumor.getNumSamples()]; // tumor coverage (for this combination)
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorVars[i] = model.newBoolVar("t_"+i);
		}
	}

	private void initNormalVariables() {	
		normalVars = new IntVar[dataNormal.getNumSamples()]; // tumor coverage (for this combination)
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			normalVars[i] = model.newIntVar(0, Integer.MAX_VALUE, "n_"+i);
		}
	}

	private void initNormalCoverConstraints() {
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			List<BoolVar> coveringVars = new ArrayList<>();

			for(Entry<List<Integer>, BoolVar> entry: combinationVars.entrySet()) {
				if (dataNormal.geneCombnationCoversSample(entry.getKey(), i)) {
					coveringVars.add(entry.getValue());
				}
			}

			model.addEquality(normalVars[i], LinearExpr.sum(coveringVars.toArray(new BoolVar[0])));	
		}
	}

	/**
	 * x_t = max_{c \in C_t} z_c
	 */
	private void initTumorCoverConstraintsMaxOperator2() {
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			List<BoolVar> coveringVars = new ArrayList<>();

			for (Entry<List<Integer>, BoolVar> entry : combinationVars.entrySet()) {
				if (dataTumor.geneCombnationCoversSample(entry.getKey(), i)) {
					coveringVars.add(entry.getValue());
				}
			}

			if (coveringVars.isEmpty()) {
	            model.addEquality(tumorVars[i], 0);
	        } else {
	            model.addMaxEquality(tumorVars[i], coveringVars);
	            
	        }
		}
	}

	private void initMaxNumCombinationsConstraints() {
		IntVar[] zVars = combinationVars.values().toArray(new IntVar[0]);
		model.addLessOrEqual(LinearExpr.sum(zVars), GlobalParam.BETA);
	}
	
	private void initObjective() {
		LinearExprBuilder objBuilder = LinearExpr.newBuilder();

		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			objBuilder.addTerm(tumorVars[i], 1);
		}		
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			objBuilder.addTerm(normalVars[i], -1);
		}	

		model.maximize(objBuilder.build());
	}

	public List<List<Integer>> createCombinations() {
		List<List<Integer>> sol = new ArrayList<>();
		for(Entry<List<Integer>, BoolVar> entry: combinationVars.entrySet()) {
			if(solver.value(entry.getValue())>0.1) {
				sol.add(entry.getKey());
			}
		}
		return sol;
	}

	public void printSolution() {
		System.out.println("status: " + status);
		System.out.println("objective " + bestSolution.getObjective());

		for(List<Integer> combination: bestSolution.getCombinationList()) {
			System.out.println(combination);
		}
		for(List<String> combination: bestSolution.getStringCombinationList()) {
			System.out.println(combination);
		}

		System.out.println("Number of combinations used " + bestSolution.getCombinationList().size());

		PerformanceMeasures.print(bestSolution.getInSamplePMC(), "In-sample");
	}

	public Solution getSolution() {
		bestSolution.setGLOBAL_TIME_LIMIT_IN_SECONDS(GlobalParam.GLOBAL_TIME_LIMIT_IN_SECONDS);
		bestSolution.setMIN_HIT_SIZE(GlobalParam.MIN_HIT_SIZE);
		bestSolution.setMAX_HIT_SIZE(GlobalParam.MAX_HIT_SIZE);
		bestSolution.setTRAIN_TEST_SPLIT(GlobalParam.TRAIN_TEST_SPLIT);
		bestSolution.setALPHA(GlobalParam.ALPHA);
		bestSolution.setBETA(GlobalParam.BETA);
		bestSolution.setMAX_SELECT(GlobalParam.MAX_SELECT);
		bestSolution.setNUM_COMBINATIONS(GlobalParam.NUM_COMBINATIONS);

		bestSolution.setTimeTotalColGen(timeTotalColGen);
		bestSolution.setTimeAddStartColumns(timeAddStartColumns);
		bestSolution.setTimePhase2(timePhase2);
		bestSolution.setTimeInitModel(timeInitModel);
		return bestSolution;
	}
}
