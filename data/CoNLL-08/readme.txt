CoNLL-08 sample data

description: http://surdeanu.info/conll08/
source: http://surdeanu.info/conll08/trial.closed

format:
- TAB-separated
- columns: 
	1 ID Token counter, starting at 1 for each new sentence.
	2 FORM Unsplit word form or punctuation symbol.
	3 LEMMA Predicted lemma of FORM.
	4 GPOS Gold part-of-speech tag from the Treebank (empty at test time).
	5 PPOS Predicted POS tag.
	6 SPLIT FORM Tokens split at hyphens and slashes.
	7 SPLIT LEMMA Predicted lemma of SPLIT FORM.
	8 PPOSS Predicted POS tags of the split forms.
	9 HEAD Syntactic head of the current token, which is either a value of ID or zero (0).
	10 DEPREL Syntactic dependency relation to the HEAD.
	11 PRED Rolesets of the semantic predicates in this sentence.
	12 ... ARG