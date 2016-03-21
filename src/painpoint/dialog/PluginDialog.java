package painpoint.dialog;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import java.awt.event.ActionEvent;

import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import painpoint.component.ProjectViewManager;
import painpoint.decoration.PainPointPresentation;
import painpoint.domain.painpoint.PainPointDomain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.SQLException;

public class PluginDialog extends JDialog {

    public PluginDialog(PainPointPresentation painPointPresentation, PainPointDomain painPointDomain, final Project project, final DataContext dataContext) {
        super(new JFrame(), "Plugin Dialog");

        try {
            painPointDomain.getPainPointMap(true);
        }
        catch (SQLException sEx) {
            System.out.println("SQLException.." + sEx.getMessage());
        }
        final ProjectViewManager projectViewManager = ProjectViewManager.getInstance(project);
        setSize(100,100);

        System.out.println("creating the window..");
        // set the position of the window
        Point p = new Point(400, 400);
        setLocation(p.x, p.y);

        // Create a message
        JPanel messagePane = new JPanel();
        messagePane.add(new JLabel("Report"));
        // get content pane, which is usually the
        // Container of all the dialog's components.
        getContentPane().add(messagePane);

        JPanel cbPane = new JPanel();
        JCheckBox jCheckBox = new JCheckBox();
        jCheckBox.setSelected(painPointPresentation.currentUserHasPainPoint());
        jCheckBox.setText("Pain Point");
        final PainPointPresentation fPainPointPresentation = painPointPresentation;
        final PainPointDomain fPainPointDomain = painPointDomain;
        final ProjectViewManager fProjectViewManager = projectViewManager;
        final Project fProject = project;
        jCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JCheckBox jCheckBox1 = (JCheckBox)actionEvent.getSource();
                boolean isSelected = jCheckBox1.isSelected();
                Integer classId = fPainPointPresentation.getClassId();
                String gitPair = fPainPointPresentation.getGitPairString();
                boolean success = fPainPointDomain.addOrUpdateForClass(classId, gitPair, isSelected);
                if(success) {
                    try {
                        fPainPointDomain.getPainPointMap(true);
                        successPopup(dataContext);
                    } catch (SQLException sqlEx) {
                        System.out.println("SQLException: " + sqlEx.getMessage());
                        failPopup(dataContext);
                    }
                    fProjectViewManager.refreshProjectView(fProject);
                }
                else {
                    failPopup(dataContext);
                }
            }
        });
        cbPane.add(jCheckBox);
        messagePane.add(cbPane);

        // Create a button
        JPanel buttonPane = new JPanel();
        JButton button = new JButton("Close me");
        buttonPane.add(button);

        // set action listener on the button
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("disposing the window..");
                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(buttonPane, BorderLayout.PAGE_END);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void failPopup(final DataContext dataContext) {
        String htmlText = "Failed to add or update painpoint.  Check the database.  Is it up?";

        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(DataKeys.PROJECT.getData(dataContext));

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(htmlText, MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    private void successPopup(final DataContext dataContext) {
        String htmlText = "PainPoint Added.";

        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(DataKeys.PROJECT.getData(dataContext));

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(htmlText, MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        Action action = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                System.out.println("escaping..");
                setVisible(false);
                dispose();
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", action);
        return rootPane;
    }
}
