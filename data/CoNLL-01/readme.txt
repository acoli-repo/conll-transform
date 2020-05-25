CoNLL-01 sample data

description: https://www.clips.uantwerpen.be/conll2001/clauses/
source: https://www.clips.uantwerpen.be/conll2001/clauses/data/train1

format:
- SPACE-separated
- columns: WORD POS CHUNK CLAUSE
- CHUNK: IOB (like CoNLL-00)
- CLAUSE: S for clause break, X otherwise
	documentation mentions a bracket notation, which is not part of the train data, however
