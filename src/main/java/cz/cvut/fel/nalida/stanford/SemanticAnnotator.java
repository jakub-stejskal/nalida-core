package cz.cvut.fel.nalida.stanford;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import cz.cvut.fel.nalida.Token;
import cz.cvut.fel.nalida.db.Lexicon;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;

public class SemanticAnnotator implements Annotator, CoreAnnotation<Set<Token>> {

	public static final String ANNOTATOR_CLASS = "semantic";
	public static final String SCHEMA_PATH = "semantic-path";
	public static final Requirement SEMSET_REQUIREMENT = new Requirement(ANNOTATOR_CLASS);

	private final Lexicon lexicon;

	@SuppressWarnings("rawtypes")
	private static Class<? extends Set> tokenSet = Collections.<Token> emptySet().getClass();

	public SemanticAnnotator(String annotatorClass, Properties props, Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.containsKey(TokensAnnotation.class)) {
			List<CoreLabel> tokens = annotation.get(TokensAnnotation.class);
			List<String> lemmas = new ArrayList<>();
			for (CoreLabel token : tokens) {
				lemmas.add(token.lemma().toLowerCase());
			}
			for (CoreLabel token : tokens) {
				Set<Token> compatibleTokens = new HashSet<>();
				Set<Token> semSet = this.lexicon.getTokens(token.lemma());
				if (semSet == null) {
					compatibleTokens = null;
				} else {
					for (Token t : semSet) {
						if (lemmas.containsAll(t.getWords())) {
							compatibleTokens.add(t);
						}
					}
				}
				token.set(SemanticAnnotator.class, compatibleTokens);
			}
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(SEMSET_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_SSPLIT_POS_LEMMA;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<Set<Token>> getType() {
		return (Class<Set<Token>>) tokenSet;
	}
}
