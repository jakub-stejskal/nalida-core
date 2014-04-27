package cz.cvut.fel.nalida.interpretation;

public class Attachment<T> {
	T source;
	T target;
	String type;

	public Attachment(T source, T target, String type) {
		super();
		this.source = source;
		this.target = target;
		this.type = type;
	}

	@Override
	public String toString() {
		return this.source + "-" + this.type + "-" + this.target;
	}

	public T getSource() {
		return this.source;
	}

	public T getTarget() {
		return this.target;
	}
}
