# Identifying Multi-Hit Cancer Drivers Without Massive Parallelization: A CP, MIP, and Column Generation Framework

## Description
This repository contains a framework for identifying multi-hit combinations of gene mutations that drive cancer. The problem is formalised as the Multi-Hit Cancer Driver Set Cover Problem (MHCDSCP), for which three methods are proposed:
- Greedy Restricted Constraint Programming (GR-CP) Heuristic
- Greedy Restricted Mixed Integer Programming (GR-MIP) Heuristic
- Price-and-Branch Heuristic

Please cite the Arxiv version if you use our code (see references below)

## Software

The CP formulations are solved using the `OR-Tools` library, which is publicly available.

The MIP formulations are solved using the `CPLEX` library, which needs to be installed via [IBM CPLEX](https://www.ibm.com/docs/nl/icos/22.1.2?topic=cplex-installing).

### Dependencies
- SLF4J is used for logging. This can be removed without breaking core functionalities.

## Prepating the data

### Data formatting

We use the same data formatting as the `BRCA` and `PRAD` datasets that are publicly available via the [Oak Ridge National Laboratory Gitlab](https://code.ornl.gov/vo0/bigpicc). 

For example, the `BRCA` dataset contains two files:
- `BRCA-normal.csv`
- `BRCA-tumor.csv`

Both csv files contain a binary matrix where each row represents a gene and each column represents a sample. A matrix entry is 1 if that gene of that sample is mutated, 0 otherwise.

### Creating binary matrix
When the normal and tumor data are partially processed, the binary matrix can be obtained as follows.

1. Place normal genes and tumor data in the correct folders, see for instance `exampleNormalGeneList.txt` and `exampleTumorData.txt`.
2. Run `ConvertInstances.java`.

## Running the methods
1. Place the binary matrices in the correct folders, see for instance `exampleTumorData-normal.csv` and `exampleTumorData-tumor.csv`. 
2. (optional) Parameters can be changed in `GlobalParam.java`, e.g. time limit, hit range.
3. Run GR-CP via `RunCP.java`.
4. Run GR-MIP or price-and-branch via `RunColGenFramework.java`. The solver can be changed via `ModelType.java`. 

## References
Rick S. H. Willemsen, Tenindra Abeywickrama, Ramu Anandakrishnan, A Fast and Practical Column Generation Approach for Identifying Carcinogenic Multi-Hit Gene Combinations (2026), [arXiv:2602.22551](https://arxiv.org/abs/2602.22551).
