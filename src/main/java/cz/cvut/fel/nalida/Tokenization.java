package cz.cvut.fel.nalida;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import cz.cvut.fel.nalida.db.Lexicon.ElementType;

public class Tokenization {
	final protected Set<Attachment<Token>> attachments;
	final protected List<Token> tokens;

	public Tokenization(List<Token> tokenList, Set<Attachment<Integer>> attachmentsTemplate) {
		this.tokens = tokenList;
		this.attachments = createAttachments(attachmentsTemplate);
	}

	private Set<Attachment<Token>> createAttachments(Set<Attachment<Integer>> attachmentsTemplate) {
		Set<Attachment<Token>> atts = new HashSet<>();
		for (Attachment<Integer> attachment : attachmentsTemplate) {
			Token source = this.tokens.get(attachment.source.intValue());
			Token target = this.tokens.get(attachment.target.intValue());
			atts.add(new Attachment<Token>(source, target, attachment.type));
		}
		return atts;
	}

	public Set<Attachment<Token>> getAttachments() {
		return this.attachments;
	}

	public List<Token> getTokens() {
		return this.tokens;
	}

	public Collection<Token> getTokens(final ElementType type) {
		return Collections2.filter(this.tokens, new Predicate<Token>() {
			@Override
			public boolean apply(Token token) {
				return token.getElementType() == type;
			}
		});
	}

	public Set<Token> getAttached(Token token) {
		Set<Token> attached = new HashSet<>();
		for (Attachment<Token> edge : this.attachments) {
			if (edge.source.equals(token)) {
				attached.add(edge.target);
			} else if (edge.target.equals(token)) {
				attached.add(edge.source);
			}
		}
		return attached;
	}

	@Override
	public String toString() {
		return this.attachments.toString() + "\n" + this.tokens.toString();
	}
}
