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

/** read "conventional" CoNLL-TSV formats, i.e. those that use 
 * 		\n\n as block separator, 
 * 		\n as row separator, 
 * 		\t as col separator,
 * 		# as comment marker, and
 * 		_ as empty annotation marker
 * Write CSV/TSV format with user-provided symbols for separators, etc.
 * Can be used to produce pre-CoNLL-05 formats that use SPACE as col separator
 */
public class Postprocessor extends StringTransformer {
	
	public Postprocessor(String blockSep, String rowSep, String colSep, String commentMarker, String empty, Map<Integer,LinkedHashMap<Pattern,String>> col2pattern2replacement) {
		super(	Pattern.compile(BLOCK_SEPARATOR), blockSep, 
				Pattern.compile(ROW_SEPARATOR), rowSep, 
				Pattern.compile(COL_SEPARATOR), colSep, 
				COMMENT_MARKER, commentMarker, 
				EMPTY_ANNOTATION, empty,
				col2pattern2replacement);
	}
	
	public Postprocessor(String blockSep, String rowSep, String colSep, String commentMarker, String empty) {
		this(blockSep, rowSep, colSep, commentMarker, empty, new Hashtable<Integer,LinkedHashMap<Pattern,String>>());
	}
	
	public static void main(String args[]) throws Exception {
		System.err.println(
				"synopsis: Postprocessor [-block BLOCK_SEPARATOR] [-row ROW_SEPARATOR] [-col COL_SEPARATOR] [-empty EMPTY] [-comment COMMENT_MARKER]\n"
				+ "                       [-replacements COL PATTERN1 REPLACEMENT1 [.. PATTERNn REPLACEMENTn]]*\n"
				+ "\tBLOCK_SEPARATOR regular expression to separates one block (sentence) from the next, defaults to "+BLOCK_SEPARATOR+"\n"
				+ "\tROW_SEPARATOR   regular expression to separate one row (word) from the next, defaults to "+ROW_SEPARATOR+"\n"
				+ "\tCOL_SEPARATOR   regular expression to separate one column (annotation) from the next, defaults to "+COL_SEPARATOR+"\n"
				+ "\tEMPTY           symbol for empty annotation, defaults to empty string (between two COL_SEPARATORs)\n"
				+ "\tCOMMENT_MARKER  symbol that marks the beginning of a comment, defaults to "+COMMENT_MARKER+"\n"
				+ "\t-replacements   marks beginning of a sequence of replacement rules, may not be followed by any other flag than -replacements\n"
				+ "\tCOL             replacement rule: column identifier (0 is first column)\n"
				+ "\tPATTERNi        replacement rule: pattern (Java regexp)\n"
				+ "\tREPLACEMENTi    replacement rule: replacement (String to replace pattern matches\n"
				+ "Read CoNLL (or other) file from stdin, replaces CoNLL default separators with user-provided replacements.\n"
				+ "Note that this is a low-level replacement of the original separators. Whitespace sequences other than separators are normalized to SPACE.\n"
				+" Note on replacements: One application is to map IOBES notation (\"B-PERSON\") to IOB notation"
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
		}
		
		
		Postprocessor me = new Postprocessor(block, row, col, comment, empty);
		
		me.transform(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
	}

}
