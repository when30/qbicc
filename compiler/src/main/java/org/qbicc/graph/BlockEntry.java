package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A node representing block entry.  Block entry nodes have no dependencies.
 */
public final class BlockEntry extends AbstractNode implements PinnedNode, Action {
    private final BlockLabel blockLabel;

    BlockEntry(final Node callSite, final ExecutableElement element, final BlockLabel blockLabel) {
        super(callSite, element, 0, -1);
        this.blockLabel = blockLabel;
    }

    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return blockLabel.hashCode();
    }

    @Override
    String getNodeName() {
        return "BlockEntry";
    }

    public boolean equals(final Object other) {
        return other instanceof BlockEntry && equals((BlockEntry) other);
    }

    public boolean equals(final BlockEntry other) {
        return this == other || other != null && blockLabel.equals(other.blockLabel);
    }
}
