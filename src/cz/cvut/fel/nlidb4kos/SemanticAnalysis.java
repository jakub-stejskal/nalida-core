package cz.cvut.fel.nlidb4kos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import cz.cvut.fel.nlidb4kos.db.Lexicon;
import cz.cvut.fel.nlidb4kos.db.Lexicon.SemSet;
import cz.cvut.fel.nlidb4kos.stanford.SemanticAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class SemanticAnalysis {

	public SemanticAnalysis(Lexicon lexicon) {

	}

	public Set<Tokenization> getTokenizations(Annotation annotatedLine) {
		Map<Integer, Integer> tokenIndices = new HashMap<Integer, Integer>();
		List<Set<Token>> tokenSets = createTokenSets(annotatedLine, tokenIndices);
		Set<List<Token>> tokenLists = Sets.cartesianProduct(tokenSets);
		Set<Tokenization> tokenizations = createTokenizations(annotatedLine, tokenLists, tokenIndices);

		return tokenizations;
	}

	private List<Set<Token>> createTokenSets(Annotation annotatedLine, Map<Integer, Integer> tokenIndices) {
		List<Set<Token>> tokenSets = new ArrayList<>();
		List<CoreMap> sentences = annotatedLine.get(SentencesAnnotation.class);
		CoreMap sentence = sentences.get(0);
		//		for (CoreMap sentence : sentences) {
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		SemanticGraph dependencyTree = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		int position = 0;
		for (IndexedWord vertex : dependencyTree.vertexListSorted()) {
			int index = vertex.index();
			CoreLabel token = tokens.get(index - 1);
			SemSet semSet = token.get(SemanticAnnotator.class);
			if (semSet != null) {
				tokenIndices.put(Integer.valueOf(index), Integer.valueOf(position++));
				Set<Token> tokenSet = new HashSet<>();
				for (Token t : semSet) {
					tokenSet.add(new Token(Sets.newHashSet(token.word()), t));
				}
				tokenSets.add(tokenSet);
			}
		}
		//		}
		return tokenSets;
	}

	private Set<Tokenization> createTokenizations(Annotation annotatedLine, Set<List<Token>> tokenLists, Map<Integer, Integer> tokenIndices) {
		CoreMap sentence = annotatedLine.get(SentencesAnnotation.class).get(0);
		SemanticGraph dependencyTree = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

		Set<Attachment<Integer>> attachmentsTemplate = new HashSet<>();
		for (SemanticGraphEdge edge : dependencyTree.getEdgeSet()) {
			Integer sourceIndex = tokenIndices.get(Integer.valueOf(edge.getSource().index()));
			Integer targetIndex = tokenIndices.get(Integer.valueOf(edge.getTarget().index()));

			if (sourceIndex != null && targetIndex != null) {
				Attachment<Integer> attachment = new Attachment<>(sourceIndex, targetIndex, edge.getRelation().getShortName());
				attachmentsTemplate.add(attachment);
			}
		}

		Set<Tokenization> tokenizations = new HashSet<>();
		for (List<Token> tokenList : tokenLists) {
			tokenizations.add(new Tokenization(tokenList, attachmentsTemplate));
		}

		return tokenizations;
	}

	public void getDependencies(Annotation annotatedLine) {
		SemanticGraph dependencies = annotatedLine.get(CollapsedCCProcessedDependenciesAnnotation.class);
		System.out.println("dependencies: \n" + dependencies);
	}

}
