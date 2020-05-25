CoNLL-99 sample data

description: https://www.clips.uantwerpen.be/conll99/npb/
source: ftp://ftp.cis.upenn.edu/pub/chunker/
data representation in IOB (Ramshaw & Marcus 1995, cf. https://www.aclweb.org/anthology/E99-1023.pdf, "IOB1") 

format:
- space-separated
- columns: WORD POS CHUNK1 CHUNK2 RELATION FULL_WORD (labels by me)
- IOB-like conventions, but unlabeled 
  
  IOB conventions (here)
  O - no annotation
  B - begin of phrase (if following another phrase)
  I - phrase (incl. begin of phrase if initial or following O)
  
  interpretation (by me)
	COL3 ("CHUNK1"): [mostly] nominal chunks 
	COL4 ("CHUNK2"): [mostly] nominal chunks
	COL5 ("RELATION"): marks prepositions, connectives (incl. punctuation) and verb chunks
	COL6 ("FULL_WORD"): B if token, O if stripped morpheme ('s)

  CHUNKS1 and CHUNKS2: apparently alternative, independent annotators
	- can be identical ("[the pound]")
	- CHUNK2 can contain elements that are unmarked in CHUNK1 ("(due) for release")
	- CHUNK1 can be a larger phrase ("[July and August]" vs. "[July] and [August]")
	- CHUNK2 can be a larger phrase ("[release] [tomorrow]" vs. "[release tomorrow]")