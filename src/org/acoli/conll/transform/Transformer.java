package org.acoli.conll.transform;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import org.apache.commons.cli.*;

// TODO: property mapping using rdfs:subPropertyOf
// TODO: property mapping using SPARQL Update scripts
// TODO: complain about mapping errors/imprecise mapping
// TODO: precompile into hashtable, this is *SLOW*
// TODO: semantic role serialization (aggregate property PRED_ARGS [better: PRED-ARGS]) [previously implemented in Centering experiment]
public class Transformer {

	final Model model;
	final String src;
	private final List<String> srcCols;
	private final List<String> tgtCols;
	private final Map<String,Map<String,Integer>> sub2super2dist;
	final String tgt;
	final String conllPrefix;
	final static String CONLL_PREFIX = "http://purl.org/acoli/conll#";
	final static String CONLL_PREFIX_OLD = "http://ufal.mff.cuni.cz/conll2009-st/task-description.html#";
	final static String DEFAULT_BASEURI = "http://ignore.me/"; // used as default URI for converting data
	final static String DEFAULT_VERSION = "1";

	/** encoding features, use local names of properties as keys */
	final Map<String,String> property2featureSrc;

	/** encoding features, use local names of properties as keys */
	final Map<String,String> property2featureTgt;
	
	Transformer(String src, String tgt, String owl, String conllPrefix) {
		System.err.println("loading CoNLL-RDF ontology "+owl);
		this.conllPrefix = conllPrefix;
		model = ModelFactory.createDefaultModel() ;
		model.read(owl);
		
		String query =
				"PREFIX : <"+CONLL_PREFIX+">\n"+
				"ASK { :"+src+" a :Dialect }";
		if(QueryExecutionFactory.create(QueryFactory.create(query), model).execAsk())
			this.src=src;
		else {
			this.src=null;
			System.err.println("warning: did not find source format \""+src+"\".");
		}
		
		query =
				"PREFIX : <"+CONLL_PREFIX+">\n"+
				"ASK { :"+tgt+" a :Dialect }";
		if(QueryExecutionFactory.create(QueryFactory.create(query), model).execAsk())
			this.tgt=tgt;
		else {
			this.tgt=null;
			System.err.println("warning: did not find target format \""+tgt+"\".");
		}
		
		if(src==null || tgt ==null) {
			System.err.println("supported formats: ");
			query=
					"PREFIX : <"+CONLL_PREFIX+">\n"+
					"SELECT DISTINCT ?format "
					+ "WHERE { "
					+ "	?dialect a :Dialect ."
					+ "	BIND(replace(str(?dialect),'.*[#/]','') AS ?format)"
					+ "} ORDER BY ?format";
			ResultSet results = QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect();
			while(results.hasNext())
				System.err.println("\t"+results.next().getLiteral("?format"));
		}
		
		srcCols = getCols(src);
		tgtCols = getCols(tgt);
		
		// populate sub2super2dist
		sub2super2dist= new Hashtable<String,Map<String,Integer>>();
		query=				
				"PREFIX : <"+CONLL_PREFIX+">\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT ?subl ?superl (COUNT(DISTINCT ?step) AS ?dist) \n"
				+ "WHERE {"
				+ "	?sub (owl:sameAs*/rdfs:subPropertyOf)+ ?step." // we really count properties only
				+ " ?step (owl:sameAs|rdfs:subPropertyOf)* ?super."
				+ " BIND(replace(str(?sub),'.*[/#]','') AS ?subl)\n"
				+ " BIND(replace(str(?super),'.*[/#]','') AS ?superl)\n"
				+ "} GROUP BY ?subl ?superl";
		ResultSet results = QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect();
		while(results.hasNext()) {
			QuerySolution sol = results.next();
			String sub = sol.getLiteral("?subl").getString();
			String sup = sol.getLiteral("?superl").getString();
			int dist = sol.getLiteral("?dist").getInt();
			if(sub2super2dist.get(sub)==null)
				sub2super2dist.put(sub, new TreeMap<String,Integer>()); // TreeMap => disambiguation by lexicographic order
			if(sub2super2dist.get(sub).get(sup)==null)
				sub2super2dist.get(sub).put(sup, dist);
		}
		
		// special symbols
		property2featureSrc = new Hashtable<String,String>();
		query = "PREFIX : <"+CONLL_PREFIX+">\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT DISTINCT ?propertyL ?symbol\n"
				+ "WHERE { " // reserved symbols
				+ " { :"+src+" a :Dialect; ?property ?symbol. \n"
				+ "   ?property rdfs:subPropertyOf+ :hasReservedSymbol .\n"
				+ "	  BIND(replace(str(?property),'.*[#/]','') AS ?propertyL)\n"
				+ " } UNION {\n" // encoding (per column)
				+ "	  ?property :hasMapping [ :dialect :"+src+"; :column ?col; :encoding ?encoding ].\n"
				+ "   BIND(str(?col) AS ?propertyL)\n"
				+ "   BIND(replace(str(?encoding),'.*[#/]','') AS ?symbol)\n"
				+ " } }";
		results = QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect();
		
		while(results.hasNext()) {
			QuerySolution sol = results.next();
			String property = ""+sol.getLiteral("?propertyL");
			String symbol = sol.getLiteral("?symbol").getString();
			if(property2featureSrc.get(property)!=null) {
				System.err.println("warning: multiple symbols defined for conll:"+property+" of "+src+": "+property2featureSrc.get(property)+", "+symbol+" (keeping the first)");
			} else
				property2featureSrc.put(property, symbol);
		}

		property2featureTgt = new Hashtable<String,String>();
		query = "PREFIX : <"+CONLL_PREFIX+">\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT DISTINCT ?propertyL ?symbol\n"
				+ "WHERE { " // reserved symbols
				+ " { ?property rdfs:subPropertyOf+ :hasReservedSymbol .\n"
				+ "	  BIND(replace(str(?property),'.*[#/]','') AS ?propertyL)\n"
				+ "   :"+tgt+" ?property ?symbol \n"
				+ " } UNION {\n" // encoding (per column)
				+ "	  ?property :hasMapping [ :dialect :"+tgt+"; :column ?col; :encoding ?encoding ].\n"
				+ "   BIND(str(?col) AS ?propertyL)\n"
				+ "   BIND(replace(str(?encoding),'.*[#/]','') AS ?symbol)\n"
				+ " }}";
		results = QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect();
		while(results.hasNext()) {
			QuerySolution sol = results.next();
			String property = ""+sol.getLiteral("?propertyL");
			String symbol = sol.getLiteral("?symbol").getString();
			if(property2featureTgt.get(property)!=null) {
				System.err.println("warning: multiple values defined for conll:"+property+" of "+tgt+": "+property2featureTgt.get(property)+", "+symbol+" (keeping the first)");
			} else
				property2featureTgt.put(property, symbol);
		}
	}
	
