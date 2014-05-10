package cz.cvut.fel.nalida;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cz.cvut.fel.nalida.interpretation.Interpretation;
import cz.cvut.fel.nalida.interpretation.Interpreter;
import cz.cvut.fel.nalida.interpretation.stanford.StanfordInterpreter;
import cz.cvut.fel.nalida.query.QueryGenerator;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.query.rest.RestQueryGenerator;
import cz.cvut.fel.nalida.query.sql.SqlQueryGenerator;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.syntax.stanford.SemanticAnnotator;
import cz.cvut.fel.nalida.syntax.stanford.StanfordLemmatizer;
import cz.cvut.fel.nalida.syntax.stanford.SyntacticAnalysis;
import cz.cvut.fel.nalida.util.GraphDisplay;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class Main {

	private static final String QUERIES_FILE = "data/dev.txt";
	private static final String DATA_PATH = "data/schema3/";
	private static final String SCHEMA_FILENAME = "schema.desc";
	public static boolean PRINT_FILTERING = false;
	static Scanner in = new Scanner(System.in);

	private static SyntacticAnalysis syntacticAnalysis;
	private static Interpreter<Annotation> interpreter;
	private static QueryGenerator queryGenerator;
	private static Lexicon lexicon;
	private static CommandLine cli;
	private static Schema schema;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		parseArguments(args);
		initializeComponents();

		if (cli.hasOption("verbose")) {
			System.out.println(schema);
			System.out.println(lexicon);
		}

		if (cli.hasOption("query")) {
			processSentence(cli.getOptionValue("query"));
		} else if (cli.hasOption("file") || cli.hasOption("example")) {
			File file = null;
			if (cli.hasOption("file")) {
				file = new File(cli.getOptionValue("file"));
			} else {
				file = new File(QUERIES_FILE);
			}
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				try {
					processSentence(line);
				} catch (Error e) {
					e.printStackTrace(System.out);
				}
			}
			br.close();
			in.close();
		}

		System.exit(0);
	}

	private static void parseArguments(String[] args) {
		Options options = new Options();

		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(new Option("e", "example", false, "show example queries and their processing"));
		optionGroup.addOption(new Option("q", "query", true, "process and answer the query from argument"));
		optionGroup.addOption(new Option("f", "file", true, "process and answer each query from file"));
		optionGroup.setRequired(true);
		options.addOptionGroup(optionGroup);
		options.addOption("d", "dry-run", false, "translate a query without executing it");
		options.addOption("v", "verbose", false, "prints out detailed information about what is being done");
		options.addOption("s", "sql", false, "translate a query into SQL instead of REST request");
		options.addOption("g", "graph", false, "visualize entity-relationship graph");

		try {
			cli = new DefaultParser().parse(options, args, true);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println();
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("-example | -file <queriesListFileName> | -query <query>", options);
			System.exit(1);
		}
	}

	private static void processSentence(String line) throws IOException {
		if (line.isEmpty() || line.startsWith("#")) {
			return;
		}

		Annotation annotatedLine = syntacticAnalysis.process(line);
		if (cli.hasOption("verbose")) {
			System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");
			System.out.println("INPUT:" + line);
		}
		double startTime = System.currentTimeMillis();
		Set<Interpretation> interpretations = interpreter.interpret(annotatedLine);
		double interpretationTime = System.currentTimeMillis();
		Interpretation interpretation = pickInterpretation(interpretations);
		double pickTime = System.currentTimeMillis();
		QueryPlan queryPlan = null;
		double queryGenTime = 0, endPlanTime = 0;
		if (interpretation == null) {
			System.out.println("Failed to translate query:" + line);
			if (cli.hasOption("verbose")) {
				printSyntacticInfo(annotatedLine);
			}
		} else {
			if (cli.hasOption("verbose")) {
				System.out.println("Selected interpretation: ");
				System.out.println(interpretation);
			}
			queryPlan = queryGenerator.generateQuery(interpretation);
			queryGenTime = System.currentTimeMillis();
			if (cli.hasOption("verbose") || cli.hasOption("dry-run")) {
				System.out.println("Generated query:");
				System.out.println(queryPlan);
				System.out.println();
			}
			if (!cli.hasOption("dry-run")) {
				try {
					System.out.println(queryPlan.execute());
				} catch (Exception e) {
					System.out.println("Failed to execute the query plan: " + e.getMessage());
				}
			}
			endPlanTime = System.currentTimeMillis();
		}

		if (cli.hasOption("verbose")) {

			System.out.println("INPUT:" + line);
			System.out.println("INT#    :" + interpretations.size());
			System.out.println("INT-SIZE:" + ((interpretation != null) ? interpretation.getEntityCount() : 0));
			System.out.println("QP-LEN  :" + (queryPlan != null ? Integer.valueOf(queryPlan.getLenght()) : "-"));
			System.out.println();
			System.out.println("TIME-INT:" + (interpretationTime - startTime));
			double qpgDuration = (queryGenTime > pickTime ? (queryGenTime - pickTime) : 0);
			System.out.println("TIME-QPG:" + qpgDuration);
			System.out.println("TIME-EXE:" + (endPlanTime - queryGenTime));
			System.out.println();
			System.out.println("TIME-CMP:" + ((interpretationTime - startTime) + qpgDuration));
			System.out.println("TIME-TOT:" + ((interpretationTime - startTime) + qpgDuration + (endPlanTime - queryGenTime)));
		}
	}

	private static void initializeComponents() throws IOException {
		InputStream input = new FileInputStream(new File(DATA_PATH + SCHEMA_FILENAME));
		schema = Schema.load(input);
		lexicon = new Lexicon(new StanfordLemmatizer(), schema, DATA_PATH);
		if (cli.hasOption("graph")) {
			GraphDisplay.displayGraph(schema.getGraph());
		}

		syntacticAnalysis = new SyntacticAnalysis(lexicon);
		interpreter = new StanfordInterpreter(lexicon);

		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));

		if (cli.hasOption("sql")) {
			queryGenerator = new SqlQueryGenerator(schema, props);
		} else {
			queryGenerator = new RestQueryGenerator(schema, props);
		}
	}

	private static Interpretation pickInterpretation(Set<Interpretation> interpretations) throws IOException {
		if (interpretations.size() == 1) {
			Interpretation interpretation = interpretations.iterator().next();
			if (interpretation.getElements().isEmpty()) {
				return null;
			} else {
				return interpretation;
			}
		} else if (interpretations.size() == 0) {
			return null;
		}

		List<Interpretation> tokList = new ArrayList<>(interpretations);
		Collections.sort(tokList, new Comparator<Interpretation>() {
			@Override
			public int compare(Interpretation o1, Interpretation o2) {
				return o1.getTokens().toString().compareTo(o2.getTokens().toString());
			}
		});

		System.out.println("Choose correct interpretation:");
		for (int i = 0; i < tokList.size(); i++) {
			System.out.println(i + ": " + tokList.get(i).getTokens() + "(" + i + ")");
		}

		int choice = in.nextInt();
		return tokList.get(choice);
	}

	public static void printSyntacticInfo(Annotation document) {
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			System.out.println(sentence.get(TextAnnotation.class));
			System.out.println();

			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				System.out.println(token.word() + " - " + token.lemma() + " - " + token.get(SemanticAnnotator.class));
			}
			System.out.println();

			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("dependencies: \n" + dependencies);
			for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
				System.out.println(edge.getGovernor() + " - " + edge.getRelation() + " - " + edge.getDependent());
			}
			System.out.println();
		}
	}

	public static void printSemanticInfo(Set<Interpretation> interpretations) {
		System.out.println("Interpretations: \n");
		for (Interpretation interpretation : interpretations) {
			System.out.println(interpretation);
			System.out.println();
		}
	}
}
