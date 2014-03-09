package cz.cvut.fel.nlidb4kos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import stanford.SemanticAnnotator;

import com.google.common.collect.Sets;

import cz.cvut.fel.nlidb4kos.db.Lexicon;
import cz.cvut.fel.nlidb4kos.db.Lexicon.SemSet;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

public class SemanticAnalysis {

	public SemanticAnalysis(Lexicon lexicon) {

	}

	public Set<List<Pair<Lexicon.ElementType, String>>> getTokenizations(Annotation annotatedLine) {

		List<CoreMap> sentences = annotatedLine.get(SentencesAnnotation.class);
		List<SemSet> semSets = new ArrayList<>();

		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			List<IndexedWord> dependencyTreeVertices = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class).vertexListSorted();
			for (IndexedWord vertex : dependencyTreeVertices) {
				CoreLabel token = tokens.get(vertex.index() - 1);
				SemSet semSet = token.get(SemanticAnnotator.class);
				if (semSet != null) {
					semSets.add(semSet);
				}
			}
		}
		return Sets.cartesianProduct(semSets);
	}

	public void getDependencies(Annotation annotatedLine) {
		SemanticGraph dependencies = annotatedLine.get(CollapsedCCProcessedDependenciesAnnotation.class);
		System.out.println("dependencies: \n" + dependencies);
	}

}