	/** create args for -conll parameter */
	public String[] formatterArgs() {
		String result = "-conll";
		for(String prop : tgtCols) 
			result=result+"\t"+prop;
		
		return result.split("\t");
	}
	
	public String[] extractorArgs() {
		return extractorArgs(DEFAULT_BASEURI);
	}
	
	/** dialect should be src or tgt labels (local names) */
	protected List<String> getCols(String dialect) {
		String query = 
				"PREFIX conll: <"+CONLL_PREFIX+">\n"
				+ "SELECT DISTINCT ?label ?col\n"
				+ "WHERE { "
				+ "	?prop conll:hasMapping [ conll:dialect conll:"+dialect+"; conll:column ?col ] . "
				+ " BIND(replace(str(?prop),'.*[#/]','') AS ?label)"
				+ "} ORDER BY ?col ?label";
		ResultSet results = QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect();
		Vector<String> col2prop = new Vector<String>();
		while(results.hasNext()) {
			QuerySolution sol = results.next();
			int col = sol.getLiteral("?col").getInt()-1;
			String label = sol.getLiteral("?label").getString();
			while(col2prop.size()<=col) col2prop.add(null);
			if(col2prop.get(col)!=null && !col2prop.get(col).equals(label)) {
				System.err.println("warning: column "+col+" maps to multiple properties: "+col2prop.get(col)+" and "+label+", keeping the first");
			} else
				col2prop.set(col, label);
		}
		for(int col = 0; col<col2prop.size(); col++)
			if(col2prop.get(col)==null) {
				System.err.println("warning: no mapping defined for column "+(col+1)+", using label \"IGNORE\"");
				col2prop.set(col, "IGNORE");
			}
		return col2prop;
	}
	
