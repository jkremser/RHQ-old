package org.rhq.augeas.tree.impl;

import java.io.File;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasRootNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Default implementation of the tree builder.
 * This just loads the data from Augeas and represents the returned data as the
 * node tree without any modifications.
 * 
 * @author Filip Drabek
 */
public class DefaultAugeasTreeBuilder implements AugeasTreeBuilder {
    private static String AUGEAS_DATA_PATH = File.separatorChar + "files";

    public DefaultAugeasTreeBuilder() {
    }

    public AugeasTree buildTree(AugeasProxy component, AugeasConfiguration moduleConfig, String name, boolean lazy)
        throws AugeasTreeException {

        AugeasTree tree;
        AugeasModuleConfig module = moduleConfig.getModuleByName(name);
        if (lazy = true)
            tree = new AugeasTreeLazy(component.getAugeas(), module);
        else
            tree = new AugeasTreeReal(component.getAugeas(), module);

        AugeasNode rootNode = new AugeasRootNode();

        for (String fileName : module.getConfigFiles()) {
            rootNode.addChildNode(tree.createNode(AUGEAS_DATA_PATH + File.separatorChar + fileName));
        }

        tree.setRootNode(rootNode);

        return tree;
    }

}
