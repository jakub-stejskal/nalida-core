package cz.cvut.fel.nalida.db;

import java.util.List;

import javax.xml.xpath.XPathExpressionException;

public interface Query {

	String execute() throws Exception;

	List<String> execute(List<String> queryParams) throws Exception;

	List<String> project(List<String> queryResponse) throws XPathExpressionException;

}
