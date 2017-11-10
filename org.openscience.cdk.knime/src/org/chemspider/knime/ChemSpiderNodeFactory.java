/*

Copyright (C) 2007-2013 Egon Willighagen <egon.willighagen@gmail.com>

Permission to use, copy, modify, and/or distribute this software for any 
purpose with or without fee is hereby granted, provided that the above 
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES 
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, 
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
THIS SOFTWARE.

 */
package org.chemspider.knime;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ChemSpider" Node. Download Structures from
 * ChemSpider.com.
 *
 * @author Egon Willighagen
 */
public class ChemSpiderNodeFactory extends NodeFactory<ChemSpiderNodeModel> {

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public ChemSpiderNodeModel createNodeModel() {
        return new ChemSpiderNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeView(int,
     *      org.knime.core.node.NodeModel)
     */
    @Override
    public NodeView<ChemSpiderNodeModel> createNodeView(final int viewIndex,
            final ChemSpiderNodeModel nodeModel) {
        return null;
    }

    /**
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ChemSpiderNodeDialog();
    }

}
