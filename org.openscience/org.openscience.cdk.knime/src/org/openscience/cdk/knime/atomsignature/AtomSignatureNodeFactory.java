package org.openscience.cdk.knime.atomsignature;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AtomSignature" Node.
 * 
 * @author ldpf
 */
public class AtomSignatureNodeFactory extends NodeFactory<AtomSignatureNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AtomSignatureNodeModel createNodeModel() {

		return new AtomSignatureNodeModel();
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
	public NodeView<AtomSignatureNodeModel> createNodeView(final int viewIndex, final AtomSignatureNodeModel nodeModel) {

		// Why this has to be turn as null?
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

		return new AtomSignatureNodeDialog();
	}
}
