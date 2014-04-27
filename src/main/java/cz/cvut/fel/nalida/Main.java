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
import cz.cvut.fel.nalida.query.QueryGenerator;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.query.rest.RestQueryGenerator;
import cz.cvut.fel.nalida.query.sql.SqlQueryGenerator;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.syntax.stanford.SemanticAnnotator;
import cz.cvut.fel.nalida.syntax.stanford.SyntacticAnalysis;
import cz.cvut.fel.nalida.tokenization.stanford.StanfordInterpreter;
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
	private static final String DATA_PATH = "data/schema2/";
	private static final String SCHEMA_FILENAME = "schema.desc";
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
		initializeModules();

		System.out.println(schema);
		System.out.println(lexicon);

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
				} catch (Exception e) {
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

		System.out.println("\n XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");
		System.out.println(line);

		Annotation annotatedLine = syntacticAnalysis.process(line);
		//				printSyntacticInfo(annotatedLine);
		Set<Interpretation> interpretations = interpreter.interpret(annotatedLine);
		//			printSemanticInfo(interpretations);
		Interpretation interpretation = pickInterpretation(interpretations);
		System.out.println("Selected interpretation: ");

		if (interpretation == null) {
			System.out.println("Failed to translate query.");
			return;
		}
		System.out.println(interpretation);

		QueryPlan queryPlan = queryGenerator.generateQuery(interpretation);
		System.out.println("Generated query:");
		System.out.println(queryPlan);
		System.out.println();

		if (!cli.hasOption("dry-run")) {
			try {
				System.out.println(queryPlan.execute());
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
	}

	private static void initializeModules() throws IOException {
		InputStream input = new FileInputStream(new File(DATA_PATH + SCHEMA_FILENAME));
		schema = Schema.load(input);
		lexicon = new Lexicon(schema, DATA_PATH);
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
		System.out.println("pickInterpretation: " + interpretations.size());
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
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {

			System.out.println(sentence.get(TextAnnotation.class));
			System.out.println();

			//					System.out.println(sentence.keySet());
			//					System.out.println(sentence.get(TokensAnnotation.class).get(0).keySet());

			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

				//				System.out.println(token.toShortString("Text", "PartOfSpeech", "Lemma", "NamedEntityTag", "Utterance", "Speaker"));
				System.out.println(token.word() + " - " + token.lemma() + " - " + token.get(SemanticAnnotator.class));
			}
			System.out.println();

			//			// this is the parse tree of the current sentence
			//			Tree tree = sentence.get(TreeAnnotation.class);
			//			System.out.println("tree:  \n" + tree);
			//			System.out.println();

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("dependencies: \n" + dependencies);
			for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
				System.out.println(edge.getGovernor() + " - " + edge.getRelation() + " - " + edge.getDependent());
			}
			System.out.println();
		}

		//		// This is the coreference link graph
		//		// Each chain stores a set of mentions that link to each other,
		//		// along with a method for getting the most representative mention
		//		// Both sentence and token offsets start at 1!
		//		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		//		System.out.println("graph: \n" + graph);
		//		System.out.println();
		//
		//		// SUTime annotations
		//		List<CoreMap> timexAnnsAll = document.get(TimeAnnotations.TimexAnnotations.class);
		//		for (CoreMap cm : timexAnnsAll) {
		//			List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
		//			System.out.println(cm + " [from char offset " + tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) + " to "
		//					+ tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' + " --> "
		//					+ cm.get(TimeExpression.Annotation.class).getTemporal());
		//		}

	}

	public static void printSemanticInfo(Set<Interpretation> interpretations) {
		System.out.println("Interpretations: \n");
		for (Interpretation interpretation : interpretations) {
			System.out.println(interpretation);
			System.out.println();
		}
	}
}
