#!/usr/bin/env python3
"""
Reads a MHCDSCP instance (the same CSV format as Instance.readData in
src/data/Instance.java: "numGenes,numSamples" header, then
"geneIdx,geneName,bit_0,bit_1,...") and emits a MiniZinc .dzn data file
for minizinc/mhcdscp.mzn.

Unlike PricingProblemRandom.java (which randomly samples BETA-worth of
candidate combinations for scalability on large instances), this script
enumerates ALL gene combinations with size in [min_hit, max_hit]. That is
exact and tractable for small example instances such as exampleTumorData,
and it reproduces the exact CP formulation in CPModel.java without
depending on PricingProblemRandom's seeded RNG.
"""
import argparse
import csv
import itertools
import sys


def read_data(path):
    with open(path, newline="") as f:
        reader = csv.reader(f)
        rows = [row for row in reader if row]
    num_genes, num_samples = int(rows[0][0]), int(rows[0][1])
    gene_names = []
    mat = []
    for row in rows[1:1 + num_genes]:
        gene_names.append(row[1])
        mat.append([int(v) == 1 for v in row[2:2 + num_samples]])
    return gene_names, mat, num_samples


def covers(mat, combination, sample_idx):
    return all(mat[g][sample_idx] for g in combination)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("tumor_csv")
    ap.add_argument("normal_csv")
    ap.add_argument("-o", "--output", required=True)
    ap.add_argument("--min-hit", type=int, default=2, help="GlobalParam.MIN_HIT_SIZE")
    ap.add_argument("--max-hit", type=int, default=3, help="GlobalParam.MAX_HIT_SIZE")
    ap.add_argument("--beta", type=int, default=10, help="GlobalParam.BETA")
    args = ap.parse_args()

    tumor_genes, tumor_mat, n_tumor = read_data(args.tumor_csv)
    normal_genes, normal_mat, n_normal = read_data(args.normal_csv)
    assert tumor_genes == normal_genes, "gene lists in tumor/normal files must match"
    n_genes = len(tumor_genes)

    combinations = []
    for size in range(args.min_hit, args.max_hit + 1):
        combinations.extend(itertools.combinations(range(n_genes), size))

    n_comb = len(combinations)

    def bool_row(vals):
        return "[" + ", ".join("true" if v else "false" for v in vals) + "]"

    with open(args.output, "w") as f:
        f.write(f"nComb = {n_comb};\n")
        f.write(f"nTumor = {n_tumor};\n")
        f.write(f"nNormal = {n_normal};\n")
        f.write(f"beta = {args.beta};\n\n")

        f.write("% combination legend (1-indexed -> gene names)\n")
        for idx, combo in enumerate(combinations, start=1):
            names = "+".join(tumor_genes[g] for g in combo)
            f.write(f"% {idx}: {names}\n")
        f.write("\n")

        f.write("coversTumor = [| ")
        f.write("\n                | ".join(
            ", ".join("true" if covers(tumor_mat, combo, t) else "false" for t in range(n_tumor))
            for combo in combinations
        ))
        f.write(" |];\n\n")

        f.write("coversNormal = [| ")
        f.write("\n                 | ".join(
            ", ".join("true" if covers(normal_mat, combo, s) else "false" for s in range(n_normal))
            for combo in combinations
        ))
        f.write(" |];\n")

    print(f"Wrote {args.output}: {n_genes} genes, {n_comb} combinations "
          f"(sizes {args.min_hit}-{args.max_hit}), {n_tumor} tumor samples, "
          f"{n_normal} normal samples, beta={args.beta}", file=sys.stderr)


if __name__ == "__main__":
    main()
