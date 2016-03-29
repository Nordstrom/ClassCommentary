/* 
 * @(#) $Id:  $
 */
package painpoint.decoration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;

public class CCProjectViewNodeDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode viewNode, PresentationData presentationData) {
        if (viewNode != null && viewNode instanceof ClassTreeNode) {
            final Project project = viewNode.getProject();
            final ClassTreeNode classTreeNode = (ClassTreeNode) viewNode;
            final PresentationData fPresentationData = presentationData;

            PainPointPresentation presentation = PainPointPresentationFactory.creatPresentation(project, classTreeNode);
            ClassFileDecoration classFileDecoration = new ClassFileDecoration(presentation);
            classFileDecoration.decorate(classTreeNode, fPresentationData);
        }
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {
        PluginManager.getLogger().warn("Decorate package dependencies");
    }
}
