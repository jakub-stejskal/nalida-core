package cz.cvut.fel.nalida.tokenization.stanford;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import cz.cvut.fel.nalida.Lexicon;
import cz.cvut.fel.nalida.schema.Attribute;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.syntax.stanford.SemanticAnnotator;
import cz.cvut.fel.nalida.tokenization.Attachment;
import cz.cvut.fel.nalida.tokenization.Token;
import cz.cvut.fel.nalida.tokenization.Tokenization;
import cz.cvut.fel.nalida.tokenization.Tokenizer;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class StanfordTokenizer implements Tokenizer<Annotation> {

	private static final boolean VALIDATE_BY_TOKENS = true;

	public StanfordTokenizer(Lexicon lexicon) {

	}

	@Override
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
		Integer root = tokenIndices.get(Integer.valueOf(dependencyTree.getFirstRoot().index()));
		for (List<Token> tokenList : tokenLists) {
			tokenizations.add(new Tokenization(tokenList, root, attachmentsTemplate));
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

		if (validTokenizations.size() == 1 && validTokenizations.iterator().next().getElements().isEmpty()) {
			return Collections.emptySet();
		}

		return validTokenizations;
	}

	private boolean isValidTokenization(List<String> words, Tokenization tokenization) {
		ImmutableSet<Token> uniqueTokens = ImmutableSet.copyOf(tokenization.getTokens());
		for (String word : words) {
			int occurences = 0;
			for (Token token : uniqueTokens) {
				if (token.getWords().contains(word)) {
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
		if (VALIDATE_BY_TOKENS) {
			return hasValidAttachmentsByTokens(tokenization);
		} else {
			return hasValidAttachmentsByAttachments(tokenization);
		}
	}

	private boolean hasValidAttachmentsByTokens(Tokenization tokenization) {
		for (Token token : tokenization.getTokens()) {
			boolean valid = false;
			if (token.isType(ElementType.ENTITY)) {
				for (Token attached : tokenization.getAttached(token)) {
					if (token.equals(tokenization.getRoot())
							|| attached.isType(ElementType.WH_WORD)
							|| (attached.isType(ElementType.VALUE, ElementType.ATTRIBUTE, ElementType.SUBRESOURCE) && attached
									.getEntityElement().equals(token.getElement()))) {
						valid = true;
						break;
					}
				}
			} else if (token.isType(ElementType.ATTRIBUTE, ElementType.SUBRESOURCE)) {
				Attribute attribute = (Attribute) token.getElement();
				for (Token attached : tokenization.getAttached(token)) {
					if (token.equals(tokenization.getRoot())
							|| attached.isType(ElementType.WH_WORD)
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

	private boolean hasValidAttachmentsByAttachments(Tokenization tokenization) {
		for (Attachment<Token> attachment : tokenization.getAttachments()) {
			Token source = attachment.getSource();
			Token target = attachment.getTarget();
			if (!source.isType(ElementType.WH_WORD) && !target.isType(ElementType.WH_WORD)
					&& !source.getEntityElement().equals(target.getEntityElement()) && !isAttributeOfType(source, target)
					&& !isAttributeOfType(target, source)) {
				System.out.println("TOKENIZATION : " + tokenization);
				System.out.println("FAILED ATTACH: " + attachment);
				return false;
			}
		}
		return true;
	}

	private boolean isAttributeOfType(Token attrToken, Token type) {

		if (attrToken.isType(ElementType.ATTRIBUTE, ElementType.SUBRESOURCE)) {
			Attribute attribute = ((Attribute) attrToken.getElement());
			if (!attribute.isPrimitiveType()) {
				return attribute.getTypeEntity().equals(type.getEntityElement());
			}
		}
		return false;
	}
}
