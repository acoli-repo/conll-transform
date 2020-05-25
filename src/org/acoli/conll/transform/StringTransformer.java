package org.acoli.conll.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** abstract superclass for classes that apply line-level replacement rules (Preprocessor, PostProcessor) 
 *  subclasses should implement main()
 * */
abstract class StringTransformer {

	public static final String BLOCK_SEPARATOR = "\n\n";
	public static final String ROW_SEPARATOR="\n";
	public static final String COL_SEPARATOR="\t";
	public static final String COMMENT_MARKER="#";
	public static final String EMPTY_ANNOTATION="";
	
	/** note that this must contain a LinkedHashMap in order to maintain replacement order */
	protected final Map<Integer,LinkedHashMap<Pattern,String>> col2pattern2replacement;

	protected final Pattern blockSepSrc;
	protected final Pattern rowSepSrc;
	protected final Pattern colSepSrc;
	protected final String commentMarkerSrc;
	protected final String emptySrc;

	protected final String blockSepTgt;
	protected final String rowSepTgt;
	protected final String colSepTgt;
	protected final String commentMarkerTgt;
	protected final String emptyTgt;

	public StringTransformer(
			Pattern blockSepSrc, String blockSepTgt, 
			Pattern rowSepSrc, String rowSepTgt, 
			Pattern colSepSrc, String colSepTgt,
			String commentMarkerSrc, String commentMarkerTgt, 
			String emptySrc, String emptyTgt,
			Map<Integer,LinkedHashMap<Pattern,String>> col2pattern2replacement) {
		this.blockSepSrc=blockSepSrc;
		this.rowSepSrc=rowSepSrc;
		this.colSepSrc=colSepSrc;
		this.commentMarkerSrc=commentMarkerSrc;
		this.emptySrc=emptySrc;
		
		this.blockSepTgt=blockSepTgt;
		this.rowSepTgt=rowSepTgt;
		this.colSepTgt=colSepTgt;
		this.commentMarkerTgt=commentMarkerTgt;
		this.emptyTgt=emptyTgt;
		
		this.col2pattern2replacement=col2pattern2replacement;
	}

	public void transform(Reader in, Writer out) throws IOException {
		BufferedReader input = new BufferedReader(in);
		
		StringBuffer buffer = new StringBuffer();
		
		for(String line = input.readLine(); line!=null; line=input.readLine()) {
			if(blockSepSrc.matcher(buffer.toString()).find()) {
				for(String b : blockSepSrc.split(buffer.toString()))
					if(b.trim().length()>0)
						writeBlock(b,out);
				buffer=new StringBuffer();
			}
			buffer.append(line+"\n");
		}
		for(String b : blockSepSrc.split(buffer.toString()))
			if(b.trim().length()>0)
				writeBlock(b,out);
		out.flush();
}

/** note that we expect the block separator to be cut off already <br/>
 *  also note that we write non-empty blocks only
 * */
protected void writeBlock(String buffer, Writer out) throws IOException {
					if(buffer.trim().length()>0) {
						for(String r : rowSepSrc.split(buffer)) {
							String content = r;
							String comment = "";
							if(r.startsWith(commentMarkerSrc)) {
								comment=r;
								content="";
							} else if(r.contains(commentMarkerSrc)) {
								comment=r.substring(r.indexOf(commentMarkerSrc)+commentMarkerSrc.length());
								content=r.substring(0,r.indexOf(commentMarkerSrc));
							}
							if(content.trim().length()>0) {
								String[] cols = colSepSrc.split(content);
								for(int i = 0; i<cols.length; i++) {
									String c = cols[i];
									if(c.trim().equals(emptySrc))
										out.write(emptyTgt);
									else {
										boolean replaced = false;
										if(this.col2pattern2replacement.get(i)!=null)
											for(Pattern pattern : col2pattern2replacement.get(i).keySet()) 
												if(!replaced && pattern.matcher(c).find()) {
													c=c.replaceAll(pattern.toString(),col2pattern2replacement.get(i).get(pattern));
													replaced=true;
												}
										c=c.replaceAll("\\s+", " ").trim();
										// if SPACE is a column separator, spaces between characters are replaced by _, 
										// spaces bordering to a non-character (e.g., punctuation) are dropped 
										if(" ".matches(this.colSepTgt))
											c=c.replaceAll("([a-zA-Z0-9]) ([a-zA-Z0-9])","$1_$2")
												.replaceAll(" ", "");
										out.write(c);
									}
									if(i<cols.length-1)
										out.write(colSepTgt);
								}
							}
							if(comment.trim().length()>0) 
								out.write(commentMarkerTgt+comment);
							out.write(rowSepTgt);
							out.flush();
						}
						out.write(blockSepTgt);
						out.flush();
					}
			}

	public String transform(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		transform(in,out);
		return out.toString();
	}
	
}
