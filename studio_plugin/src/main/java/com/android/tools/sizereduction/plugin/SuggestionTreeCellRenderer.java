package com.android.tools.sizereduction.plugin;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

/**
 * Custom tree cell renderer for displaying the left pane suggestion tree.
 */
public class SuggestionTreeCellRenderer implements TreeCellRenderer {
  private final TreeCellRenderer parentRenderer;

  SuggestionTreeCellRenderer(TreeCellRenderer parentRenderer) {
    this.parentRenderer = parentRenderer;
  }

  @Override
  public Component getTreeCellRendererComponent(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
    final Component rendererComponent =
        parentRenderer.getTreeCellRendererComponent(
            tree, value, selected, expanded, leaf, row, hasFocus);
    if (rendererComponent instanceof SimpleColoredComponent) {
      Object nodeObject = ((DefaultMutableTreeNode) value).getUserObject();
      for (SimpleColoredComponent.ColoredIterator it =
          ((SimpleColoredComponent) rendererComponent).iterator();
          it.hasNext(); ) {
        it.next();
        int offset = it.getOffset();
        int endOffset = it.getEndOffset();
        if (selected) {
          if (nodeObject instanceof CategoryData) {
            it.split(
                endOffset - offset,
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.WHITE));
          } else if (nodeObject instanceof SuggestionData) {
            it.split(
                endOffset - offset,
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.WHITE));
          }
        } else {
          if (nodeObject instanceof CategoryData) {
            it.split(endOffset - offset, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          }
        }
      }

      SimpleTextAttributes annotationStyle =
          selected
              ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.WHITE)
              : SimpleTextAttributes.GRAYED_ATTRIBUTES;
      if (nodeObject instanceof CategoryData) {
        CategoryData category = (CategoryData) nodeObject;
        ((SimpleColoredComponent) rendererComponent)
            .append(
                "  " + category.totalSuggestions(), annotationStyle);
        ((SimpleColoredComponent) rendererComponent)
            .append(
                category.totalSuggestions() == 1 ? " recommendation" : " recommendations",
                annotationStyle);
        if (category.totalSizeSaved() != null) {
          ((SimpleColoredComponent) rendererComponent)
              .append(", " + category.totalSizeSaved(), annotationStyle);
        }
      } else if (nodeObject instanceof SuggestionData) {
        SuggestionData suggestion = (SuggestionData) nodeObject;
        if (suggestion.getBytesSaved() != null) {
          ((SimpleColoredComponent) rendererComponent)
              .append("  " + suggestion.getBytesSaved(), annotationStyle);
        }
      }
    }
    return rendererComponent;
  }

}
