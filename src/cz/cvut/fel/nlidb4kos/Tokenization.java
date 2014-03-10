package cz.cvut.fel.nlidb4kos;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import cz.cvut.fel.nlidb4kos.db.Lexicon.ElementType;
import edu.stanford.nlp.util.Pair;

public class Tokenization {
	final protected Set<Pair<Token, Token>> attachments;
	final protected List<Token> tokens;

	public Tokenization(List<Token> tokenList, Set<Pair<Integer, Integer>> attachmentsTemplate) {
		this.tokens = tokenList;
		this.attachments = createAttachments(attachmentsTemplate);
	}

	private Set<Pair<Token, Token>> createAttachments(Set<Pair<Integer, Integer>> attachmentsTemplate) {
		Set<Pair<Token, Token>> atts = new HashSet<>();

		for (Pair<Integer, Integer> pair : attachmentsTemplate) {
			atts.add(Pair.makePair(this.tokens.get(pair.first.intValue()), this.tokens.get(pair.second.intValue())));
		}
		return atts;
	}

	public Set<Pair<Token, Token>> getAttachments() {
		return this.attachments;
	}

	public List<Token> getTokens() {
		return this.tokens;
	}

	public Collection<Token> getTokens(final ElementType type) {
		return Collections2.filter(this.tokens, new Predicate<Token>() {
			@Override
			public boolean apply(Token token) {
				return token.getEntityType() == type;
			}
		});
	}

	public Set<Token> getAttached(Token token) {
		Set<Token> attached = new HashSet<>();
		for (Pair<Token, Token> edge : this.attachments) {
			if (edge.first.equals(token)) {
				attached.add(edge.second);
			} else if (edge.second.equals(token)) {
				attached.add(edge.first);
			}
		}
		return attached;
	}

	@Override
	public String toString() {
		return this.attachments.toString() + "\n" + this.tokens.toString();
	}
}
