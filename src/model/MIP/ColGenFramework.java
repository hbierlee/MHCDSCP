package model.MIP;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.Data;
import data.GlobalParam;
import data.Instance;
import data.ModelType;
import data.Solution;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import performanceMeasures.PerformanceMeasures;
import performanceMeasures.PerformanceMeasuresClass;

/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Column generation framework:
 * - ModelType = greedyRestrictedHeuristic. Generate greedy random columns, then solve a MIP
 * - ModelType = priceAndBranchHeuristic. First solve the root node to optimality, then solve a MIP
 */
public class ColGenFramework {
	private static Logger log = LoggerFactory.getLogger(ColGenFramework.class);

	private Instance instance;
	private Data dataTumor, dataNormal;
	private IloCplex model;
	private PricingProblemMIP pricingProblem;
	private PricingProblemVNS ppVNS;
	private Solution bestSolution;
	private ModelType colGenType;

	private IloObjective objective;
	private IloNumVar[] tumorVars;
	private IloNumVar[] normalVars;
	private IloRange[] tumorConstraints;
	private IloRange[] normalConstraints;
	private IloRange maxNumCombinationsConstraints;
	private Map<List<Integer>, IloNumVar> combinationVars;
	private Set<Integer> selectedGeneSet;

	private String modelStatus;
	private double modelGap;
	private int colGenIter;
	private int colGenNumColumnsAdded;

	private double[] tumorDuals;
	private double[] normalDuals;

	private double rootnodeObj;

	private double timeTotalColGen, timeAddStartColumns, timePhase1, timePhase2, timeInitModel, timeMP, timePP;
	private boolean timeLimitReached;
	private long startTimeTotalColGen, startTimePhase2;

	private int numRootnodeIterations;
	private double curObj, bestObj, bestLP, rootnodeGap;
	
	// pp VNS
	private int ppVNS_iter, ppVNS_numColumns;

	public ColGenFramework(Instance instance, ModelType colGenType) throws IloException {
		this.instance = instance;
		this.colGenType = colGenType;
		System.out.println("ColGenType: " +colGenType);
		this.dataTumor = instance.getDataTumor();
		this.dataNormal = instance.getDataNormal();

		this.model = new IloCplex();
		model.setOut(null);

		initModel();
	}

	public void addStartColumns() throws IloException {
		// Generate some cols
		PricingProblemRandom ppr = new PricingProblemRandom(instance);
		Set<List<Integer>> combinations = ppr.solve();

		for(List<Integer> combination: combinations) {
			addColumn(combination);
		}
	}

