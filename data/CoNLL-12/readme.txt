CoNLL-12 sample data

description: http://conll.cemantix.org/2012/data.html
source: http://conll.cemantix.org/2012/download/conll-2012-train.v4.tar.gz, conll-2012/v4/data/train/data/chinese/annotations/bn/cnr/00/cnr_0001.v4_gold_skel (excluding word forms)

format:
- TAB-separated
- columns: 
	Column	Type	Description
	1	Document ID	This is a variation on the document filename
	2	Part number	Some files are divided into multiple parts numbered as 000, 001, 002, ... etc.
	3	Word number	
	4	Word itself	This is the token as segmented/tokenized in the Treebank. Initially the *_skel file contain the placeholder [WORD] which gets replaced by the actual token from the Treebank which is part of the OntoNotes release.
	5	Part-of-Speech	
	6	Parse bit	This is the bracketed structure broken before the first open parenthesis in the parse, and the word/part-of-speech leaf replaced with a *. The full parse can be created by substituting the asterix with the "([pos] [word])" string (or leaf) and concatenating the items in the rows of that column.
	7	Predicate lemma	The predicate lemma is mentioned for the rows for which we have semantic role information. All other rows are marked with a "-"
	8	Predicate Frameset ID	This is the PropBank frameset ID of the predicate in Column 7.
	9	Word sense	This is the word sense of the word in Column 3.
	10	Speaker/Author	This is the speaker or author name where available. Mostly in Broadcast Conversation and Web Log data.
	11	Named Entities	These columns identifies the spans representing various named entities.
	12:N	Predicate Arguments	There is one column each of predicate argument structure information for the predicate mentioned in Column 7.
	N	Coreference	Coreference chain information encoded in a parenthesis structure.
