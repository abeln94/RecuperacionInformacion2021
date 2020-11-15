import argparse
from collections import defaultdict

import pandas as pd

INFORMATION_NEED = 'INFORMATION_NEED'
DOCUMENT_ID = 'DOCUMENT_ID'
RELEVANCY = 'RELEVANCY'

if __name__ == '__main__':
    # args
    parser = argparse.ArgumentParser(description='Main file.')
    parser.add_argument('-qrels', action="store", required=True, type=str, help='relevance file')
    parser.add_argument('-results', action="store", required=True, type=str, help='results file')
    parser.add_argument('-output', action="store", required=True, type=str, help='output file')
    args = parser.parse_args()

    # read files
    qrels = pd.read_csv(args.qrels, delimiter='\t', names=[INFORMATION_NEED, DOCUMENT_ID, RELEVANCY], header=None)
    results = pd.read_csv(args.results, delimiter='\t', names=[INFORMATION_NEED, DOCUMENT_ID], header=None)

    # parse
    with open(args.output, 'w') as output:
        def fprintln(*args):
            output.write('\t'.join([
                '%.3f' % x
                if isinstance(x, int) or isinstance(x, float)
                else str(x)
                for x in args]) + '\n')


        for inf_need, doc_ids in results.groupby([INFORMATION_NEED]):
            doc_ids = doc_ids[DOCUMENT_ID].values

            # values
            allDocs = set(qrels[DOCUMENT_ID].values).union(doc_ids)

            prec_rec_list = []
            rec = set()
            notRec = set(allDocs)
            rel = set(qrels[(qrels[INFORMATION_NEED] == inf_need) & (qrels[RELEVANCY] == 1)][DOCUMENT_ID].values)
            notRel = allDocs - rel

            for doc_id in doc_ids:
                rec.add(doc_id)
                notRec.remove(doc_id)

                tp = len(rec.intersection(rel))
                fp = len(rec.intersection(notRel))
                fn = len(notRec.intersection(rel))

                p = tp / (tp + fp)
                r = tp / (tp + fn)

                prec_rec_list.append((p, r, doc_id in rel))

            # header
            fprintln(INFORMATION_NEED, inf_need)

            # precision
            fprintln('precision', p)

            # recall
            fprintln('recall', r)

            # f1
            f1 = 2 * p * r / (p + r)
            fprintln('f1', f1)

            # prec@10
            prec10 = prec_rec_list[9][0] if len(prec_rec_list) >= 10 else tp / 10
            fprintln('prec@10', prec10)

            # average_precision
            prec_list = [p for p, _, r in prec_rec_list if r]
            avg_prec = sum(prec_list) / len(prec_list) if len(prec_list) > 0 else 0
            fprintln('avg_prec', avg_prec)

            # recall_precision
            fprintln('recall_precision')
            for p, r, rel in prec_rec_list:
                if rel:
                    fprintln(r, p)

            # interpolated_recall_precision
            int_rec_precs = [(interp, max([0] + [p for p, r, _ in prec_rec_list if r >= interp])) for interp in [i / 10 for i in range(11)]]
            fprintln('interpolated_recall_precision')
            for r, p in int_rec_precs:
                fprintln(r, p)

            fprintln()

    pass
