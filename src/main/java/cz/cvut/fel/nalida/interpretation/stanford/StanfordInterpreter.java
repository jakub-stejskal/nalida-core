package cz.cvut.fel.nalida.interpretation.stanford;

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
import cz.cvut.fel.nalida.interpretation.Attachment;
import cz.cvut.fel.nalida.interpretation.Interpretation;
import cz.cvut.fel.nalida.interpretation.Interpreter;
import cz.cvut.fel.nalida.interpretation.Token;
import cz.cvut.fel.nalida.schema.Attribute;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.syntax.stanford.SemanticAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class StanfordInterpreter implements Interpreter<Annotation> {

	private static final boolean VALIDATE_BY_TOKENS = true;

	public StanfordInterpreter(Lexicon lexicon) {

	}

	@Override
	public Set<Interpretation> interpret(Annotation annotatedLine) {
		Map<Integer, Integer> tokenIndices = new HashMap<Integer, Integer>();
		List<Set<Token>> tokenSets = createTokenSets(annotatedLine, tokenIndices);
		Set<List<Token>> tokenLists = Sets.cartesianProduct(tokenSets);
		Set<Interpretation> interpretations = createInterpretations(annotatedLine, tokenLists, tokenIndices);

		return validateInterpretations(annotatedLine, interpretations);
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

	private Set<Interpretation> createInterpretations(Annotation annotatedLine, Set<List<Token>> tokenLists,
			Map<Integer, Integer> tokenIndices) {
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

		Set<Interpretation> interpretation = new HashSet<>();
		Integer root = tokenIndices.get(Integer.valueOf(dependencyTree.getFirstRoot().index()));
		for (List<Token> tokenList : tokenLists) {
			interpretation.add(new Interpretation(tokenList, root, attachmentsTemplate));
		}

		return interpretation;
	}

	private Set<Interpretation> validateInterpretations(Annotation annotatedLine, Set<Interpretation> interpretations) {
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

		Set<Interpretation> validInterpretations = new HashSet<>();
		for (Interpretation interpretation : interpretations) {
			if (isValidTokenization(words, interpretation) && hasValidAttachments(interpretation)) {
				validInterpretations.add(interpretation);
			}
		}

		if (validInterpretations.size() == 1 && validInterpretations.iterator().next().getElements().isEmpty()) {
			return Collections.emptySet();
		}

		return validInterpretations;
	}

	private boolean isValidTokenization(List<String> words, Interpretation interpretation) {
		ImmutableSet<Token> uniqueTokens = ImmutableSet.copyOf(interpretation.getTokens());
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

	private boolean hasValidAttachments(Interpretation interpretation) {
		if (VALIDATE_BY_TOKENS) {
			return hasValidAttachmentsByTokens(interpretation);
		} else {
			return hasValidAttachmentsByAttachments(interpretation);
		}
	}

	private boolean hasValidAttachmentsByTokens(Interpretation interpretation) {
		for (Token token : interpretation.getTokens()) {
			boolean valid = false;
			if (token.isType(ElementType.ENTITY)) {
				for (Token attached : interpretation.getAttached(token)) {
					if (token.equals(interpretation.getRoot())
							|| attached.isType(ElementType.WH_WORD)
							|| (attached.isType(ElementType.VALUE, ElementType.ATTRIBUTE, ElementType.SUBRESOURCE) && attached
									.getEntityElement().equals(token.getElement()))) {
						valid = true;
						break;
					}
				}
			} else if (token.isType(ElementType.ATTRIBUTE, ElementType.SUBRESOURCE)) {
				Attribute attribute = (Attribute) token.getElement();
				for (Token attached : interpretation.getAttached(token)) {
					if (token.equals(interpretation.getRoot())
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
				System.out.println("INTERPRETATION : " + interpretation);
				System.out.println("FAILED TOKEN: " + token + " --- " + interpretation.getAttached(token));
				return false;
			}
		}
		return true;
	}

	private boolean hasValidAttachmentsByAttachments(Interpretation interpretation) {
		for (Attachment<Token> attachment : interpretation.getAttachments()) {
			Token source = attachment.getSource();
			Token target = attachment.getTarget();
			if (!source.isType(ElementType.WH_WORD) && !target.isType(ElementType.WH_WORD)
					&& !source.getEntityElement().equals(target.getEntityElement()) && !isAttributeOfType(source, target)
					&& !isAttributeOfType(target, source)) {
				System.out.println("INTERPRETATION : " + interpretation);
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
