package chiralsoftware.linkerwebp.impl;

import java.util.logging.Logger;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

/**
 * Implement Webp metadata
 */
final class WebpMetaData extends IIOMetadata {

    private static final Logger LOG = Logger.getLogger(WebpMetaData.class.getName());
    
    static final String nativeMetadataFormatName =
                "chiralsoftware.imageio.WebpMetadata_1.0";
    
    static final String nativeMetadataFormatClassName =
                "chiralsoftware.imageio.WebpMetadata";
        
    @Override
    public boolean isReadOnly() {
        return true; // fix this
    }

    @Override
    public Node getAsTree(String formatName) {
        final IIOMetadataNode root =
                        new IIOMetadataNode(nativeMetadataFormatName);
        // no metadata for now - just return root
        return root;
    }

    @Override
    public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
        LOG.fine("does nothing");
    }

    @Override
    public void reset() {
        LOG.fine("does nothing");
    }
    
}
