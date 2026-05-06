package run;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import data.GlobalParam;
import data.Instance;
import data.Solution;
import ilog.concert.IloException;
import model.CP.CPModel;
import performanceMeasures.PerformanceMeasures;
import performanceMeasures.PerformanceMeasuresClass;
import util.Pair;
import util.SpreadsheetGenerator;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 */
public class RunCP {

	public static void main(String[] args) throws IloException, IOException {
		String date_file = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		String folderName = "data/output";
		String fileName = "exampleTumorData";
		Instance instance = Instance.readFromFile("data/instances", fileName);
		
		Pair<Instance, Instance> pairInstance = Instance.splitTrainTestInstance(instance, GlobalParam.TRAIN_TEST_SPLIT);
		
		CPModel model = new CPModel(pairInstance.first);
		model.solve();
		model.printSolution();
		Solution solution = model.getSolution();
		
		Instance testInstance = pairInstance.second;
		PerformanceMeasuresClass pmc = PerformanceMeasures.getOutSamplePerformanceMeasuresClass(testInstance, solution.getCombinationList());
		PerformanceMeasures.print(pmc);
		
		solution.setSolverType("runCP");
		solution.setOriginalInstance(instance);
		solution.setTestInstance(testInstance);
		solution.setTestSamplePMC(pmc);
		solution.setFolderFileName(folderName, date_file+"_"+instance.getWriteToFileName());
		SpreadsheetGenerator.writeSpreadsheet(solution);
	}

}
