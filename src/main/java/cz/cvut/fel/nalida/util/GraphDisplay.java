package cz.cvut.fel.nalida.util;

import java.awt.Dimension;

import javax.swing.JApplet;
import javax.swing.JFrame;

import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;

import cz.cvut.fel.nalida.schema.Element;

public class GraphDisplay extends JApplet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Dimension DEFAULT_SIZE = new Dimension(800, 600);

	// 
	private JGraphXAdapter<Element, DefaultWeightedEdge> jgxAdapter;
	private final Graph<Element, DefaultWeightedEdge> graph;

	public GraphDisplay(Graph<Element, DefaultWeightedEdge> graph) {
		this.graph = graph;
	}

	public static void displayGraph(Graph<Element, DefaultWeightedEdge> graph) {
		GraphDisplay applet = new GraphDisplay(graph);
		applet.init();

		JFrame frame = new JFrame();
		frame.getContentPane().add(applet);
		frame.setTitle("JGraphT Adapter to JGraph Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}

	/**
	 * @see java.applet.Applet#init().
	 */
	@Override
	public void init() {

		// create a visualization using JGraph, via an adapter
		this.jgxAdapter = new JGraphXAdapter<Element, DefaultWeightedEdge>(this.graph);
		this.jgxAdapter.getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_NOLABEL, "1");

		getContentPane().add(new mxGraphComponent(this.jgxAdapter));
		resize(DEFAULT_SIZE);

		mxHierarchicalLayout layout = new mxHierarchicalLayout(this.jgxAdapter);

		layout.execute(this.jgxAdapter.getDefaultParent());
	}
}