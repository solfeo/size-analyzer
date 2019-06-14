package com.android.tools.sizereduction.plugin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.treeStructure.Tree;
import java.awt.Component;
import java.util.Enumeration;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AnalyzeSizeToolWindowTest {
  AnalyzeSizeToolWindow toolWindow;

  AutoFix webPAutofix = mock(AutoFix.class);

  Suggestion webPSuggestion =
      Suggestion.create(
          Suggestion.IssueType.WEBP,
          Suggestion.Category.WEBP,
          Payload.getDefaultInstance(),
          "Convert drawing.png to webp with lossless encoding",
          20000L,
          webPAutofix);

  Suggestion streamingSuggestion =
      Suggestion.create(
          Suggestion.IssueType.MEDIA_STREAMING,
          Suggestion.Category.LARGE_FILES,
          Payload.getDefaultInstance(),
          "Stream media file foo.mp4 from the internet to avoid" + " bundling in apk",
          5000L,
          /* autoFix= */ null);

  SuggestionDataFactory suggestionDataFactory = new SuggestionDataFactory();
  SuggestionData webPSuggestionData = suggestionDataFactory.fromSuggestion(webPSuggestion);
  SuggestionData streamingSuggestionData =
      suggestionDataFactory.fromSuggestion(streamingSuggestion);
  CategoryData webPCategoryData = new CategoryData(Category.WEBP, 2);

  Project project = mock(Project.class);

  @Before
  public void setUp() {
    ImmutableListMultimap<Category, Suggestion> categorizedSuggestions =
        ImmutableListMultimap.<Category, Suggestion>builder()
            .putAll(
                Category.WEBP,
                webPSuggestion,
                Suggestion.create(
                    Suggestion.IssueType.WEBP,
                    Suggestion.Category.WEBP,
                    Payload.getDefaultInstance(),
                    "Convert drawing.png to webp with lossless encoding",
                    10000L,
                    new AutoFix() {
                      @Override
                      public void apply() {}
                    }))
            .putAll(
                Category.LARGE_FILES,
                streamingSuggestion,
                Suggestion.create(
                    Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
                    Suggestion.Category.LARGE_FILES,
                    Payload.getDefaultInstance(),
                    "Place large file foo.mp4 inside an on demand"
                        + " dynamic-feature to avoid bundlingin apk",
                    3600L,
                    /* autoFix= */ null))
            .put(
                Category.PROGUARD,
                Suggestion.create(
                    Suggestion.IssueType.PROGUARD_NO_SHRINKING,
                    Category.PROGUARD,
                    Payload.getDefaultInstance(),
                    "It seems that you are not using Proguard/R8, consider"
                        + " enabling it in your application.",
                    /* estimatedBytesSaved= */ null,
                    /* autoFix= */ null))
            .build();
    ImmutableList<Category> categoryDisplayOrder =
        ImmutableList.of(
            Suggestion.Category.WEBP,
            Suggestion.Category.LARGE_FILES,
            Suggestion.Category.PROGUARD);

    toolWindow =
        new AnalyzeSizeToolWindow(null, project, categorizedSuggestions, categoryDisplayOrder);
  }

  @Test
  public void selectingTreeNodeChangesDescriptionPanel() {
    JPanel toolPanel = toolWindow.getContent();
    Component[] components = toolPanel.getComponents();
    OnePixelSplitter splitter = (OnePixelSplitter) components[0];
    JBScrollPane leftPane = (JBScrollPane) splitter.getFirstComponent();
    JViewport leftView = leftPane.getViewport();
    Tree suggestionTree = (Tree) leftView.getView();
    JPanel rightPane = (JPanel) splitter.getSecondComponent();

    assertThat(suggestionTree.getSelectionCount()).isEqualTo(0);
    for (Component component : rightPane.getComponents()) {
      if (component instanceof JButton
          || component instanceof JBLabel
          || component instanceof HyperlinkLabel) {
        assertThat(component.isVisible()).isFalse();
      }
    }

    TreePath webPPath = getTreePathWithString(suggestionTree, webPSuggestionData.toString());
    suggestionTree.addSelectionPath(webPPath);

    assertThat(suggestionTree.getSelectionCount()).isEqualTo(1);
    for (Component component : rightPane.getComponents()) {
      if (component instanceof JButton
          || component instanceof JBLabel
          || component instanceof HyperlinkLabel) {
        assertThat(component.isVisible()).isTrue();
      }
      if (component instanceof JButton) {
        assertThat(((JButton) component).getText()).isEqualTo(webPSuggestionData.getAutoFixTitle());
      }
      if (component instanceof JBLabel) {
        assertThat(((JBLabel) component).getText())
            .containsMatch(
                "(" + webPSuggestionData.getTitle() + "|" + webPSuggestionData.getDesc() + ")");
      }
    }
  }

  @Test
  public void selectingCategoryNodeChangesCategoryPanel() {
    JPanel toolPanel = toolWindow.getContent();
    Component[] components = toolPanel.getComponents();
    OnePixelSplitter splitter = (OnePixelSplitter) components[0];
    JBScrollPane leftPane = (JBScrollPane) splitter.getFirstComponent();
    JViewport leftView = leftPane.getViewport();
    Tree suggestionTree = (Tree) leftView.getView();

    assertThat(suggestionTree.getSelectionCount()).isEqualTo(0);

    TreePath webPPath = getTreePathWithString(suggestionTree, webPCategoryData.toString());
    suggestionTree.addSelectionPath(webPPath);
    JPanel rightPane = (JPanel) splitter.getSecondComponent();

    assertThat(suggestionTree.getSelectionCount()).isEqualTo(1);
    for (Component component : rightPane.getComponents()) {
      if (component instanceof SimpleColoredComponent) {
        assertThat(component.toString()).contains(webPCategoryData.toString());
        assertThat(component.toString()).contains("Estimated savings: 29.30 KB");
      } else if (component instanceof JPanel) {
        Component[] suggestionPanelComponents = ((JPanel) component).getComponents();
        assertThat(suggestionPanelComponents).hasLength(2);
        for (Component linkLabel : suggestionPanelComponents) {
          assertThat(((LinkLabel<?>) linkLabel).getText()).isEqualTo(webPSuggestionData.getTitle());
        }
      }
    }
  }

  @Test
  public void categoryNodesHaveCorrectExtraInformation() {
    JPanel toolPanel = toolWindow.getContent();
    Component[] components = toolPanel.getComponents();
    OnePixelSplitter splitter = (OnePixelSplitter) components[0];
    JBScrollPane leftPane = (JBScrollPane) splitter.getFirstComponent();
    JViewport leftView = leftPane.getViewport();
    Tree suggestionTree = (Tree) leftView.getView();

    TreePath webPPath = getTreePathWithString(suggestionTree, webPCategoryData.toString());
    TreeCellRenderer treeCellRenderer = suggestionTree.getCellRenderer();
    Component renderedNode =
        treeCellRenderer.getTreeCellRendererComponent(
            suggestionTree,
            webPPath.getLastPathComponent(),
            false,
            false,
            false,
            suggestionTree.getRowForPath(webPPath),
            false);
    assertThat(renderedNode.toString()).contains("2 recommendations");
    assertThat(renderedNode.toString()).contains("29.30 KB");
  }

  @Test
  public void autofixIsNotShownWhenNull() {
    JPanel toolPanel = toolWindow.getContent();
    Component[] components = toolPanel.getComponents();
    OnePixelSplitter splitter = (OnePixelSplitter) components[0];
    JBScrollPane leftPane = (JBScrollPane) splitter.getFirstComponent();
    JViewport leftView = leftPane.getViewport();
    Tree suggestionTree = (Tree) leftView.getView();
    JPanel rightPane = (JPanel) splitter.getSecondComponent();

    TreePath streamingPath =
        getTreePathWithString(suggestionTree, streamingSuggestionData.toString());
    suggestionTree.addSelectionPath(streamingPath);

    for (Component component : rightPane.getComponents()) {
      if (component instanceof JButton) {
        assertThat(component.isVisible()).isFalse();
      }
    }
  }

  private TreePath getTreePathWithString(Tree tree, String searchTarget) {
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
    for (Enumeration<?> treeEnumeration = rootNode.depthFirstEnumeration();
        treeEnumeration.hasMoreElements(); ) {
      Object node = treeEnumeration.nextElement();
      if (node instanceof DefaultMutableTreeNode) {
        Object value = ((DefaultMutableTreeNode) node).getUserObject();
        if (value.toString().equals(searchTarget)) {
          return new TreePath(((DefaultMutableTreeNode) node).getPath());
        }
      }
    }
    return null;
  }
}
