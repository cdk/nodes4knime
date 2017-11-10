package org.openscience.cdk.knime.nodes.depiction;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Depiction" Node.
 * Depict CDK structures into images
 *
 * @author Sameul Webb, Lhasa Limited
 */
public class DepictionNodeFactory 
        extends NodeFactory<DepictionNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DepictionNodeModel createNodeModel() {
        return new DepictionNodeModel();
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
    public NodeView<DepictionNodeModel> createNodeView(final int viewIndex,
            final DepictionNodeModel nodeModel) {
        return new DepictionNodeView(nodeModel);
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
        return new DepictionNodeDialog();
    }

}

