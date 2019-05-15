package org.netbeans.core.windows.view.ui;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentListener;
import javax.swing.RootPaneContainer;
import org.netbeans.core.windows.view.Controller;
import org.openide.windows.AuxWindow;

/**
 * A tag used to identify AuxWindow UI components 
 *      Implementation: 
 *          AuxWindowFrame based on JFrame
 *          AuxWindowDialog based on JDialog
 * 
 * @author Graeme Ingleby
 */
public interface AuxWindowComponent extends RootPaneContainer  {
    /**
     * 
     * @return The AuxWindow model object associated with this UI Component
     */
    public AuxWindow getAuxWindow();
    
    // The methods below are concepts from other core.windows classses and were
    // added to this interface to provide easy access without having to cast
    
    /**
     * @return The desktop component embedded in this UI Component
     */
    public Component getDesktopComponent();
    
    /**
     * Set the desktop component embedded in this UI Component
     * 
     * @param desktop 
     */
    public void setDesktop(Component desktop);
    
    /**
     * @return The View Controller for this UI Component
     */
    public Controller getController();
    
    /**
     * Change the visibility of this UI Component
     * 
     * @param isVisible 
     */
    public void setVisible(boolean isVisible);
    
    /**
     * @return If this UI Component is visible
     */
    public boolean isVisible();
    
    /**
     * Add a Component listener to this UI Component
     * @param listener 
     */
    public void addComponentListener(ComponentListener listener);
    
    /**
     * Remove a Component listener from this UI Component
     * @param listener 
     */
    public void removeComponentListener(ComponentListener listener);
    
    /**
     * Set the bounds of this UI Component
     * @param bounds 
     */
    public void setBounds(Rectangle bounds);
}