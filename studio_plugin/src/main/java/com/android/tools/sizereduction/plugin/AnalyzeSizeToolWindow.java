package com.android.tools.sizereduction.plugin;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.intellij.icons.AllIcons.Actions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;

/** Tool Window for Size Analyzer. */
public final class AnalyzeSizeToolWindow {
  private final Tree suggestionTree;
  private final JPanel window;
  private final Splitter splitter;
  private final JScrollPane leftPane;
  private final JPanel descriptionPanel;
  private final Action autofixAction;
  private final JButton descriptionAutoFixButton;
  private final Component descriptionAutoFixStrut;
  private final JBLabel descriptionTitle;
  private final Component descriptionTitleStrut;
  private final JBLabel description;
  private final Component descriptionStrut;
  private final HyperlinkLabel descriptionMoreInfo;
  private final JPanel categoryPanel;
  private final SimpleColoredComponent categoryTitle;
  private final JPanel suggestionsPanel;
  private ImmutableListMultimap<Category, Suggestion> categorizedSuggestions;
  private ImmutableList<Category> categoryDisplayOrder;

  public AnalyzeSizeToolWindow(
      ToolWindow toolWindow,
      Project project,
      ImmutableListMultimap<Category, Suggestion> categorizedSuggestions,
      ImmutableList<Category> categoryDisplayOrder) {
    this.categorizedSuggestions = categorizedSuggestions;
    this.categoryDisplayOrder = categoryDisplayOrder;

    suggestionTree = createSuggestionTree();

    window = new JPanel(new BorderLayout());
    splitter = new OnePixelSplitter(false, 0.5f);
    splitter.setHonorComponentsMinimumSize(false);
    leftPane = ScrollPaneFactory.createScrollPane();
    leftPane.setViewportView(suggestionTree);

    descriptionPanel = new JPanel();
    descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.PAGE_AXIS));

    descriptionAutoFixButton = new JButton("Apply AutoFix", Actions.IntentionBulb);
    descriptionAutoFixButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    descriptionAutoFixButton.setVisible(false);
    autofixAction =
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            assert this.getValue("autofix") != null
                : "Autofix action called with null autofix value";
            if (this.getValue("autofix") != null) {
              String indicatorText =
                  this.getValue("indicatorText") != null
                      ? (String) this.getValue("indicatorText")
                      : "Autofixing...";
              AutoFix autoFix = (AutoFix) this.getValue("autofix");
              ProgressManager.getInstance()
                  .run(
                      new Task.Backgroundable(project, indicatorText) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                          indicator.setText(indicatorText);

                          ApplicationManager.getApplication()
                              .invokeLater(
                                  new Runnable() {
                                    @Override
                                    public void run() {
                                      autoFix.apply();
                                    }
                                  });
                        }
                      });
              ((DefaultTreeModel) suggestionTree.getModel())
                  .removeNodeFromParent((DefaultMutableTreeNode) this.getValue("autofixNode"));
              descriptionAutoFixButton.setEnabled(false);
            }
          }
        };
    descriptionAutoFixButton.setAction(autofixAction);

    descriptionAutoFixStrut = Box.createVerticalStrut(16);
    descriptionAutoFixStrut.setVisible(false);

    descriptionTitle = new JBLabel("This is the title of the issue on the left hand side.");
    descriptionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
    descriptionTitle.setVisible(false);

    descriptionTitleStrut = Box.createVerticalStrut(8);
    descriptionTitleStrut.setVisible(false);

    description = new JBLabel("This is a descriptive text about the issue on the left hand side.");
    description.setAlignmentX(Component.LEFT_ALIGNMENT);
    description.setVisible(false);

    descriptionStrut = Box.createVerticalStrut(8);
    descriptionStrut.setVisible(false);

    descriptionMoreInfo = new HyperlinkLabel("Learn more");
    descriptionMoreInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
    // Constrain the Hyperlink to the area the text covers.
    descriptionMoreInfo.setMaximumSize(descriptionMoreInfo.getPreferredSize());
    descriptionMoreInfo.setVisible(false);

    descriptionPanel.add(descriptionAutoFixButton);
    descriptionPanel.add(descriptionAutoFixStrut);
    descriptionPanel.add(descriptionTitle);
    descriptionPanel.add(descriptionTitleStrut);
    descriptionPanel.add(description);
    descriptionPanel.add(descriptionStrut);
    descriptionPanel.add(descriptionMoreInfo);
    descriptionPanel.setBorder(BorderFactory.createEmptyBorder(16, 12, 10, 10));

    categoryPanel = new JPanel();
    categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.PAGE_AXIS));

    categoryTitle = new SimpleColoredComponent();
    categoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

    suggestionsPanel = new JPanel();
    suggestionsPanel.setLayout(new BoxLayout(suggestionsPanel, BoxLayout.PAGE_AXIS));
    suggestionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    categoryPanel.add(categoryTitle);
    categoryPanel.add(suggestionsPanel);
    categoryPanel.setBorder(BorderFactory.createEmptyBorder(16, 12, 10, 10));

    splitter.setFirstComponent(leftPane);
    splitter.setSecondComponent(descriptionPanel);

    window.add(splitter, BorderLayout.CENTER);
  }

  public JPanel getContent() {
    window.validate();
    return window;
  }

  private Tree createSuggestionTree() {
    DefaultMutableTreeNode rootNode =
        new DefaultMutableTreeNode("Hurray! Your application is already optimized for size.");
    SuggestionDataFactory factory = new SuggestionDataFactory();
    for (Category category : categoryDisplayOrder) {
      ImmutableList<Suggestion> suggestions = categorizedSuggestions.get(category);
      if (!suggestions.isEmpty()) {
        DefaultMutableTreeNode categoryNode =
            new DefaultMutableTreeNode();
        long totalBytesSaved = 0;
        for (Suggestion suggestion : suggestions) {
          DefaultMutableTreeNode suggestionNode =
              new DefaultMutableTreeNode(factory.fromSuggestion(suggestion));
          categoryNode.add(suggestionNode);
          totalBytesSaved +=
              suggestion.getEstimatedBytesSaved() != null ? suggestion.getEstimatedBytesSaved() : 0;
        }
        categoryNode.setUserObject(new CategoryData(category, suggestions.size(), totalBytesSaved));
        rootNode.add(categoryNode);
      }
    }
    Tree tree = new Tree(rootNode);
    // Show the root if we do not have any size suggestions for the developer.
    tree.setRootVisible(rootNode.getChildCount() == 0);

    tree.addTreeSelectionListener(
        (TreeSelectionEvent e) -> {
          Object selectedNode = e.getPath().getLastPathComponent();
          Object selection = ((DefaultMutableTreeNode) selectedNode).getUserObject();
          descriptionAutoFixButton.setVisible(false);
          descriptionAutoFixStrut.setVisible(false);
          descriptionTitle.setVisible(false);
          descriptionTitleStrut.setVisible(false);
          description.setVisible(false);
          descriptionStrut.setVisible(false);
          descriptionMoreInfo.setVisible(false);
          if (selection instanceof SuggestionData) {
            splitter.setSecondComponent(descriptionPanel);
            descriptionTitle.setVisible(true);
            descriptionTitleStrut.setVisible(true);
            description.setVisible(true);
            showInformationInDescriptionPanelFor((DefaultMutableTreeNode) selectedNode);
          } else if (selection instanceof CategoryData) {
            splitter.setSecondComponent(categoryPanel);
            showCategoryInCategoryPanelFor((DefaultMutableTreeNode) selectedNode);
          } else if (selection instanceof String) {
            splitter.setSecondComponent(descriptionPanel);
            description.setVisible(true);
            String escapedSelection = escapeHtml4((String) selection);
            description.setText("<html> " + escapedSelection + "</html>");
          }
        });

    tree.setCellRenderer(new SuggestionTreeCellRenderer(tree.getCellRenderer()));

    return tree;
  }

  private void showInformationInDescriptionPanelFor(DefaultMutableTreeNode selectedNode) {
    SuggestionData suggestion = (SuggestionData) selectedNode.getUserObject();
    // html tag is used so the text wraps.
    String escapedTitle =
        suggestion.getTitle() != null ? "<b> " + escapeHtml4(suggestion.getTitle()) + " </b>" : "";
    if (suggestion.getBytesSaved() != null) {
      escapedTitle +=
          "<span style=\"color:"
              + "#" + ColorUtil.toHex(UIUtil.getInactiveTextColor())
              + ";\"> &nbsp;&nbsp;"
              + "Estimated savings: "
              + suggestion.getBytesSaved()
              + "</span>";
    }
    descriptionTitle.setText("<html> " + escapedTitle + " </html>");
    String escapedDescription =
        suggestion.getDesc() != null ? escapeHtml4(suggestion.getDesc()) : "";
    description.setText("<html> " + escapedDescription + " </html>");
    if (suggestion.getMoreInfo() != null) {
      descriptionStrut.setVisible(true);
      descriptionMoreInfo.setVisible(true);
      descriptionMoreInfo.setHyperlinkTarget(suggestion.getMoreInfo());
    }
    if (suggestion.getAutoFix() != null) {
      descriptionAutoFixButton.setVisible(true);
      descriptionAutoFixButton.setText(suggestion.getAutoFixTitle());
      descriptionAutoFixButton.setIcon(Actions.IntentionBulb);
      descriptionAutoFixButton.setEnabled(true);
      descriptionAutoFixStrut.setVisible(true);
      autofixAction.putValue("autofix", suggestion.getAutoFix());
      autofixAction.putValue("autofixNode", selectedNode);
    }
    descriptionPanel.revalidate();
  }

  private void showCategoryInCategoryPanelFor(DefaultMutableTreeNode categoryNode) {
    CategoryData category = (CategoryData) categoryNode.getUserObject();
    categoryTitle.clear();
    categoryTitle.append(category.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    if (category.totalSizeSaved() != null) {
      categoryTitle.append(
          "  Estimated savings: " + category.totalSizeSaved(),
          SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    categoryTitle.setMaximumSize(categoryTitle.getPreferredSize());

    suggestionsPanel.removeAll();
    for (Enumeration<?> nodeEnumeration = categoryNode.children();
        nodeEnumeration.hasMoreElements(); ) {
      Object nodeValue = nodeEnumeration.nextElement();
      if (nodeValue instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode suggestionNode = (DefaultMutableTreeNode) nodeValue;
        SuggestionData suggestion = (SuggestionData) suggestionNode.getUserObject();
        suggestionsPanel.add(
            new LinkLabel<>(
                suggestion.getTitle(),
                null,
                new LinkListener<Object>() {
                  @Override
                  @SuppressWarnings("rawtypes") // Declaration uses raw type, needed for override.
                  public void linkSelected(LinkLabel source, Object linkData) {
                    suggestionTree.clearSelection();
                    TreePath selectedNode =
                        new TreePath(((DefaultMutableTreeNode) linkData).getPath());
                    suggestionTree.addSelectionPath(selectedNode);
                    suggestionTree.scrollPathToVisible(selectedNode);
                  }
                },
                suggestionNode));
      }
    }
  }

  public void setCategorizedSuggestions(
      ImmutableListMultimap<Category, Suggestion> categorizedSuggestions) {
    this.categorizedSuggestions = categorizedSuggestions;
  }

  public void setCategoryDisplayOrder(ImmutableList<Category> categoryDisplayOrder) {
    this.categoryDisplayOrder = categoryDisplayOrder;
  }
}
