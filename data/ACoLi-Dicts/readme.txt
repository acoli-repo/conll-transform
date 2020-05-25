TIAD-TSV format as used in the ACoLi Dictionary Graph
sample data

description: (see TIAD-TSV)
source: https://github.com/acoli-repo/acoli-dicts/blob/master/stable/apertium/apertium-rdf-2020-03-18/trans_EN-ES.tsv.gz

format:
- cols TAB-separated
- row not separated, blocks \n-separated (every row is a unit in isolation)
- adopts Turtle conventions for representing literals (language-tagged strings) and URIs (TIAD-TSV had double quotes)
- columns: 
	"source written representation" (FORM)
	"source lexical entry (URI)"
	"source sense (URI)"
	"translation (URI)"
	"target sense (URI)"
	"target lexical entry (URI)"
	"target written representation"
	"part of speech (URI)" 