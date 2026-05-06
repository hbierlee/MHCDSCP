package data;

import java.util.List;

import performanceMeasures.PerformanceMeasures;
import performanceMeasures.PerformanceMeasuresClass;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Stores the instance and corresponding solution and statistics
 */
public class Solution {
	private String folderName;
	private String fileName;
	private String folderFileName;
	private int setting;
	
	private Instance trainInstance;
	private double objective;
	private double gap;
	
	private String solverType;
	
	private Instance originalInstance;
	private Instance validationInstance;
	private Instance testInstance;
	
	// GlobalParam
	private double GLOBAL_TIME_LIMIT_IN_SECONDS;
	private int MIN_HIT_SIZE;
	private int MAX_HIT_SIZE;
	private double TRAIN_TEST_SPLIT; 
	private double ALPHA;
	private double BETA;
	private int MAX_SELECT;
	private int NUM_COMBINATIONS;
	
	private List<List<Integer>> combinationList;
	private List<List<String>> stringCombinationList;
	private String modelStatus;
	private PerformanceMeasuresClass inSamplePMC;
	private PerformanceMeasuresClass validationSamplePMC;
	private PerformanceMeasuresClass testSamplePMC;
	
	private double timeTotalColGen, timeAddStartColumns, timePhase1, timePhase2, timeInitModel, timeMP, timePP;
	private double rootnodeObj;
	private int colGenIter;
	private int colGenNumColumnsAdded;
	private int countPPStage1, countPPStage2;
	private int countPPBNBStage1, countPPBNBStage2;
	
	private ModelType colGenType;
	
	private int numRootnodeIterations;
	private double bestLP, rootnodeGap;
	
	private int ppVNS_iter, ppVNS_numColumns;
	
	public Solution(Instance instance, double objective) {
		this.trainInstance = instance;
		this.objective = objective;
	}
	
	public int getCountPPBNBStage1() {
		return countPPBNBStage1;
	}

	public void setCountPPBNBStage1(int countPPBNBStage1) {
		this.countPPBNBStage1 = countPPBNBStage1;
	}

	public int getCountPPBNBStage2() {
		return countPPBNBStage2;
	}

	public void setCountPPBNBStage2(int countPPBNBStage2) {
		this.countPPBNBStage2 = countPPBNBStage2;
	}

	public int getPpVNS_iter() {
		return ppVNS_iter;
	}

	public void setPpVNS_iter(int ppVNS_iter) {
		this.ppVNS_iter = ppVNS_iter;
	}

	public int getPpVNS_numColumns() {
		return ppVNS_numColumns;
	}

	public void setPpVNS_numColumns(int ppVNS_numColumns) {
		this.ppVNS_numColumns = ppVNS_numColumns;
	}

	public double getRootnodeGap() {
		return rootnodeGap;
	}

	public void setRootnodeGap(double rootnodeGap) {
		this.rootnodeGap = rootnodeGap;
	}

	public int getNumRootnodeIterations() {
		return numRootnodeIterations;
	}

	public void setNumRootnodeIterations(int numRootnodeIterations) {
		this.numRootnodeIterations = numRootnodeIterations;
	}

	public double getBestLP() {
		return bestLP;
	}

	public void setBestLP(double bestLP) {
		this.bestLP = bestLP;
	}

	public int getCountPPStage1() {
		return countPPStage1;
	}

	public void setCountPPStage1(int countPPStage1) {
		this.countPPStage1 = countPPStage1;
	}

	public int getCountPPStage2() {
		return countPPStage2;
	}

	public void setCountPPStage2(int countPPStage2) {
		this.countPPStage2 = countPPStage2;
	}

	public double getRootnodeObj() {
		return rootnodeObj;
	}

	public void setRootnodeObj(double rootnodeObj) {
		this.rootnodeObj = rootnodeObj;
	}

	public int getColGenIter() {
		return colGenIter;
	}

	public void setColGenIter(int colGenIter) {
		this.colGenIter = colGenIter;
	}

	public int getColGenNumColumnsAdded() {
		return colGenNumColumnsAdded;
	}

