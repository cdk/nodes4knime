package org.openscience.cdk.knime.symmetrycalculator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SymmetryCalculator" Node.
 * 
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class SymmetryCalculatorNodeFactory extends NodeFactory<SymmetryCalculatorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SymmetryCalculatorNodeModel createNodeModel() {

		return new SymmetryCalculatorNodeModel();
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
	public NodeView<SymmetryCalculatorNodeModel> createNodeView(final int viewIndex,
			final SymmetryCalculatorNodeModel nodeModel) {

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

		return new SymmetryCalculatorNodeDialog();
	}

}
