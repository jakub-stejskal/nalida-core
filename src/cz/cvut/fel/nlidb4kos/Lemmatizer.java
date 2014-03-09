package cz.cvut.fel.nlidb4kos;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Lemmatizer {

	private final StanfordCoreNLP pipeline;

	public Lemmatizer() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");

		pipeline = new StanfordCoreNLP(props);
	}

	public List<String> getLemmas(String text) {
		Annotation document = new Annotation(text);

		pipeline.annotate(document);

		List<String> lemmas = new ArrayList<>();
		for (CoreLabel token : document.get(TokensAnnotation.class)) {
			lemmas.add(token.lemma().toLowerCase());
		}
		return lemmas;

	}
}