	public void setColGenNumColumnsAdded(int colGenNumColumnsAdded) {
		this.colGenNumColumnsAdded = colGenNumColumnsAdded;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFolderName() {
		return folderName;
	}
	
	public String getFolderFileName() {
		return folderFileName;
	}

	public void setFolderFileName(String folderName, String fileName) {
		this.folderName = folderName;
		this.fileName = fileName;
		this.folderFileName = folderName + "/" + fileName + ".xlsx";
	}

	public int getSetting() {
		return setting;
	}

	public void setSetting(int setting) {
		this.setting = setting;
	}

	public double getTimeInitModel() {
		return timeInitModel;
	}

	public void setTimeInitModel(double timeInitModel) {
		this.timeInitModel = timeInitModel;
	}

	public double getTimeMP() {
		return timeMP;
	}

	public void setTimeMP(double timeMP) {
		this.timeMP = timeMP;
	}

	public double getTimePP() {
		return timePP;
	}

	public void setTimePP(double timePP) {
		this.timePP = timePP;
	}

	public ModelType getColGenType() {
		return colGenType;
	}

	public void setColGenType(ModelType colGenType) {
		this.colGenType = colGenType;
	}

	public static double getAvgValidationPMC(List<Solution> solutionList) {
		double avg = 0;
		for(Solution solution: solutionList) {
			avg += PerformanceMeasures.calcAvgMeasure(solution.getValidationSamplePMC());
		}
		avg /= solutionList.size();
		return avg;
	}
	
	public static double getAvgTestPMC(List<Solution> solutionList) {
		double avg = 0;
		for(Solution solution: solutionList) {
			avg += PerformanceMeasures.calcAvgMeasure(solution.getTestSamplePMC());
		}
		avg /= solutionList.size();
		return avg;
	}
	
	public static double getAvgNumCombinations(List<Solution> solutionList) {
		double avg = 0;
		for(Solution solution: solutionList) {
			avg += solution.getCombinationList().size();
		}
		avg /= solutionList.size();
		return avg;
	}

	public Instance getValidationInstance() {
		return validationInstance;
	}

	public void setValidationInstance(Instance validationInstance) {
		this.validationInstance = validationInstance;
	}

	public PerformanceMeasuresClass getValidationSamplePMC() {
		return validationSamplePMC;
	}

	public void setValidationSamplePMC(PerformanceMeasuresClass validationSamplePMC) {
		this.validationSamplePMC = validationSamplePMC;
	}

	public double getTimeTotalColGen() {
		return timeTotalColGen;
	}

	public void setTimeTotalColGen(double timeTotalColGen) {
		this.timeTotalColGen = timeTotalColGen;
	}

	public double getTimeAddStartColumns() {
		return timeAddStartColumns;
	}

	public void setTimeAddStartColumns(double timeAddStartColumns) {
		this.timeAddStartColumns = timeAddStartColumns;
	}

	public double getTimePhase1() {
		return timePhase1;
	}

	public void setTimePhase1(double timePhase1) {
		this.timePhase1 = timePhase1;
	}

	public double getTimePhase2() {
		return timePhase2;
	}

	public void setTimePhase2(double timePhase2) {
		this.timePhase2 = timePhase2;
	}

	public double getGap() {
		return gap;
	}

	public void setGap(double gap) {
		this.gap = gap;
	}

	public Instance getTrainInstance() {
		return trainInstance;
	}

	public void setTrainInstance(Instance trainInstance) {
		this.trainInstance = trainInstance;
	}

	public double getObjective() {
		return objective;
	}

	public void setObjective(double objective) {
		this.objective = objective;
	}

	public String getSolverType() {
		return solverType;
	}

	public void setSolverType(String solverType) {
		this.solverType = solverType;
	}

	public Instance getOriginalInstance() {
		return originalInstance;
	}

	public void setOriginalInstance(Instance originalInstance) {
		this.originalInstance = originalInstance;
	}

	public Instance getTestInstance() {
		return testInstance;
	}

	public void setTestInstance(Instance testInstance) {
		this.testInstance = testInstance;
	}

	public double getGLOBAL_TIME_LIMIT_IN_SECONDS() {
		return GLOBAL_TIME_LIMIT_IN_SECONDS;
	}

	public void setGLOBAL_TIME_LIMIT_IN_SECONDS(double gLOBAL_TIME_LIMIT_IN_SECONDS) {
		GLOBAL_TIME_LIMIT_IN_SECONDS = gLOBAL_TIME_LIMIT_IN_SECONDS;
	}

	public int getMIN_HIT_SIZE() {
		return MIN_HIT_SIZE;
	}

	public void setMIN_HIT_SIZE(int mIN_HIT_SIZE) {
		MIN_HIT_SIZE = mIN_HIT_SIZE;
	}

	public int getMAX_HIT_SIZE() {
		return MAX_HIT_SIZE;
	}

	public void setMAX_HIT_SIZE(int mAX_HIT_SIZE) {
		MAX_HIT_SIZE = mAX_HIT_SIZE;
	}

	public double getTRAIN_TEST_SPLIT() {
		return TRAIN_TEST_SPLIT;
	}

	public void setTRAIN_TEST_SPLIT(double tRAIN_TEST_SPLIT) {
		TRAIN_TEST_SPLIT = tRAIN_TEST_SPLIT;
	}

	public double getALPHA() {
		return ALPHA;
	}

	public void setALPHA(double aLPHA) {
		ALPHA = aLPHA;
	}

	public double getBETA() {
		return BETA;
	}

	public void setBETA(double bETA) {
		BETA = bETA;
	}

	public int getMAX_SELECT() {
		return MAX_SELECT;
	}

	public void setMAX_SELECT(int mAX_SELECT) {
		MAX_SELECT = mAX_SELECT;
	}

	public int getNUM_COMBINATIONS() {
		return NUM_COMBINATIONS;
	}

	public void setNUM_COMBINATIONS(int nUM_COMBINATIONS) {
		NUM_COMBINATIONS = nUM_COMBINATIONS;
	}

	public List<List<Integer>> getCombinationList() {
		return combinationList;
	}

	public List<List<String>> getStringCombinationList() {
		return stringCombinationList;
	}

	public void setStringCombinationList(List<List<String>> stringCombinationList) {
		this.stringCombinationList = stringCombinationList;
	}

	public void setCombinationList(List<List<Integer>> combinationList) {
		this.combinationList = combinationList;
	}

	public String getModelStatus() {
		return modelStatus;
	}

	public void setModelStatus(String modelStatus) {
		this.modelStatus = modelStatus;
	}

	public PerformanceMeasuresClass getInSamplePMC() {
		return inSamplePMC;
	}

	public void setInSamplePMC(PerformanceMeasuresClass inSamplePMC) {
		this.inSamplePMC = inSamplePMC;
	}

	public PerformanceMeasuresClass getTestSamplePMC() {
		return testSamplePMC;
	}

	public void setTestSamplePMC(PerformanceMeasuresClass testSamplePMC) {
		this.testSamplePMC = testSamplePMC;
	}	
}
