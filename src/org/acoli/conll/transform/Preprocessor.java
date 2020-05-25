package org.acoli.conll.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** uses command-line parameters produce "normalized" CoNLL
 *  i.e., block separator, row separator, col separator, comment marker
 *  replace block separator with \n\n, row separator with \n, col separator with \t, comment marker with #
 */
public class Preprocessor extends StringTransformer {
	
	public Preprocessor(String blockSep, String rowSep, String colSep, String commentMarker, String empty, Map<Integer,LinkedHashMap<Pattern,String>> col2pattern2replacement) {
		super(	Pattern.compile(blockSep), BLOCK_SEPARATOR,
				Pattern.compile(rowSep), ROW_SEPARATOR,
				Pattern.compile(colSep), COL_SEPARATOR,
				commentMarker, COMMENT_MARKER,
				empty, EMPTY_ANNOTATION,
				col2pattern2replacement);
	}
	
	public Preprocessor(String blockSep, String rowSep, String colSep, String commentMarker, String empty) {
		this(blockSep, rowSep, colSep, commentMarker, empty, new Hashtable<Integer,LinkedHashMap<Pattern,String>>());
	}
	
	public static void main(String args[]) throws Exception {
		System.err.println(
				"synopsis: Preprocessor [-block BLOCK_SEPARATOR] [-row ROW_SEPARATOR] [-col COL_SEPARATOR] [-empty EMPTY] [-comment COMMENT_MARKER]\n"+
				"                       [-replacements COL PATTERN1 REPLACEMENT1 [.. PATTERNn REPLACEMENTn]]*\n"
				+ "\tBLOCK_SEPARATOR regular expression to separates one block (sentence) from the next, defaults to "+BLOCK_SEPARATOR+"\n"
				+ "\tROW_SEPARATOR   regular expression to separate one row (word) from the next, defaults to "+ROW_SEPARATOR+"\n"
				+ "\tCOL_SEPARATOR   regular expression to separate one column (annotation) from the next, defaults to "+COL_SEPARATOR+"\n"
				+ "\tEMPTY           symbol for empty annotation, defaults to empty string (between two COL_SEPARATORs)\n"
				+ "\tCOMMENT_MARKER  symbol that marks the beginning of a comment, defaults to "+COMMENT_MARKER+"\n"
				+ "\t-replacements   marks beginning of a sequence of replacement rules, may not be followed by any other flag than -replacements\n"
				+ "\tCOL             replacement rule: column identifier (0 is first column)\n"
				+ "\tPATTERNi        replacement rule: pattern (Java regexp)\n"
				+ "\tREPLACEMENTi    replacement rule: replacement (String to replace pattern matches\n"
				+ "Read CoNLL (or other) file from stdin, replaces separators and comment markers with CoNLL defaults.\n"
				+ "Note that this is a low-level replacement of the original separators. Whitespace sequences other than separators are normalized to SPACE.\n"
				+" Note that if multiple replacements apply to a column, we apply the first in argument order"
				+" Note on replacements: One application is to map IOBES notation (\"B-PERSON\") to plain (\"PERSON\") or bracket encoding (\"(PERSON\")"
				);
		String block = BLOCK_SEPARATOR;
		String row = ROW_SEPARATOR;
		String col = COL_SEPARATOR;
		String comment = COMMENT_MARKER;
		String empty = EMPTY_ANNOTATION;
		
		Map<Integer,LinkedHashMap<Pattern,String>> col2pattern2replacement = new Hashtable<Integer,LinkedHashMap<Pattern,String>>();
		
		for(int i = 0; i<args.length; i++) {
			if(args[i].startsWith("-block"))
				block=args[++i];
			else if(args[i].startsWith("-row"))
				row=args[++i];
			else if(args[i].startsWith("-col"))
				col=args[++i];
			else if(args[i].startsWith("-comment"))
				comment=args[++i];
			else if(args[i].startsWith("-empty"))
				empty=args[++i];
			else if(args[i].startsWith("-replacements")) { // must be last, can only be followed by other -replacements
				while(i<args.length-1) {
					int coli = -1;
					try {
						coli=Integer.parseInt(args[++i]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						System.err.println("while processing\""+args[i]+"\"");
					}
					if(col2pattern2replacement.get(coli)==null)
						col2pattern2replacement.put(coli, new LinkedHashMap<Pattern,String>());
					while(i<args.length-1 && !args[i].startsWith("-replacements")) {
						Pattern patterni = null;
						try { 
							patterni = Pattern.compile(args[++i]);
						} catch (PatternSyntaxException e) {
							e.printStackTrace();
							System.err.println("while processing \""+args[i]+"\"");
						}
						String replacementi = args[++i];
						if(patterni!=null && col2pattern2replacement.get(coli).get(patterni)==null)
							col2pattern2replacement.get(coli).put(patterni, replacementi);
					}	
				}
			}
		}
		
		Preprocessor me = new Preprocessor(block, row, col, comment, empty, col2pattern2replacement);
		
		me.transform(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
	}

}
