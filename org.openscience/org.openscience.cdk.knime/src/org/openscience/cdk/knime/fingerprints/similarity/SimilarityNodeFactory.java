package org.openscience.cdk.knime.fingerprints.similarity;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Similarity" Node.
 * 
 *
 * @author Stephan Beisken
 */
public class SimilarityNodeFactory 
        extends NodeFactory<SimilarityNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SimilarityNodeModel createNodeModel() {
        return new SimilarityNodeModel();
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
    public NodeView<SimilarityNodeModel> createNodeView(final int viewIndex,
            final SimilarityNodeModel nodeModel) {
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
        return new SimilarityNodeDialog();
    }

}

