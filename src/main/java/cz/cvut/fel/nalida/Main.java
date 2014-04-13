package cz.cvut.fel.nalida;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cz.cvut.fel.nalida.db.GraphDisplay;
import cz.cvut.fel.nalida.db.Lexicon;
import cz.cvut.fel.nalida.db.QueryPlan;
import cz.cvut.fel.nalida.stanford.SemanticAnnotator;
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
	private static final boolean VISUALIZE_SCHEMA = false;
	static Scanner in = new Scanner(System.in);

	private static SyntacticAnalysis syntacticAnalysis;
	private static SemanticAnalysis semanticAnalysis;
	private static QueryGenerator queryGenerator;
	private static Lexicon lexicon;
	private static CommandLine cli;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		parseArguments(args);
		initializeModules();

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

		try {
			cli = new BasicParser().parse(options, args, true);
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
		Set<Tokenization> tokenizations = semanticAnalysis.getTokenizations(annotatedLine);
		//			printSemanticInfo(tokenizations);
		Tokenization tokenization = pickTokenization(tokenizations);
		System.out.println("Selected tokenization: ");
		System.out.println(tokenization);
		if (tokenization == null) {
			return;
		}

		QueryPlan queryPlan = queryGenerator.generateQuery(tokenization);
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
		Properties properties = new Properties();
		properties.load(Main.class.getClassLoader().getResourceAsStream("nlpcore.properties"));

		lexicon = new Lexicon("data/schema/");
		if (VISUALIZE_SCHEMA) {
			GraphDisplay.displayGraph(lexicon.getSchema().getGraph());
		}

		syntacticAnalysis = new SyntacticAnalysis(properties, lexicon);
		semanticAnalysis = new SemanticAnalysis(lexicon);

		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));

		if (cli.hasOption("sql")) {
			queryGenerator = new SqlQueryGenerator(lexicon.getSchema(), props);
		} else {
			queryGenerator = new RestQueryGenerator(lexicon.getSchema(), props);
		}
	}

	private static Tokenization pickTokenization(Set<Tokenization> tokenizations) throws IOException {
		System.out.println("pickTokenization: " + tokenizations.size());
		if (tokenizations.size() == 1) {
			return tokenizations.iterator().next();
		} else if (tokenizations.size() == 0) {
			return null;
		}

		List<Tokenization> tokList = new ArrayList<>(tokenizations);
		Collections.sort(tokList, new Comparator<Tokenization>() {
			@Override
			public int compare(Tokenization o1, Tokenization o2) {
				return o1.getTokens().toString().compareTo(o2.getTokens().toString());
			}
		});

		System.out.println("Choose correct tokenization:");
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

	public static void printSemanticInfo(Set<Tokenization> tokenizations) {
		System.out.println("Tokenizations: \n");
		for (Tokenization tokenization : tokenizations) {
			System.out.println(tokenization);
			System.out.println();
		}
	}
}
