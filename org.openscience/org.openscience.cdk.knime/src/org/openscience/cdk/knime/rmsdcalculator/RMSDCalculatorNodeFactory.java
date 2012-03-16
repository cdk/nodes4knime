package org.openscience.cdk.knime.rmsdcalculator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RMSDCalculator" Node.
 * 
 *
 * @author Luis F. de Figueiredo
 */
public class RMSDCalculatorNodeFactory 
        extends NodeFactory<RMSDCalculatorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RMSDCalculatorNodeModel createNodeModel() {
        return new RMSDCalculatorNodeModel();
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
    public NodeView<RMSDCalculatorNodeModel> createNodeView(final int viewIndex,
            final RMSDCalculatorNodeModel nodeModel) {
        return new RMSDCalculatorNodeView(nodeModel);
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
        return new RMSDCalculatorNodeDialog();
    }

}

