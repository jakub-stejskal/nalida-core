package cz.cvut.fel.nalida;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import cz.cvut.fel.nalida.interpretation.Token;
import cz.cvut.fel.nalida.schema.Attribute;
import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.syntax.Lemmatizer;

public class Lexicon {

	Map<String, Set<Token>> lexicon;
	Lemmatizer lemmatizer;

	public Lexicon(Lemmatizer lemmatizer, Schema schema, String valuesPath) throws IOException {
		this.lexicon = new HashMap<>();
		this.lemmatizer = lemmatizer;
		loadLexicon(schema, valuesPath);
	}

	private void loadLexicon(Schema schema, String schemaPath) throws IOException {
		for (Entity entity : schema.getEntities()) {
			loadEntity(entity);
			for (Attribute attribute : entity.getAttributes()) {
				loadAttribute(entity, attribute);
				loadValues(schemaPath, entity, attribute);
			}
			for (Attribute attribute : entity.getSubresources()) {
				loadAttribute(entity, attribute);
			}
		}
		loadCommons(schemaPath);
	}

	private void loadEntity(Entity entity) {
		for (String token : entity.getTokens()) {
			putToTokens(Lists.newArrayList(token), entity);
		}
	}

	private void loadAttribute(Entity entity, Attribute attribute) {
		for (String token : attribute.getTokens()) {
			putToTokens(this.lemmatizer.getLemmas(token), attribute);
		}
	}

	private void loadValues(String schemaPath, Entity entity, Attribute attribute) throws IOException {
		if (attribute.isPrimitiveType()) {
			String valueFilename = format("%s%s.%s.values", schemaPath, entity.getName(), attribute.getName().replace('/', '-'));

			BufferedReader br = new BufferedReader(new FileReader(valueFilename));
			String line;
			while ((line = br.readLine()) != null) {
				putToTokens(this.lemmatizer.getLemmas(line), attribute.getValueElement());

			}
			br.close();
		}
	}

	private void loadCommons(String schemaPath) throws IOException {
		String valueFilename = format("%s%s.%s.values", schemaPath, "common", "wh");

		BufferedReader br = new BufferedReader(new FileReader(valueFilename));
		String line;
		while ((line = br.readLine()) != null) {
			putToTokens(this.lemmatizer.getLemmas(line), Element.WH_ELEMENT);
		}
		br.close();
	}

	private void putToTokens(List<String> lemmas, Element element) {
		for (String word : lemmas) {
			Set<Token> tokenSet = this.lexicon.get(word);
			if (tokenSet == null) {
				tokenSet = new HashSet<>();
				this.lexicon.put(word, tokenSet);
			}
			tokenSet.add(new Token(lemmas, element));
		}
	}

	public Set<Token> getTokens(String lemma) {
		return this.lexicon.get(lemma.toLowerCase());
	}

	@Override
	public String toString() {
		return "lexicon: \n" + this.lexicon;
	}
}
