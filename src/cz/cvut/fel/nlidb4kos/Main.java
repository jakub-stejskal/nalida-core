package cz.cvut.fel.nlidb4kos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import stanford.SemanticAnnotator;
import cz.cvut.fel.nlidb4kos.db.Lexicon;
import cz.cvut.fel.nlidb4kos.db.Lexicon.ElementType;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

public class Main {

	private static final String QUERIES_FILE = "data/queries2.txt";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		properties.load(Main.class.getClassLoader().getResourceAsStream("nlpcore.properties"));

		Lexicon lexicon = new Lexicon("data/schema/");

		System.out.println(lexicon);

		SyntacticAnalysis syntacticAnalysis = new SyntacticAnalysis(properties, lexicon);
		SemanticAnalysis semanticAnalysis = new SemanticAnalysis(lexicon);

		File file = new File(QUERIES_FILE);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;

		while ((line = br.readLine()) != null) {
			System.out.println("\n XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");
			Annotation annotatedLine = syntacticAnalysis.process(line);
			printSyntacticInfo(annotatedLine);
			Set<List<Pair<ElementType, String>>> tokenizations = semanticAnalysis.getTokenizations(annotatedLine);
			printSemanticInfo(tokenizations);
		}
		br.close();
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

			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			System.out.println("tree:  \n" + tree);
			System.out.println();

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("dependencies: \n" + dependencies);
			for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
				System.out.println(edge.getGovernor() + " - " + edge.getRelation() + " - " + edge.getDependent());
			}
			System.out.println();
		}

		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		System.out.println("graph: \n" + graph);
		System.out.println();

		// SUTime annotations
		List<CoreMap> timexAnnsAll = document.get(TimeAnnotations.TimexAnnotations.class);
		for (CoreMap cm : timexAnnsAll) {
			List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
			System.out.println(cm + " [from char offset " + tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) + " to "
					+ tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' + " --> "
					+ cm.get(TimeExpression.Annotation.class).getTemporal());
		}

	}

	public static void printSemanticInfo(Set<List<Pair<ElementType, String>>> tokenizations) {
		for (List<Pair<ElementType, String>> tokenization : tokenizations) {
			System.out.println(tokenization);
		}
	}
}
