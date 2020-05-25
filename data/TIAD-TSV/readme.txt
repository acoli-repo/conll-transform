TIAD-TSV (TIAD-2017, TIAD-2019, TIAD-2020) sample data

description: http://compling.hss.ntu.edu.sg/omw/
source: https://tiad2020.unizar.es/data/TranslationSetsApertiumRDF.zip

format:
- cols TAB-separated
- row not separated, blocks \n-separated (every row is a unit in isolation)
- unlike CoNLL formats, entries are enclosed in double quotes
- columns: 
	"source written representation" (FORM)
	"source lexical entry (URI)"
	"source sense (URI)"
	"translation (URI)"
	"target sense (URI)"
	"target lexical entry (URI)"
	"target written representation"
	"part of speech (URI)" 