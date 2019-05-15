package org.netbeans.core.windows.view.ui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import org.netbeans.core.windows.AuxWindowImpl;
import org.netbeans.core.windows.AuxWindowTracker;
import org.netbeans.core.windows.ModeImpl;
import org.netbeans.core.windows.WindowManagerImpl;
import org.netbeans.core.windows.options.WinSysPrefs;
import org.netbeans.core.windows.view.Controller;
import org.netbeans.core.windows.view.dnd.ZOrderManager;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Aux-Window Dialog
 *
 * @author Graeme Ingleby
 */
public class AuxWindowDialog extends JDialog implements AuxWindowComponent {

    private static final String ICON_16 = "org/netbeans/core/startup/frame.gif"; // NOI18N
    private static final String ICON_32 = "org/netbeans/core/startup/frame32.gif"; // NOI18N
    private static final String ICON_48 = "org/netbeans/core/startup/frame48.gif"; // NOI18N

    static void initFrameIcons(Window f) {
        List<Image> currentIcons = f.getIconImages();
        if (!currentIcons.isEmpty()) {
            return; //do not override icons if they have been already provided elsewhere (JDev)
        }
        f.setIconImages(Arrays.asList(
                ImageUtilities.loadImage(ICON_16, true),
                ImageUtilities.loadImage(ICON_32, true),
                ImageUtilities.loadImage(ICON_48, true)));
    }

    private Controller controller;

    private AuxWindowImpl window;

    private Component desktop;

    private WindowSnapper snapper;
    private boolean ignoreMovedEvents = false;

    public AuxWindowDialog(AuxWindowImpl window, Rectangle bounds, Controller controller) {
        super(WindowManager.getDefault().getMainWindow(), window.getName());
        setName(window.getName());
        setBounds(bounds);

        this.window = window;
        this.controller = controller;

        // Automatically register with window tracker
        AuxWindowTracker.getInstance().addWindow(window, this);

        // initialize frame
        initFrameIcons(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Not a type - we're going to use the main window title!
        String title = NbBundle.getMessage(MainWindow.class, "CTL_MainWindow_Title_No_Project", window.getName()); //NOI18N
        if (!title.isEmpty()) {
            setTitle(title);
        }

        // To be able to activate on mouse click.
        enableEvents(java.awt.AWTEvent.MOUSE_EVENT_MASK);

        ZOrderManager.getInstance().attachWindow(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
               WindowManagerImpl wm = WindowManagerImpl.getInstance();
               wm.setActiveMode(wm.getActiveMode(getAuxWindow()));
            }

            @Override
            public void windowClosing(WindowEvent e) {
                AuxWindowDialog.this.controller.userClosingAuxWindow(AuxWindowDialog.this.window);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                ZOrderManager.getInstance().detachWindow(AuxWindowDialog.this);
            }

        });

        try {
            snapper = new WindowSnapper();
        } catch (AWTException e) {
            snapper = null;
            Logger.getLogger(AuxWindowDialog.class.getName()).log(Level.INFO, null, e);
        }
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent ce) {
                if (ignoreMovedEvents || null == snapper
                        || !WinSysPrefs.HANDLER.getBoolean(WinSysPrefs.SNAPPING, true)) {
                    return;
                }

                snapWindow();

                snapper.cursorMoved();
            }
        });

    }

    private void snapWindow() {
        Rectangle myBounds = getBounds();

        WindowManagerImpl wm = WindowManagerImpl.getInstance();
        Set<? extends ModeImpl> modes = wm.getModes();
        for (ModeImpl m : modes) {
//            if (m.getState() != Constants.MODE_STATE_SEPARATED) {
//                continue;
//            }
            TopComponent tc = m.getSelectedTopComponent();
            if (null == tc) {
                continue;
            }
            Window w = SwingUtilities.getWindowAncestor(tc);
            if (w == AuxWindowDialog.this) {
                continue;
            }
            if(!(w instanceof AuxWindowComponent)) {
                continue;
            }
            Rectangle targetBounds = w.getBounds();
            if (snapper.snapTo(myBounds, targetBounds)) {
                return;
            }
        }

        if (WinSysPrefs.HANDLER.getBoolean(WinSysPrefs.SNAPPING_SCREENEDGES, true)) {
            snapper.snapToScreenEdges(myBounds);
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name); //To change body of generated methods, choose Tools | Templates.

        // Not a typeo - we're going to use the main window title!
        String title = NbBundle.getMessage(MainWindow.class, "CTL_MainWindow_Title_No_Project", name); //NOI18N
        if (!title.isEmpty()) {
            setTitle(title);
        }
    }

    public Component getDesktopComponent() {
        return desktop;
    }

    public void setDesktop(Component desktop) {
        if (this.desktop != desktop) {
            if (this.desktop != null) {
                getContentPane().remove(this.desktop);
            }
            this.desktop = desktop;
            getContentPane().add(desktop, BorderLayout.CENTER);
        }
    }

    @Override
    public AuxWindowImpl getAuxWindow() {
        return window;
    }

    public Controller getController() {
        return controller;
    }
}