	public String[] extractorArgs(String baseuri) {
		String result = baseuri;
		for(String prop : srcCols) {
			result=result+"\t"+prop;
		}

		return result.split("\t");
	}
	
	private static String writeArray(String[] array) {
		String result = "";
		for(String s : array)
			result=result+" "+s;
		return result.trim();
	}

	private static void printHelp(String baseuri) {
		System.err.println("synopsis: Transformer [-silent] [-help] [-version VERSION] SRC TGT OWL [BASEURI]\n"+
				"\t-silent  suppress this message\n"+
				"\t-help    show this message and quit\n"+
				"\t-version specify CoNLL-RDF version (1 or 2). Defaults to 1\n"+
				"\tSRC      source format\n"+
				"\tTGT      target format\n"+
				"\tOWL      CoNLL-RDF ontology (or a replacement that defines one or more conll:Dialect objects\n"+
				"\tBASEURI  base URI for the data being processed, defaults to "+baseuri+"\n"+
				"generates CoNLL-RDF calls for reading and writing different dialects\n"+
				"TODO: reads one-token-per-line TSV (\"CoNLL\") data from stdin, transforms to output format according to the conll:Dialect mapping defined in OWL\n"
				+"NOTE that we generate Bash scripts at the moment, but that escaping (of SPARQL scripts) and paths (classpath, package) need to be adjusted in order to execute it\n"
				+"CoNLL-RDF JSON configs soon to come.");
	}
	
	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();

		options.addOption("silent", false, "suppress this message");
		options.addOption(Option.builder("version")
										 .hasArg()
										 .required(false)
										 .desc("version of CoNLL-RDF={1,2}. Default: 1")
										 .build());
		options.addOption("help", false, "show this help message and quit");

