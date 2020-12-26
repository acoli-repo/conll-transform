
# conll-transform
seamless conversion between different CoNLL/TSV dialects

generates transformation workflows for CoNLL-RDF/Fintan
(currently: bash scripts, upcoming: JSON configurations)

open issues:
- call with CoNLLCorpusManager JSON config file
- deprecate bash output OR provide proper escaping

## Synopsis


./transform.sh [-help]
./transform.sh SRC TGT [OWL]
  -help list all supported formats
  SRC   source format
  TGT   target format
  OWL   CoNLL ontology, TTL format, defaults to ./conll-rdf/owl/conll.ttl
read CoNLL data from stdin, write to stdout
transform from SRC format to TGT format according to OWL

## Sample call

`$ ./transform.sh CoNLL-99 CoNLL-03`

> loading CoNLL-RDF ontology ./conll-rdf/owl/conll.ttl
building bash script
>
> 1. configure preprocessing
warning: unsupported input encoding "iobEncoding"
warning: unsupported input encoding "iobEncoding"
warning: unsupported input encoding "iobEncoding"
>
> 2. configure extraction
>
> 3. configure update
generalization: VCHUNK => CHUNK
warning: no mapping for target format property NER
warning: no mapping for source format property NCHUNK
warning: no mapping for source format property CLAUSE
>
> 4. configure formatter
>
> 5. configure postprocessing
warning: unsupported output encoding "iobEncoding"
>
> 6. writing script
>
> `java -classpath bin org.acoli.conll.transform.Preprocessor -col " " | \`
`./run.sh CoNLLStreamExtractor http://ignore.me/ WORD POS NCHUNK CLAUSE VCHUNK | \`
`./run.sh CoNLLRDFUpdater -updates PREFIX conll: <http://ufal.mff.cuni.cz/conll2009-st/task-description.html#>
INSERT { ?a conll:CHUNK ?b } WHERE { ?a conll:VCHUNK ?b}; | \`
`./run.sh CoNLLRDFFormatter -conll WORD POS CHUNK NER | \`
`java -classpath bin org.acoli.conll.transform.Postprocessor -col " "`

