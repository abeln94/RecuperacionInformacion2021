import argparse
from collections import defaultdict

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
ELEVEN_POINTS = [i / 10 for i in range(11)]


def avg(l, default=0):
    """ Returns the average of the list, or default if the list is empty"""
    return sum(l) / len(l) if len(l) != 0 else default


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

    # calculate all values
    measures = defaultdict(lambda: [])
    for inf_need, doc_ids in results.groupby([INFORMATION_NEED]):
        # for each information need

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
        for doc_id in doc_ids[DOCUMENT_ID].values:
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
            inf_need
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
            2 * p * r / (p + r)  # from last calculated precision and recall
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
            [
                (
                    interp,
                    max((p for r, p, _ in recall_precision_list if r >= interp), default=0)
                )
                for interp in ELEVEN_POINTS
            ]
        )

    # open output file to write results
    with open(args.output, 'w') as output:
        def fprintln(*args):
            """Prints a formatted line (substitutes a normal println)"""
            output.write('\t'.join([
                '%.3f' % x
                if isinstance(x, int) or isinstance(x, float)
                else str(x)
                for x in args]) + '\n')


        # print each information need
        for measure in range(len(measures[INFORMATION_NEED])):
            for label in [INFORMATION_NEED, PRECISION, RECALL, F1, PREC10, AVG_PREC]:
                fprintln(label, measures[label][measure])

            fprintln(RECALL_PRECISION)
            for r, p in measures[RECALL_PRECISION][measure]:
                fprintln(r, p)

            fprintln(INTERPOLATED_RECALL_PRECISION)
            for r, p in measures[INTERPOLATED_RECALL_PRECISION][measure]:
                fprintln(r, p)
            fprintln()

        # print average total
        fprintln(TOTAL)
        for label in [PRECISION, RECALL, F1, PREC10]:
            fprintln(label, avg(measures[label]))

        fprintln(MAP, avg(measures[AVG_PREC]))

        fprintln(INTERPOLATED_RECALL_PRECISION)
        for list_recall_precision in zip(*measures[INTERPOLATED_RECALL_PRECISION]):
            fprintln(list_recall_precision[0][0], avg([x for _, x in list_recall_precision]))  # average of lists element by element

        fprintln()