		String baseuri = Transformer.DEFAULT_BASEURI;
		String version = Transformer.DEFAULT_VERSION;
		String conllPrefix = Transformer.CONLL_PREFIX_OLD;

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		}
		catch (ParseException ex) {
			printHelp(baseuri);
			System.exit(1);
		}
		String[] positionalArgs = cmd.getArgs();

		if(cmd.hasOption("help") || positionalArgs.length <2  ||  !cmd.hasOption("silent")) {
			printHelp(baseuri);
			if (positionalArgs.length < 2)
				System.exit(1);
		}

		if (cmd.hasOption("version"))
			version = cmd.getOptionValue("version");

		if (version.equals("2"))
			conllPrefix = Transformer.CONLL_PREFIX;

		if (positionalArgs.length > 3)
			baseuri = positionalArgs[2];

		Transformer me = new Transformer(positionalArgs[0], positionalArgs[1], positionalArgs[2], conllPrefix);

		System.err.println("building bash script");
		System.err.println("\n1. configure preprocessing");		
		String[] preprocArgs = me.preprocessorArgs();
		
		System.err.println("\n2. configure extraction");
		String[] extractorArgs = me.extractorArgs(baseuri);
		
		System.err.println("\n3. configure update");
		String[] updateArgs = me.updateArgs();
		
		System.err.println("\n4. configure formatter");
		String[] formatterArgs = me.formatterArgs();
		
		System.err.println("\n5. configure postprocessing");
		String[] postprocArgs = me.postprocessorArgs();
		
		System.err.println("\n6. writing script");
		if(preprocArgs!=null) // optional
			System.out.println("java -classpath bin org.acoli.conll.transform.Preprocessor "+writeArray(preprocArgs) +" | \\");
		// obligatory
		System.out.println("./run.sh CoNLLStreamExtractor "+writeArray(extractorArgs)+ " | \\");
		if(updateArgs!=null) // optional
			System.out.println("./run.sh CoNLLRDFUpdater "+writeArray(updateArgs) + " | \\");
		// obligatory
		System.out.print("./run.sh CoNLLRDFFormatter "+writeArray(formatterArgs));
		if(postprocArgs!=null) // optional
			System.out.print(" | \\\njava -classpath bin org.acoli.conll.transform.Postprocessor "+writeArray(postprocArgs));
		System.out.println();
	}

	/** arguments for Preprocessor */
	public String[] preprocessorArgs() {
		String result = "";
		
		String block = property2featureSrc.get("blockSeparator");
		String col = property2featureSrc.get("colSeparator");
		String row = property2featureSrc.get("rowSeparator");
		String comment = property2featureSrc.get("commentMarker");
		String empty = property2featureSrc.get("emptyAnnotationMarker");
		
		if(block!=null && !block.equals(StringTransformer.BLOCK_SEPARATOR)) 
			result=result+"-block\t\""+block+"\"\t";
		if(col!=null && !col.equals(StringTransformer.COL_SEPARATOR)) 
			result=result+"-col\t\""+col+"\"\t";
		if(row!=null && !row.equals(StringTransformer.ROW_SEPARATOR)) 
			result=result+"-row\t\""+row+"\"\t";
		if(comment!=null && !comment.equals(StringTransformer.COMMENT_MARKER)) 
			result=result+"-comment\t\""+comment+"\"\t";
		if(empty!=null && !empty.equals(StringTransformer.EMPTY_ANNOTATION)) 
			result=result+"-empty\t\""+empty+"\"\t";

		
		for(String prop : property2featureSrc.keySet()) {
			if(prop.matches("^[0-9]+$")) {
				col = prop;
				String encoding = property2featureSrc.get(col);
				if(encoding.equals("plainEncoding")) { 			// keep it (default)
				} else if(encoding.equals("bracketEncoding")) { // keep it (default for parsed structures)
				} else if(encoding.equals("iobesEncoding")) { // transform to bracketEncoding
					result=result+
							"-replacement\t"+col+"\t"+
								"\"^S-(.*)$\"" + "\t" + "\"($1 *)\""+"\t"+
								"\"^I-(.*)$\"" + "\t" + "\"*\""		+"\t"+
								"\"^B-(.*)$\"" + "\t" + "\"( *\""	+"\t"+
								"\"^E-(.*)$\"" + "\t" + "\"* )\""	+"\t"+
								"\"^O$\"" 	   + "\t" + "\"*\""		+"\t";
				} else
					System.err.println("warning: unsupported input encoding \""+encoding+"\"");
			}
		}

		if(result.trim().length()==0) return null;
		return result.trim().split("\t");
	}

	public String[] postprocessorArgs() {
		String result = "";
		
		String block = property2featureTgt.get("blockSeparator");
		String col = property2featureTgt.get("colSeparator");
		String row = property2featureTgt.get("rowSeparator");
		String comment = property2featureTgt.get("commentMarker");
		String empty = property2featureTgt.get("emptyAnnotationMarker");
		
		if(block!=null && !block.equals(StringTransformer.BLOCK_SEPARATOR)) 
			result=result+"-block\t\""+block+"\"\t";
		if(col!=null && !col.equals(StringTransformer.COL_SEPARATOR)) 
			result=result+"-col\t\""+col+"\"\t";
		if(row!=null && !row.equals(StringTransformer.ROW_SEPARATOR)) 
			result=result+"-row\t\""+row+"\"\t";
		if(comment!=null && !comment.equals(StringTransformer.COMMENT_MARKER)) 
			result=result+"-comment\t\""+comment+"\"\t";
		if(empty!=null && !empty.equals(StringTransformer.EMPTY_ANNOTATION)) 
			result=result+"-empty\t\""+empty+"\"\t";
		
		for(String prop : property2featureTgt.keySet()) {
			if(prop.matches("^[0-9]+$")) {
				col = prop;
				String encoding = property2featureTgt.get(col);
				if(encoding.equals("plainEncoding")) { 			// keep it (default)
				} else if(encoding.equals("bracketEncoding")) { // keep it (default for parsed structures)
				} else
					System.err.println("warning: unsupported output encoding \""+encoding+"\"");
			}
		}

		if(result.trim().length()==0) return null;
		return result.trim().split("\t");
	}
	
	/** create a SPARQL script to transform source into target format; return null for no changes*/
	public String[] updateArgs() {
		Hashtable<String,String> tgt2src = new Hashtable<String,String>(); 
		//  we try to fill the target, so there must be one source property per target property
		// but not necessarily vice versa
		for(String tgt : tgtCols) {
			if(srcCols.contains(tgt))
				tgt2src.put(tgt,tgt);
			if(tgt2src.get(tgt)==null) {
				Integer dist = null;
				// map src to superproperty in tgt
				for(String src : srcCols)
					if(sub2super2dist.get(src)!=null && sub2super2dist.get(src).get(tgt)!=null) {
						if(dist==null || sub2super2dist.get(src).get(tgt)<dist)
							tgt2src.put(tgt, src);
						else if(sub2super2dist.get(src).get(tgt)==dist)
							System.err.println("warning: ambiguous mapping "+src+" or "+tgt2src.get(tgt)+" => "+tgt+", using "+tgt2src.get(tgt));
					}
				if(tgt2src.get(tgt)!=null) 
					System.err.println("generalization: "+tgt2src.get(tgt)+" => "+tgt);
			}
			if(tgt2src.get(tgt)==null) {
				Integer dist = null;
				// map src to subproperty in tgt
				if(sub2super2dist.get(tgt)!=null)
					for(String src : sub2super2dist.get(tgt).keySet())
						if(srcCols.contains(src)) {
							if(dist==null || sub2super2dist.get(tgt).get(src)<dist)
								tgt2src.put(tgt, src);
							else if(sub2super2dist.get(tgt).get(src)==dist) {
								System.err.println("warning: ambiguous mapping "+src+" or "+tgt2src.get(tgt)+" => "+tgt+", using "+tgt2src.get(tgt));
							}
						}
				if(tgt2src.get(tgt)!=null)
					System.err.println("approximative specialization: "+tgt2src.get(tgt)+" => "+tgt);
			}
		}

		// validation: report missing properties
		for(String tgt : tgtCols)
			if(tgt2src.get(tgt)==null)
				System.err.println("warning: no mapping for target format property "+tgt);
		for(String src : srcCols) {
			String tgt = null;
			for(String t : tgt2src.keySet()) 
				if(tgt2src.get(t).equals(src))
					tgt=t;
			if(tgt==null)
				System.err.println("warning: no mapping for source format property "+src);
		}
			
		String updates = "";
		for(String tgt : tgt2src.keySet())
			if(tgt2src.get(tgt)!=null && !tgt2src.get(tgt).equals(tgt)) {
				String src = tgt2src.get(tgt);
				updates=updates
					+ "INSERT { ?a conll:"+tgt+" ?b } "
					+ "WHERE { ?a conll:"+src+" ?b};\n";
			}
		if(updates.length()==0) return null;
		return new String[] {"-updates", "PREFIX conll: <"+conllPrefix+">\n"+updates };
	}
}
