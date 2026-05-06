package model.MIP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.Data;
import data.GlobalParam;
import data.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
/**
 * SPDX-FileCopyrightText: 2026 Rick Willemsen
 * SPDX-License-Identifier: MIT
 * 
 * Solve the pricing problem to optimality using a MIP solver.
 */
public class PricingProblemMIP {
	private static Logger log = LoggerFactory.getLogger(PricingProblemMIP.class);
	
	private Instance instance;
	private Data dataTumor;
	private Data dataNormal;
	private int numGenes;

	private IloCplex model;

	private IloNumVar[] tumorVars;
	private IloNumVar[] normalVars;
	private IloNumVar[] geneVars;
	private IloObjective objective;
	
	private int countStage1, countStage2;

	public PricingProblemMIP(Instance instance) throws IloException {
		this.instance = instance;
		this.dataTumor = instance.getDataTumor();
		this.dataNormal = instance.getDataNormal();
		this.numGenes = instance.getDataTumor().getNumGenes();
		
		this.model = new IloCplex();
		model.setOut(null);
		model.setParam(IloCplex.Param.MIP.Limits.Solutions, 1);
		model.setParam(IloCplex.Param.MIP.Tolerances.LowerCutoff, GlobalParam.EPSILON6);

		initModel();
	}

	private void initModel() throws IloException {
		initEmptyMaps();
		initTumorVariables();
		initNormalVariables();
		initGeneVariables();
		initMinMaxNumGenesConstraints();
		initTumorCoverConstraints();
		initNormalCoverConstraints();
	}
	
	private void initEmptyMaps() {
		countStage1 = 0;
		countStage2 = 0;
	}

	private void initTumorVariables() throws IloException {
		tumorVars = new IloNumVar[dataTumor.getNumSamples()];	
		for(int i = 0; i < dataTumor.getNumSamples(); i++) {
			tumorVars[i] = model.boolVar();
		}
	}

	private void initNormalVariables() throws IloException {
		normalVars = new IloNumVar[dataNormal.getNumSamples()];	
		for(int i = 0; i < dataNormal.getNumSamples(); i++) {
			normalVars[i] = model.boolVar();
		}
	}

	private void initGeneVariables() throws IloException {
		geneVars = new IloNumVar[numGenes];	
		for(int i = 0; i < numGenes; i++) {
			geneVars[i] = model.boolVar();
		}
	}

	private void initMinMaxNumGenesConstraints() throws IloException {
		IloNumExpr exprMin = model.constant(GlobalParam.MIN_HIT_SIZE);
		IloNumExpr exprMax = model.constant(-GlobalParam.MAX_HIT_SIZE);

		for(int i = 0; i < numGenes; i++) {
			exprMin = model.diff(exprMin, geneVars[i]);
			exprMax = model.sum(exprMax, geneVars[i]);
		}

		model.addLe(exprMin, 0);
		model.addLe(exprMax, 0);
	}

	private void initTumorCoverConstraints() throws IloException {
		for(int i = 0; i < dataTumor.getNumSamples(); i++) {
			IloNumExpr expr = model.constant(1);
			expr = model.diff(expr, tumorVars[i]);

			for(int g = 0; g < numGenes; g++) {
				if(!dataTumor.getMat()[g][i]) {
					expr = model.diff(expr, model.prod(1/((double) GlobalParam.MAX_HIT_SIZE), geneVars[g]));
				}
			}
			model.addGe(expr, 0);
		}
	}

	private void initNormalCoverConstraints() throws IloException {
		for(int i = 0; i < dataNormal.getNumSamples(); i++) {
			IloNumExpr expr = model.constant(1);
			expr = model.diff(expr, normalVars[i]);

			for(int g = 0; g < numGenes; g++) {
				if(!dataNormal.getMat()[g][i]) {
					expr = model.diff(expr, geneVars[g]);
				}
			}
			model.addLe(expr, 0);
		}
	}

	private void initObjective(DualValues dualValues) throws IloException {
		if(objective!=null) {
			model.delete(objective);
		}
		
		// Clear previous objective if needed
		IloNumExpr objectiveExpr = model.constant(-dualValues.getLambd());

		// Tumor part: maximize sum π_t * v_t
		for (int i = 0; i < dataTumor.getNumSamples(); i++) {
			double coeff = dualValues.getTumorDuals()[i];
			objectiveExpr = model.sum(objectiveExpr, model.prod(coeff, tumorVars[i]));
		}

		// Normal part: subtract μ_n * v_n
		for (int i = 0; i < dataNormal.getNumSamples(); i++) {
			double coeff = -dualValues.getNormalDuals()[i];
			objectiveExpr = model.sum(objectiveExpr, model.prod(coeff, normalVars[i]));
		}

		objective = model.addObjective(IloObjectiveSense.Maximize, objectiveExpr);
	}

	public int getCountStage1() {
		return countStage1;
	}

	public int getCountStage2() {
		return countStage2;
	}

	public List<Integer> solvePP(DualValues dualValues, Set<Integer> selectedGeneSet) throws IloException {
		initObjective(dualValues);
		
		List<Integer> combination = null; 
		if(GlobalParam.PP_SOLVE_REDUCED_MIP) {
			int count = 0;
			for(int i = 0; i < numGenes; i++) {
				if(!selectedGeneSet.contains(i)) {
					geneVars[i].setUB(0);
					count++;
				}
			}
			
			log.info("Stage 1: fix genes: {}", count);
			combination = solveMIP();
			
			if(combination!=null) {
				countStage1++;
				return combination;
			}
			
			for(int i = 0; i < numGenes; i++) {
				if(!selectedGeneSet.contains(i)) {
					geneVars[i].setUB(1);
				}
			}
		}
		
		log.info("Stage 2: unfix genes");
		combination = solveMIP();
		if(combination!=null) {
			countStage2++;
			return combination;	
		}
		return null;
	}
	
	private List<Integer> solveMIP() throws IloException {
		model.solve();
		
		if(!model.isPrimalFeasible()) {
			log.info("PP infeasible");
			return null;
		}
		log.info("PP objective (RC) = {}", model.getObjValue());
		
		// get solution
		List<Integer> combination = new ArrayList<>();
		for(int i = 0; i < numGenes; i++) {
			if(model.getValue(geneVars[i])>0.5) {
				combination.add(i);
			}
		}
		
		log.info("New column (combination) found: {}", combination);
		return combination;
	}
	
	public void clearModel() throws IloException {
		model.clearModel();
		model.end();
	}
}
