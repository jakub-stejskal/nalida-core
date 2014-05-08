package cz.cvut.fel.nalida.syntax.stanford;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import cz.cvut.fel.nalida.syntax.Lemmatizer;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class StanfordLemmatizer implements Lemmatizer {

	private final StanfordCoreNLP pipeline;
	private static final List<String> STOPWORDS = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in",
			"into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
			"to", "was", "will", "with");

	public StanfordLemmatizer() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");

		this.pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public List<String> getLemmas(String text) {
		Annotation document = new Annotation(text);

		this.pipeline.annotate(document);

		List<String> lemmas = new ArrayList<>();
		for (CoreLabel token : document.get(TokensAnnotation.class)) {
			String lemma = token.lemma().toLowerCase();
			if (!STOPWORDS.contains(lemma)) {
				lemmas.add(lemma);
			}
		}
		return lemmas;
	}
}
