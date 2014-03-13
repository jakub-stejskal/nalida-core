package cz.cvut.fel.nalida;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import cz.cvut.fel.nalida.db.Lexicon;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class Main {

	private static final String QUERIES_FILE = "data/queries2.txt";
	static Scanner in = new Scanner(System.in);

	private static SyntacticAnalysis syntacticAnalysis;
	private static SemanticAnalysis semanticAnalysis;
	private static QueryGenerator queryGenerator;
	private static Lexicon lexicon;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		initializeModules();

		File file = new File(QUERIES_FILE);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;

		while ((line = br.readLine()) != null) {
			System.out.println("\n XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");
			System.out.println(line);

			Annotation annotatedLine = syntacticAnalysis.process(line);
			//			printSyntacticInfo(annotatedLine);
			Set<Tokenization> tokenizations = semanticAnalysis.getTokenizations(annotatedLine);
			//			printSemanticInfo(tokenizations);
			Tokenization tokenization = pickTokenization(tokenizations);

			System.out.println("Generated query:");
			System.out.println(queryGenerator.generateQuery(tokenization));
			System.out.println();
			System.out.println("Prettified query:");
			System.out.println(queryGenerator.prettyPrintQuery(tokenization));
		}
		br.close();
		in.close();
	}

	private static void initializeModules() throws IOException {
		Properties properties = new Properties();
		properties.load(Main.class.getClassLoader().getResourceAsStream("nlpcore.properties"));

		lexicon = new Lexicon("data/schema/");

		syntacticAnalysis = new SyntacticAnalysis(properties, lexicon);
		semanticAnalysis = new SemanticAnalysis(lexicon);

		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));

		queryGenerator = new QueryGenerator(null, new URL(props.getProperty("baseUrl")));

	}

	private static Tokenization pickTokenization(Set<Tokenization> tokenizations) throws IOException {
		if (tokenizations.size() == 1) {
			return tokenizations.iterator().next();
		}

		List<Tokenization> tokList = new ArrayList<>(tokenizations);
		System.out.println("Choose correct tokenization:");
		for (int i = 0; i < tokList.size(); i++) {
			System.out.println(i + ": " + tokList.get(i).getTokens());
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

			//			//					System.out.println(sentence.keySet());
			//			//					System.out.println(sentence.get(TokensAnnotation.class).get(0).keySet());
			//
			//			// traversing the words in the current sentence
			//			// a CoreLabel is a CoreMap with additional token-specific methods
			//			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			//
			//				//				System.out.println(token.toShortString("Text", "PartOfSpeech", "Lemma", "NamedEntityTag", "Utterance", "Speaker"));
			//				System.out.println(token.word() + " - " + token.lemma() + " - " + token.get(SemanticAnnotator.class));
			//			}
			//			System.out.println();
			//
			//			//			// this is the parse tree of the current sentence
			//			//			Tree tree = sentence.get(TreeAnnotation.class);
			//			//			System.out.println("tree:  \n" + tree);
			//			//			System.out.println();
			//
			//			// this is the Stanford dependency graph of the current sentence
			//			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			//			System.out.println("dependencies: \n" + dependencies);
			//			for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
			//				System.out.println(edge.getGovernor() + " - " + edge.getRelation() + " - " + edge.getDependent());
			//			}
			//			System.out.println();
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
