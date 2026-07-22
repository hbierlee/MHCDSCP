% SPDX-FileCopyrightText: 2026 Rick Willemsen
% SPDX-License-Identifier: MIT
%
% MiniZinc translation of the OR-Tools CP formulation in src/model/CP/CPModel.java
% (Multi-Hit Cancer Driver Set Cover Problem).
%
% A "combination" is a candidate set of genes (a multi-hit). A combination
% covers a sample if every gene in the combination is mutated in that
% sample (Data.geneCombnationCoversSample). At most `beta` combinations may
% be selected. A tumor sample is covered if ANY selected combination covers
% it (max/OR, see initTumorCoverConstraintsMaxOperator2); a normal sample's
% penalty is the NUMBER of selected combinations that cover it (sum, see
% initNormalCoverConstraints). The objective maximizes tumor coverage minus
% normal coverage, exactly as in CPModel.initObjective().

int: nComb;   % number of candidate gene combinations
int: nTumor;  % number of tumor samples
int: nNormal; % number of normal samples
int: beta;    % GlobalParam.BETA: max number of combinations that may be selected

array[1..nComb, 1..nTumor] of bool: coversTumor;
array[1..nComb, 1..nNormal] of bool: coversNormal;

% z_c: whether combination c is selected (combinationVars in CPModel.java)
array[1..nComb] of var bool: z;

% x_t = max_{c: covers t} z_c (tumorVars)
array[1..nTumor] of var bool: x;

% n_s = sum_{c: covers s} z_c (normalVars)
array[1..nNormal] of var int: n;

constraint forall(t in 1..nTumor) (
  x[t] <-> exists(c in 1..nComb where coversTumor[c, t]) (z[c])
);

constraint forall(s in 1..nNormal) (
  n[s] = sum(c in 1..nComb where coversNormal[c, s]) (z[c])
);

% initMaxNumCombinationsConstraints
constraint sum(c in 1..nComb) (z[c]) <= beta;

var int: objTumor = sum(t in 1..nTumor) (x[t]);
var int: objNormal = sum(s in 1..nNormal) (n[s]);

solve maximize objTumor - objNormal;

output [
  "status objective = \(objTumor - objNormal)\n",
  "tumor covered = \(objTumor) / \(nTumor)\n",
  "normal penalty = \(objNormal)\n",
  "selected combinations (1-indexed) = \([c | c in 1..nComb where fix(z[c])])\n"
];
