package org.acoli.conll.transform;

import java.util.*;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

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
	final static String BASEURI = "http://ufal.mff.cuni.cz/conll2009-st/task-description.html#";
	final static String DEFAULT_URI = "http://ignore.me/"; // used as default URI for converting data
	
	Transformer(String src, String tgt, String owl) {
		System.err.println("loading CoNLL-RDF ontology "+owl);
		model = ModelFactory.createDefaultModel() ;
		model.read(owl);
		
		String query =
				"PREFIX : <"+BASEURI+">\n"+
				"ASK { :"+src+" a :Dialect }";
		if(QueryExecutionFactory.create(QueryFactory.create(query), model).execAsk())
			this.src=src;
		else {
			this.src=null;
			System.err.println("warning: did not find source format \""+src+"\".");
		}
		
		query =
				"PREFIX : <"+BASEURI+">\n"+
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
					"PREFIX : <"+BASEURI+">\n"+
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
				"PREFIX : <"+BASEURI+">\n"
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
	}
	
	/** create args for -conll parameter */
	public String[] formatterArgs() {
		String result = "-conll";
		for(String prop : tgtCols) 
			result=result+"\t"+prop;
		
		return result.split("\t");
	}
	
	public String[] extractorArgs() {
		return extractorArgs(DEFAULT_URI);
	}
	
	/** dialect should be src or tgt labels (local names) */
	protected List<String> getCols(String dialect) {
		String query = 
				"PREFIX conll: <"+BASEURI+">\n"
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
	
	public static void main(String[] args) {
		String baseuri = Transformer.DEFAULT_URI;
		System.err.println("synopsis: Transformer SRC TGT OWL´[BASEURI]\n"+
				"\tSRC     source format\n"+
				"\tTGT     target format\n"+
				"\tOWL     CoNLL-RDF ontology (or a replacement that defines one or more conll:Dialect objects\n"+
				"\tBASEURI base URI for the data being processed, defaults to "+baseuri+"\n"+
				"generates CoNLL-RDF calls for reading and writing different dialects\n"+
				"TODO: reads one-token-per-line TSV (\"CoNLL\") data from stdin, transforms to output format according to the conll:Dialect mapping defined in OWL");
		Transformer me = new Transformer(args[0], args[1],args[2]);
		System.out.println("./run.sh CoNLLStreamExtractor "+writeArray(me.extractorArgs(baseuri)));
		String[] updateArgs = me.updateArgs();
		if(updateArgs!=null)
			System.out.println("./run.sh CoNLLRDFFormatter "+writeArray(updateArgs));
		System.out.println("./run.sh CoNLLRDFFormatter "+writeArray(me.formatterArgs()));
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
		return new String[] {"-updates", "PREFIX conll: <"+BASEURI+">\n"+updates };
	}
}