	private void addColumn(List<Integer> combination) throws IloException {
		if(combinationVars.containsKey(combination)) {
			throw new IllegalArgumentException("Combination already added to colgen: " + combination);
		}

		IloColumn col = model.column(objective, 0);

		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			if (dataTumor.geneCombnationCoversSample(combination, i)) {
				col = col.and(model.column(tumorConstraints[i], -1));
			}
		}

		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			if (dataNormal.geneCombnationCoversSample(combination, i)) {
				col = col.and(model.column(normalConstraints[i], 1));
			}
		}

		col = col.and(model.column(maxNumCombinationsConstraints, 1));

		IloNumVar var = model.numVar(col, 0, Double.POSITIVE_INFINITY);
		combinationVars.put(combination, var);
	}

	private void initModel() throws IloException {
		long startTime = System.currentTimeMillis();
		initEmptyMaps();

		initTumorVariables();
		initNormalVariables();
		initObjective();
		initTumorCoverConstraints();
		initNormalCoverConstraints();
		initMaxNumCombinationsConstraints();
		if(colGenType.equals(ModelType.priceAndBranchHeuristic)) {
			initPricingProblem();
		}

		timeInitModel = (System.currentTimeMillis() - startTime) / 1000.0f;
		System.out.println("Init model (s): " + timeInitModel);
	}

	private void initEmptyMaps() {
		combinationVars = new LinkedHashMap<>();
		selectedGeneSet = new LinkedHashSet<>();
		
		tumorDuals = new double[dataTumor.getNumSamples()];
		normalDuals = new double[dataNormal.getNumSamples()];
	}

	private void initPricingProblem() throws IloException {
		pricingProblem = new PricingProblemMIP(instance);
		ppVNS = new PricingProblemVNS(instance);
	}

	private void initMaxNumCombinationsConstraints() throws IloException {
		IloNumExpr expr = model.numExpr();
		maxNumCombinationsConstraints = model.addLe(expr, GlobalParam.BETA);
	}

	private void initObjective() throws IloException {
		IloNumExpr expr = model.numExpr();

		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			expr = model.sum(expr, tumorVars[i]);
		}		
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			IloNumExpr temp = model.prod(GlobalParam.ALPHA, normalVars[i]);
			expr = model.diff(expr, temp);
		}	

		objective = model.addObjective(IloObjectiveSense.Maximize, expr);
	}

	private void initTumorCoverConstraints() throws IloException {
		tumorConstraints = new IloRange[dataTumor.getNumSamples()];
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorConstraints[i] = model.addLe(tumorVars[i], 0);
		}
	}

	private void initNormalCoverConstraints() throws IloException {
		normalConstraints = new IloRange[dataNormal.getNumSamples()];
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			normalConstraints[i] = model.addLe(model.prod(-1, normalVars[i]), 0);
		}
	}

	private void initTumorVariables() throws IloException {
		tumorVars = new IloNumVar[dataTumor.getNumSamples()];
		for(int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorVars[i] = model.numVar(0, 1);
		}
	}

	private void initNormalVariables() throws IloException {
		normalVars = new IloNumVar[dataNormal.getNumSamples()];
		for(int i = 0; i < dataNormal.getNumSamples(); i++) {
			normalVars[i] = model.numVar(0, Double.POSITIVE_INFINITY);
		}
	}

	public void solveColGen() throws IloException {
		if(colGenType.equals(ModelType.greedyRestrictedHeuristic)) {
			solveFirstRootnodeThenIP(true);
		}
		else if(colGenType.equals(ModelType.priceAndBranchHeuristic)) {
			solveFirstRootnodeThenIP(false);
		}
		else {
			throw new IllegalArgumentException("ColGenType " + colGenType + " not yet defined");
		}
	}

	private Solution roundingHeuristic() throws IloException {
		// Select the best GlobalParam.BETA columns/combinations
		Map<List<Integer>, Double> res = new LinkedHashMap<>();

		double selectedBeta = 0;
		for(Entry<List<Integer>, IloNumVar> entry: combinationVars.entrySet()) {
			double val = model.getValue(entry.getValue());
			res.put(entry.getKey(), val);
			selectedBeta += val;
		}
		
		// We are allowed to select GlobalParam.BETA, however, according to the model we need selectedBeta.
		int finalSelectedBeta = (int) Math.ceil(selectedBeta);
		finalSelectedBeta = Math.min(finalSelectedBeta, GlobalParam.BETA);
		
		  // Sort combinations by descending value
	    List<Map.Entry<List<Integer>, Double>> sorted = res.entrySet().stream()
	            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
	            .collect(Collectors.toList());

	    // Select the top GlobalParam.BETA combinations
	    List<List<Integer>> combinationList = sorted.stream()
	            .limit(finalSelectedBeta)
	            .map(Map.Entry::getKey)
	            .collect(Collectors.toList());

 		Solution solution = new Solution(instance, instance.calculateObjective(combinationList));
 		solution.setCombinationList(combinationList);
 		solution.setStringCombinationList(Data.convertToOriginalName(instance.getDataNormal(), solution.getCombinationList()));
 		solution.setInSamplePMC(PerformanceMeasures.getOutSamplePerformanceMeasuresClass(instance, combinationList));
 		
		return solution;
	}
	
	private void generateColumns() throws IloException {
		generateColumns(Integer.MAX_VALUE);
	}

	private void generateColumns(int iterLimit) throws IloException {
		while(true) {
			colGenIter++;
			
			// Solve RMP
			long startTimeTemp = System.currentTimeMillis();
			model.solve();
			timeMP += (System.currentTimeMillis() - startTimeTemp) / 1000.0f;
			double obj = model.getObjValue();
			log.info("ColGen iter {}, obj {}", colGenIter, obj);
			
			Solution currentSolution = roundingHeuristic();
			log.info("Rounding heuristic. New integral solution= {}. Current best integer solution= {}", currentSolution.getObjective(), bestObj);
			if (bestSolution == null || currentSolution.getObjective() > bestObj) {
				log.info("Storing better integral solution of value: {}", currentSolution.getObjective());
				bestObj = currentSolution.getObjective();
				bestSolution = currentSolution;
			}
			
			if(colGenIter>=iterLimit) {
				log.info("Stop. Iteration limit reached: {}", iterLimit);
				break;
			}
			
			if(GlobalParam.USE_GLOBAL_TIME_LIMIT && ((System.currentTimeMillis() - startTimeTotalColGen) / 1000.0f > GlobalParam.GLOBAL_TIME_LIMIT_IN_SECONDS)) {
				timeLimitReached = true;
				timeTotalColGen = (System.currentTimeMillis() - startTimeTotalColGen) / 1000.0f;
				modelStatus = timeLimitReached ? "timeLimitReached" : "optimal";
				bestSolution.setModelStatus(modelStatus);
				modelGap = 100d * (bestLP - bestObj) / bestObj;
				
				System.out.println("Global time limit reached! - generateColumns");
				return;
			}

			// Solve PP
			startTimeTemp = System.currentTimeMillis();
			List<List<Integer>> combinations = new ArrayList<>();
			DualValues duals = getDuals();
			if(GlobalParam.PP_USE_VNS) {
				combinations.addAll(ppVNS.solvePP(duals, selectedGeneSet));
			}
			if(!combinations.isEmpty()) {
				ppVNS_iter++;
				ppVNS_numColumns += combinations.size();
			}
			else {	
				log.info("PP solved using MIP");
				List<Integer> combination = pricingProblem.solvePP(duals, selectedGeneSet);
				if(combination!=null) {
					combinations.add(combination);
				}
			}
			double tempPP = (System.currentTimeMillis() - startTimeTemp) / 1000.0f;
			timePP += tempPP;
			log.info("PP time {}", tempPP);
			if(combinations.isEmpty()) {
				log.info("Stop. No more combinations found");
				break;
			}

			for(List<Integer> combination: combinations) {
				addColumn(combination);
				selectedGeneSet.addAll(combination);
			}
			colGenNumColumnsAdded += combinations.size();
			log.info("colGenNumColumnsAdded: " + colGenNumColumnsAdded);
		}
		curObj = model.getObjValue();
	}

	private void solveFirstRootnodeThenIP(boolean heuristic) throws IloException {
		startTimeTotalColGen = System.currentTimeMillis();
		long startTimeTotal = System.currentTimeMillis();
		long startTime = System.currentTimeMillis();
		if(heuristic) {
			addStartColumns();
		}
		timeAddStartColumns = (System.currentTimeMillis() - startTime) / 1000.0f;

		startTime = System.currentTimeMillis();
		colGenIter = 0;
		boolean isIntegral = false;
		if(heuristic) {
			// Skip
		}
		else {
			generateColumns();
			rootnodeObj = model.getObjValue();
			bestLP = rootnodeObj;
			isIntegral = isIntegral();
		}
		timePhase1 = (System.currentTimeMillis() - startTime) / 1000.0f;

		if(pricingProblem!=null) {
			System.out.println("PP columns found using Stage1: " + pricingProblem.getCountStage1() + ", using Stage2: " + pricingProblem.getCountStage2());
		}
		System.out.println("Root node solution integral? " + isIntegral);
		if(!isIntegral) {
			System.out.println("Solve IP");
			model.setParam(IloCplex.Param.TimeLimit, GlobalParam.SOLVE_IP_TIME_LIMIT_IN_SECONDS);
			convertToIntegerVariables();
			startTime = System.currentTimeMillis();
			model.solve();
			timePhase2 = (System.currentTimeMillis() - startTime) / 1000.0f;
		}
		timeTotalColGen = (System.currentTimeMillis() - startTimeTotal) / 1000.0f;
		
		Solution currentSolution = new Solution(instance, model.getObjValue());
		log.info("Solve IP. Integral solution of value: {}", currentSolution.getObjective());
		if (bestSolution == null || currentSolution.getObjective() > bestObj) {
			log.info("Storing better integral solution of value: {}", currentSolution.getObjective());
			bestObj = currentSolution.getObjective();
			bestSolution = currentSolution;
			
			bestSolution.setCombinationList(createCombinations());
			bestSolution.setStringCombinationList(Data.convertToOriginalName(instance.getDataNormal(), bestSolution.getCombinationList()));
			bestSolution.setInSamplePMC(calcPMC_basedOnCurrentVars());
		}
		modelStatus = model.isPrimalFeasible() ? "feasible" : "infeasible";
		bestSolution.setModelStatus(modelStatus);
		rootnodeGap = 100d * (bestLP - bestObj) / bestObj;
		modelGap = rootnodeGap;
	}

	public void convertToIntegerVariables() throws IloException {
		for (Entry<List<Integer>, IloNumVar> e: combinationVars.entrySet()) {
			model.add(model.conversion(e.getValue(), IloNumVarType.Int));
			e.getValue().setUB(1);
		}
		for(int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorVars[i].setUB(1);
		}
	}

	public DualValues getDuals() throws UnknownObjectException, IloException {
		for(int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorDuals[i] = model.getDual(tumorConstraints[i]);
		}

		for(int i = 0; i < dataNormal.getNumSamples(); i++) {
			normalDuals[i] = model.getDual(normalConstraints[i]);
		}
		
		double lambd = model.getDual(maxNumCombinationsConstraints);
		
		double largestCombinationDual = GlobalParam.EPSILON6;
		for(Entry<List<Integer>, IloNumVar> e: combinationVars.entrySet()) {
			double combinationDual = model.getReducedCost(e.getValue());
			if(combinationDual>largestCombinationDual) {
				largestCombinationDual = combinationDual;
			}
		}
		return new DualValues(tumorDuals, normalDuals, lambd, largestCombinationDual);
	}

	public void solve() throws IloException {
		model.solve();
	}

	public void clearModel() throws IloException {
		model.clearModel();
		model.end();
		if(pricingProblem!=null) {
			pricingProblem.clearModel();
		}
	}

	public List<List<Integer>> createCombinations() throws UnknownObjectException, IloException {
		return roundingHeuristic().getCombinationList();
	}

	private boolean isIntegral() throws IloException {
		for(Entry<List<Integer>, IloNumVar> entry: combinationVars.entrySet()) {
			double val = model.getValue(entry.getValue());
			if(isFractional(val)) {
				return false;
			}
		}
		return true;
	}

	private boolean isFractional(double val) {
		double tempViolation = Math.abs(Math.round(val) - val);
		if (tempViolation > GlobalParam.EPSILON6) {
			return true;
		}
		return false;
	}
	
	public void printSolution() throws IloException {
		System.out.println("is feasible? " + bestSolution.getModelStatus());
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

	public PerformanceMeasuresClass calcPMC_basedOnCurrentVars() throws UnknownObjectException, IloException {
		boolean[] yTrue = new boolean[dataTumor.getNumSamples() + dataNormal.getNumSamples()];
		boolean[] yPred = new boolean[yTrue.length];
		int idx = 0;

		// Tumor samples
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			yTrue[idx] = true; // true tumor
			double val = model.getValue(tumorVars[i]);
			yPred[idx] = (val > 0.1) ? true : false;
			idx++;
		}

		// Normal samples
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			yTrue[idx] = false; // true normal
			double val = model.getValue(normalVars[i]);
			yPred[idx] = (val > 0.1) ? true : false;
			idx++;
		}

		PerformanceMeasuresClass pmc = PerformanceMeasures.getPerformanceMeasuresClass(yTrue, yPred);
		return pmc;
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

		bestSolution.setModelStatus(modelStatus);
		bestSolution.setGap(modelGap);
		bestSolution.setRootnodeGap(rootnodeGap);
		
		bestSolution.setColGenType(colGenType);
		bestSolution.setTimeTotalColGen(timeTotalColGen);
		bestSolution.setTimeAddStartColumns(timeAddStartColumns);
		bestSolution.setTimePhase1(timePhase1);
		bestSolution.setTimePhase2(timePhase2);
		bestSolution.setTimeInitModel(timeInitModel);
		bestSolution.setTimeMP(timeMP);
		bestSolution.setTimePP(timePP);
		bestSolution.setRootnodeObj(rootnodeObj);
		bestSolution.setColGenIter(colGenIter);
		bestSolution.setColGenNumColumnsAdded(colGenNumColumnsAdded);

		bestSolution.setBestLP(bestLP);
		bestSolution.setNumRootnodeIterations(numRootnodeIterations);
		
		if(pricingProblem!=null) {
			bestSolution.setCountPPStage1(pricingProblem.getCountStage1());
			bestSolution.setCountPPStage2(pricingProblem.getCountStage2());
		}
		if(ppVNS!=null) {
			bestSolution.setPpVNS_iter(ppVNS_iter);
			bestSolution.setPpVNS_numColumns(ppVNS_numColumns);
		}
		return bestSolution;
	}
}
