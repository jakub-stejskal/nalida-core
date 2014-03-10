package cz.cvut.fel.nlidb4kos;

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
}
