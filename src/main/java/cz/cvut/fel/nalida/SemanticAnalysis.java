package cz.cvut.fel.nalida;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import cz.cvut.fel.nalida.db.Attribute;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Lexicon;
import cz.cvut.fel.nalida.stanford.SemanticAnnotator;
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

		return validateTokenizations(annotatedLine, tokenizations);
	}

	private List<Set<Token>> createTokenSets(Annotation annotatedLine, Map<Integer, Integer> tokenIndices) {
		List<Set<Token>> tokenSets = new ArrayList<>();
		List<CoreMap> sentences = annotatedLine.get(SentencesAnnotation.class);
		CoreMap sentence = sentences.get(0);
		//		for (CoreMap sentence : sentences) {
		List<CoreLabel> stanfordTokens = sentence.get(TokensAnnotation.class);
		SemanticGraph dependencyTree = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		int position = 0;
		for (IndexedWord vertex : dependencyTree.vertexListSorted()) {
			int index = vertex.index();
			CoreLabel stanfordToken = stanfordTokens.get(index - 1);
			Set<Token> tokenSet = stanfordToken.get(SemanticAnnotator.class);
			if (tokenSet != null) {
				tokenIndices.put(Integer.valueOf(index), Integer.valueOf(position++));
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

	private Set<Tokenization> validateTokenizations(Annotation annotatedLine, Set<Tokenization> tokenizations) {
		CoreMap sentence = annotatedLine.get(SentencesAnnotation.class).get(0);
		SemanticGraph dependencyTree = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<CoreLabel> stanfordTokens = sentence.get(TokensAnnotation.class);
		List<String> words = new ArrayList<>();
		for (IndexedWord vertex : dependencyTree.vertexListSorted()) {
			int index = vertex.index();
			CoreLabel stanfordToken = stanfordTokens.get(index - 1);
			if (stanfordToken.get(SemanticAnnotator.class) != null) {
				words.add(vertex.lemma().toLowerCase());
			}
		}

		Set<Tokenization> validTokenizations = new HashSet<>();
		for (Tokenization tokenization : tokenizations) {
			if (isValidTokenization(words, tokenization) && hasValidAttachments(tokenization)) {
				validTokenizations.add(tokenization);
			}
		}
		return validTokenizations;
	}

	private boolean isValidTokenization(List<String> words, Tokenization tokenization) {
		ImmutableSet<Token> uniqueTokens = ImmutableSet.copyOf(tokenization.getTokens());
		for (String word : words) {
			int occurences = 0;
			for (Token token : uniqueTokens) {
				if (token.words.contains(word)) {
					occurences++;
				}
			}
			if (occurences != 1) {
				return false;
			}
		}
		return true;
	}

	private boolean hasValidAttachments(Tokenization tokenization) {
		for (Token token : tokenization.getTokens()) {
			boolean valid = false;
			if (token.isType(ElementType.ENTITY)) {
				for (Token attached : tokenization.getAttached(token)) {
					if (attached.isType(ElementType.WH_WORD)
							|| (attached.isType(ElementType.VALUE, ElementType.ATTRIBUTE, ElementType.SUBRESOURCE) && attached
									.getEntityElement().equals(token.getElement()))) {
						valid = true;
						break;
					}
				}
			} else if (token.isType(ElementType.ATTRIBUTE, ElementType.SUBRESOURCE)) {
				Attribute attribute = (Attribute) token.getElement();
				for (Token attached : tokenization.getAttached(token)) {
					if (attached.isType(ElementType.WH_WORD)
							|| (attached.isType(ElementType.VALUE) && attached.getElement().equals(attribute.getValueElement()))
							|| (attached.isType(ElementType.ENTITY, ElementType.ATTRIBUTE, ElementType.SUBRESOURCE) && attached
									.getElement().equals(attribute.getTypeEntity()))) {
						valid = true;
						break;
					}
				}
			} else {
				valid = true;
			}
			if (!valid) {
				System.out.println("TOKENIZATION : " + tokenization);
				System.out.println("FAILED TOKEN: " + token + " --- " + tokenization.getAttached(token));
				return false;
			}
		}
		return true;
	}
	//	private boolean hasValidAttachments(Tokenization tokenization) {
	//		for (Attachment<Token> attachment : tokenization.getAttachments()) {
	//			if (!attachment.source.isType(ElementType.WH_WORD) && !attachment.target.isType(ElementType.WH_WORD)
	//					&& !attachment.source.getEntityElement().equals(attachment.target.getEntityElement())
	//					&& !isAttributeOfType(attachment.source, attachment.target) && !isAttributeOfType(attachment.target, attachment.source)) {
	//
	//				System.out.println("TOKENIZATION : " + tokenization);
	//				System.out.println("FAILED ATTACH: " + attachment);
	//				return false;
	//			}
	//		}
	//		return true;
	//	}
	//
	//	private boolean isAttributeOfType(Token attrToken, Token type) {
	//
	//		if (attrToken.isType(ElementType.ATTRIBUTE, ElementType.SUBRESOURCE)) {
	//			Attribute attribute = ((Attribute) attrToken.getElement());
	//			if (!attribute.isPrimitiveType()) {
	//				return attribute.getTypeEntity().equals(type.getEntityElement());
	//			}
	//		}
	//		return false;
	//	}
}
