package cz.cvut.fel.nalida.interpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import cz.cvut.fel.nalida.schema.Attribute;
import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.schema.Entity;

public class Interpretation {
	final protected Set<Attachment<Token>> attachments;
	final protected List<Token> tokens;
	final protected Token root;

	public Interpretation(List<Token> tokenList, Integer root, Set<Attachment<Integer>> attachmentsTemplate) {
		this.tokens = tokenList;
		this.attachments = createAttachments(attachmentsTemplate);
		this.root = (root == null) ? null : this.tokens.get(root.intValue());
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

	public Token getRoot() {
		return this.root;
	}

	public Collection<Token> getTokens(final ElementType... type) {
		return Collections2.filter(this.tokens, new Predicate<Token>() {
			@Override
			public boolean apply(Token token) {
				return token.isType(type);
			}
		});
	}

	public List<Element> getElements() {
		List<Element> elements = new ArrayList<>();
		for (Token token : getTokens()) {
			elements.add(token.getElement());
		}
		return elements;
	}

	public int getEntityCount() {
		Set<Entity> entities = new HashSet<>();
		for (Token token : getTokens()) {
			if (!token.isType(ElementType.WH_WORD)) {
				entities.add(token.getEntityElement());
			}
			if (token.isType(ElementType.SUBRESOURCE)
					|| (token.isType(ElementType.ATTRIBUTE) && !((Attribute) token.getElement()).isPrimitiveType())) {
				entities.add(((Attribute) token.getElement()).getTypeEntity());
			}
		}
		return entities.size();
	}

	public int getEntityCount(ElementType... types) {
		Set<Entity> entities = new HashSet<>();
		for (Token token : getTokens()) {
			if (token.isType(types)) {
				if (!token.isType(ElementType.WH_WORD)) {
					entities.add(token.getEntityElement());
				}
				if (token.isType(ElementType.SUBRESOURCE)
						|| (token.isType(ElementType.ATTRIBUTE) && !((Attribute) token.getElement()).isPrimitiveType())) {
					entities.add(((Attribute) token.getElement()).getTypeEntity());
				}
			}
		}
		return entities.size();
	}

	public List<Element> getElements(final ElementType... type) {
		List<Element> elements = new ArrayList<>();
		for (Token token : getTokens(type)) {
			elements.add(token.getElement());
		}
		return elements;
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
