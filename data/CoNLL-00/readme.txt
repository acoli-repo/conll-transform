CoNLL-00 sample data

description: https://www.clips.uantwerpen.be/conll2000/chunking/
source: https://www.clips.uantwerpen.be/conll2000/chunking/test.txt.gz

format:
- SPACE-separated
- columns: WORD POS CHUNK
- CHUNK: "classical" IOB annotation
	labels
	B- *always* used to mark beginning
