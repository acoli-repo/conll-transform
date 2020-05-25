CoNLL-X (CoNLL 2006) sample data
same format used for CoNLL 2007

description (CoNLL 2007): https://depparse.uvt.nl/SharedTaskWebsite.html, https://depparse.uvt.nl/DataFormat.html
description (CoNLL-X, mirror): http://mwetoolkit.sourceforge.net/PHITE.php?sitesig=MWE&page=MWE_070_File_types&subpage=MWE_010_CONLL
orginal description (CoNLL-X, offline): https://ilk.uvt.nl/conll/#dataformat (offline)
source: https://gitlab.com/mwetoolkit/mwetoolkit2-legacy/-/blob/master/test/filetype-samples/corpus.conll
original source (CoNLL-X, offline): https://ilk.uvt.nl/conll

format:
- TAB-separated
- columns: 
	Column 1: "ID": Word index (starts at 1).
	Column 2: "FORM": Surface form of the word.
	Column 3: "LEMMA": Lemmatized form of the word.
	Column 4: "CPOSTAG": Coarse-grained POS-tag
	Column 5: "POSTAG": Fine-grained POS-tag.
	Column 6: "FEATS": Set of features 
	Column 7: "HEAD": Head of the current token for dependency relation.
	Column 8: "DEPREL": The dependency relation.
	Column 9: "PHEAD": Projective head 
	Column 10: "PDEPREL": The dependency relation for PHEAD