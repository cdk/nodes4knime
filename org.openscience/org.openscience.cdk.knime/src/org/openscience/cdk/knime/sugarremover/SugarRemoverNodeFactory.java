package org.openscience.cdk.knime.sugarremover;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SugarRemover" Node.
 * 
 * 
 * @author ldpf
 */
public class SugarRemoverNodeFactory extends NodeFactory<SugarRemoverNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SugarRemoverNodeModel createNodeModel() {

		return new SugarRemoverNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {

		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<SugarRemoverNodeModel> createNodeView(final int viewIndex, final SugarRemoverNodeModel nodeModel) {

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {

		return new SugarRemoverNodeDialog();
	}

}
