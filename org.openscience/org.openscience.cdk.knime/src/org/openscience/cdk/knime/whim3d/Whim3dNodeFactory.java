package org.openscience.cdk.knime.whim3d;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Whim3d" Node. Holistic descriptors described by Todeschini et al. The descriptors
 * are based on a number of atom weightings. There are 5 different possible weightings implemented.
 * 
 * @author Stephan Beisken
 */
public class Whim3dNodeFactory extends NodeFactory<Whim3dNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Whim3dNodeModel createNodeModel() {

		return new Whim3dNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {

		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<Whim3dNodeModel> createNodeView(final int viewIndex, final Whim3dNodeModel nodeModel) {

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

		return new Whim3dNodeDialog();
	}
}
