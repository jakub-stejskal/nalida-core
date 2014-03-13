package cz.cvut.fel.nlidb4kos.db;

import cz.cvut.fel.nlidb4kos.db.Lexicon.ElementType;

public class Element {

	protected ElementType entityType;
	protected String entityName;

	public Element(ElementType entityType, String entityName) {
		this.entityType = entityType;
		this.entityName = entityName;
	}

	public ElementType getEntityType() {
		return this.entityType;
	}

	public String getEntityName() {
		return this.entityName;
	}

	@Override
	public String toString() {
		return this.entityType + "/" + this.entityName;
	}

	public boolean isType(ElementType... types) {
		for (ElementType type : types) {
			if (this.entityType == type) {
				return true;
			}
		}
		return false;
	}

	public Element toEntityElement() {
		try {
			return new Element(ElementType.ENTITY, this.entityName.split("\\.")[0]);
		} catch (Exception e) {
			throw new RuntimeException(this.entityName, e);
		}
	}
}
