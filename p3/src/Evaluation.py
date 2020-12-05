"""
Instrucciones de ejecución:
- Instalar los paquetes pandas (pip install pandas) y matplotlib (pip install matplotlib)
- Ejecutar con los argumentos especificados en el enunciado (si se ejecuta sin argumentos o con '-h' mostrará cuales son)
"""
import argparse
from collections import defaultdict

import matplotlib.pyplot as plt
import pandas as pd

# input texts
INFORMATION_NEED = 'INFORMATION_NEED'
DOCUMENT_ID = 'DOCUMENT_ID'
RELEVANCY = 'RELEVANCY'

# output texts
INTERPOLATED_RECALL_PRECISION = 'interpolated_recall_precision'
RECALL_PRECISION = 'recall_precision'
AVG_PREC = 'avg_prec'
PREC10 = 'prec@10'
F1 = 'f1'
RECALL = 'recall'
PRECISION = 'precision'
MAP = 'MAP'
TOTAL = 'TOTAL'

# utils
INTERPOLATION_POINTS = [i / 10 for i in range(11)]


def avg(l, default=0):
    """ Returns the average of the list, or default if the list is empty"""
    return sum(l) / len(l) if len(l) != 0 else default


def hmean(a, b):
    """ Harmonic mean of two numbers """
    return 2 * a * b / (a + b)


if __name__ == '__main__':

    # init args
    parser = argparse.ArgumentParser(description='Main file.')
    parser.add_argument('-qrels', action="store", required=True, type=str, help='relevance file')
    parser.add_argument('-results', action="store", required=True, type=str, help='results file')
    parser.add_argument('-output', action="store", required=True, type=str, help='output file')
    args = parser.parse_args()

    # read files
    qrels = pd.read_csv(args.qrels, delimiter='\t', names=[INFORMATION_NEED, DOCUMENT_ID, RELEVANCY], header=None)
    results = pd.read_csv(args.results, delimiter='\t', names=[INFORMATION_NEED, DOCUMENT_ID], header=None)

    # calculate all measures
    measures = defaultdict(lambda: [])
    for inf_need, group in results.groupby([INFORMATION_NEED]):
        # for each information need

        # get the documents (45 max)
        doc_ids = group[DOCUMENT_ID].head(45).values

        # values
        all = set(qrels[DOCUMENT_ID].values).union(doc_ids)
        recuperated = set()
        notRecuperated = set(all)
        relevant = set(qrels[(qrels[INFORMATION_NEED] == inf_need) & (qrels[RELEVANCY] == 1)][DOCUMENT_ID].values)
        notRelevant = all - relevant

        # default values in case doc_ids is empty
        p = 0
        r = 1
        tp = 0

        # calculate the precision and recall for each returned document in order
        recall_precision_list = []
        for doc_id in doc_ids:
            recuperated.add(doc_id)
            notRecuperated.remove(doc_id)

            tp = len(recuperated.intersection(relevant))
            fp = len(recuperated.intersection(notRelevant))
            fn = len(notRecuperated.intersection(relevant))

            p = tp / (tp + fp)
            r = tp / (tp + fn)

            recall_precision_list.append((r, p, doc_id in relevant))

        # header
        measures[INFORMATION_NEED].append(
            str(inf_need)
        )

        # precision
        measures[PRECISION].append(
            p  # last calculated precision, = recall_precision_list[-1][1]
        )

        # recall
        measures[RECALL].append(
            r  # last calculated recall, = recall_precision_list[-1][0]
        )

        # f1
        measures[F1].append(
            hmean(p, r)  # from last calculated precision and recall
        )

        # prec@10
        measures[PREC10].append(
            recall_precision_list[9][1] if len(recall_precision_list) >= 10 else tp / 10  # tp from last calculated
        )

        # average_precision
        measures[AVG_PREC].append(
            avg([p for _, p, k in recall_precision_list if k])
        )

        # recall_precision
        measures[RECALL_PRECISION].append(
            [(r, p) for r, p, k in recall_precision_list if k]
        )

        # interpolated_recall_precision
        measures[INTERPOLATED_RECALL_PRECISION].append(
            {interpolation: max((p for r, p, _ in recall_precision_list if r >= interpolation), default=0) for interpolation in INTERPOLATION_POINTS}
        )

    # open output file to write results
    with open(args.output, 'w') as output:

        def fprintln(*args):
            """ Prints a formatted line (substitutes a normal println) """
            output.write('\t'.join([
                '%.3f' % x if isinstance(x, int) or isinstance(x, float)  # three decimals for numbers
                else str(x)
                for x in args]) + '\n')


        # print each information need
        n = len(measures[INFORMATION_NEED])
        for i in range(n):
            for label in [INFORMATION_NEED, PRECISION, RECALL, F1, PREC10, AVG_PREC]:
                fprintln(label, measures[label][i])

            fprintln(RECALL_PRECISION)
            for r, p in measures[RECALL_PRECISION][i]:
                fprintln(r, p)

            fprintln(INTERPOLATED_RECALL_PRECISION)
            for r, p in measures[INTERPOLATED_RECALL_PRECISION][i].items():
                fprintln(r, p)
            fprintln()

        # print average total
        fprintln(TOTAL)
        fprintln(PRECISION, avg(measures[PRECISION]))
        fprintln(RECALL, avg(measures[RECALL]))
        fprintln(F1, hmean(avg(measures[PRECISION]), avg(measures[RECALL])))
        fprintln(PREC10, avg(measures[PREC10]))

        fprintln(MAP, avg(measures[AVG_PREC]))

        fprintln(INTERPOLATED_RECALL_PRECISION)
        for interp in INTERPOLATION_POINTS:
            fprintln(interp, avg([measures[INTERPOLATED_RECALL_PRECISION][i][interp] for i in range(n)]))

        fprintln()

    # graphs

    # interpolated precision recall graphs
    for i in range(n):
        plt.plot(INTERPOLATION_POINTS, [measures[INTERPOLATED_RECALL_PRECISION][i][interp] for interp in INTERPOLATION_POINTS], label=f"information need {i + 1}")

    plt.plot(INTERPOLATION_POINTS, [avg([measures[INTERPOLATED_RECALL_PRECISION][i][interp] for i in range(n)]) for interp in INTERPOLATION_POINTS], label='Total')

    plt.legend()
    plt.ylabel('precision')
    plt.xlabel('exhaustividad (recall)')
    plt.show()
