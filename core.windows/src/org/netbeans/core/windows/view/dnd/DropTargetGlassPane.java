/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.core.windows.view.dnd;


import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import org.netbeans.core.windows.Constants;
import org.netbeans.core.windows.Debug;
import org.netbeans.core.windows.view.Controller;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.geom.AffineTransform;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Set;


/**
 * Glass pane which is used for <code>DefaultContainerImpl</code>
 * as a component associated with <code>DropTarget</code> to be able
 * to paint 'drag under' indications for that container. 
 *
 *
 * Modified Graeme to correct drop indication issue in two windows
 * 
 * @author  Peter Zavadsky
 *
 * @see java.awt.dnd.DropTarget
 * @see org.netbeans.core.windows.DefaultContainerImpl
 */
public final class DropTargetGlassPane extends JPanel implements DropTargetListener {

    // XXX PENDING
    private final Observer observer;
    // XXX PENDING
    private final Informer informer;
    
    private WindowDnDManager windowDragAndDrop;
    
    /** Current location of cursor in over the glass pane,
     * or <code>null</code> in the case there it is not above
     * this component currently. */
    private Point location;
    
    /** <code>TopComponentDroppable</code> used in paint to get indication
     * rectangle. */
    private TopComponentDroppable droppable;

    private Reference<Autoscroll> lastAutoscroll = null;
    
    /** Debugging flag. */
    private static final boolean DEBUG = Debug.isLoggable(DropTargetGlassPane.class);

    

    /** Creates non initialized <code>DropTargetGlassPane</code>. */
    public DropTargetGlassPane(WindowDnDManager wdnd) {
        this.observer = wdnd;
        this.informer = wdnd;
        windowDragAndDrop = wdnd;
        
        setOpaque(false);
    }

    
    /** Called when started drag operation, to save the old visibility state. */
    public void initialize() {
        if(isVisible()) {
            // For unselected internal frame the visibility could
            // be already set, but due to a bug is needed to revalidate it.
            revalidate();
        } else {
            setVisible(true);
        }
    }

    /** Called when finished drag operation, to reset the old visibility state. */
    public void uninitialize() {
        if(location != null) {
            // #22123. Not removed drop inidication.
            dragFinished();
        }

        setVisible(false);
        stopAutoscroll();
    }
    
    /** Called when the drag operation performed over this drop target. */
    void dragOver(Point location, TopComponentDroppable droppable) {
        this.droppable = droppable;
        setDragLocation (location);
        autoscroll( droppable, location );
    }
    
    
    private Point dragLocation = null;
    private void setDragLocation (Point p) {
        Point old = dragLocation;
        dragLocation = p;
        if (p != null && p.equals(old)) {
            return;
        } else if (p == null) {
            //XXX clear?
            return;
        }
        //#234429 - make sure we're still visible - reseting the global wait cursor hides the glass pane
        setVisible( true );
        
        if (droppable != null) {
            Rectangle repaintRectangle = null;
            if( null != currentDropIndication ) {
                repaintRectangle = currentDropIndication.getBounds();
                repaintRectangle = SwingUtilities.convertRectangle(componentUnderCursor, repaintRectangle, this );
                
                if( null != currentPainter ) {
                    Rectangle rect = currentPainter.getPaintArea();
                    if( null != rect )
                        repaintRectangle.add(rect);
                }
            }
            Component c = droppable.getDropComponent();
            
            Shape s = droppable.getIndicationForLocation (
                SwingUtilities.convertPoint(this, p, c));
            if( null != s && s.equals( currentDropIndication ) ) {
                return;
            }
            
            if (droppable instanceof EnhancedDragPainter) {
                currentPainter = (EnhancedDragPainter)droppable;
            } else {
                currentPainter = null;
            }
            if(tp != null) {
                // Fix bug where previous drop indication was in another glass pane that missed an exit? (two drop indications visible at same time)
                DropTargetGlassPane lgp = tp.get();
                if(lgp != null && lgp != this) { 
                    lgp.clearIndications();
                }
            }            
            currentDropIndication = s;
            componentUnderCursor = c; 
            tp = new WeakReference<>(this);
            if( null != currentDropIndication ) {
                Rectangle rect = currentDropIndication.getBounds();
                rect = SwingUtilities.convertRectangle(c, rect, this );
                if( null != repaintRectangle )
                    repaintRectangle.add( rect );
                else
                    repaintRectangle = rect;
                
                if( null != currentPainter ) {
                    rect = currentPainter.getPaintArea();
                    if( null != rect )
                        repaintRectangle.add( rect );
                }
            }
            if( null != repaintRectangle ) {
                repaintRectangle.grow(2, 2);
                repaint( repaintRectangle );
            }
        } else {
            if( null != currentDropIndication ) {
                Rectangle repaintRect = currentDropIndication.getBounds();
                currentDropIndication = null;
                if( null != currentPainter ) {
                    Rectangle rect = currentPainter.getPaintArea();
                    if( null != rect )
                        repaintRect = repaintRect.union( rect );
                    currentPainter = null;
                }
                repaint( repaintRect );
            }
        }
        
    }
    

    
    /** Called when the drag operation exited from this drop target. */
    private void dragExited() {
        clear();
    }
    
    /** Hacks the problem when exiting of drop target, sometimes the framework
     * "forgets" to send drag exit event (when moved from the drop target too
     * quickly??) thus the indication rectangle remains visible. Used to fix
     * this problem. */
    public void clearIndications() {
        currentDropIndication = null;
        currentPainter = null;
        componentUnderCursor = null;
        repaint();
        clear();
    }

    /** Called when changed drag action. */
    private void dragActionChanged(Point location) {
        setDragLocation(location);
    }

    /** Called when drag operation finished. */
    private void dragFinished() {
        clear();
    }
    
    /** Clears glass pane. */
    private void clear() {
        stopAutoscroll();
        this.droppable = null;
        
        setDragLocation(null);
    }

    private Shape currentDropIndication;
    private EnhancedDragPainter currentPainter;
    private Component componentUnderCursor;
    private static WeakReference<DropTargetGlassPane> tp; // Graeme - remember last GP that paints drop indication, repaint it when drop indication cleared
    
    @Override
    public void paint(Graphics g) {
        if( null != currentDropIndication ) {
            Graphics2D g2d = (Graphics2D)g.create();
            
            if( null != currentPainter )
                currentPainter.additionalDragPaint(g2d);
            
            Color c = UIManager.getColor("Panel.dropTargetGlassPane");
            if (c == null) {
                c = Color.red;
            }
            g2d.setColor(c);        	
	
            Point p = new Point (0,0);

            p = SwingUtilities.convertPoint(componentUnderCursor, p, 
                this);
            AffineTransform at = AffineTransform.getTranslateInstance(p.x, p.y);
            g2d.transform(at);
            
            g2d.setStroke(getIndicationStroke());
            g2d.setPaint(getIndicationPaint());
            Color fillColor = Constants.SWITCH_DROP_INDICATION_FADE ? FILL_COLOR : null; 
            g2d.draw(currentDropIndication);
            if( null != fillColor )
                g2d.fill( currentDropIndication );
            g2d.dispose();
        }
    }

    private TexturePaint texturePaint;
    private int modeKind = -1;
    private TexturePaint getIndicationPaint() {
        if (droppable != null && droppable.getKind() != modeKind) {
            BufferedImage image = new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            Color c = UIManager.getColor("Panel.dropTargetGlassPane");
            boolean isModeMixing = (droppable.getKind() == Constants.MODE_KIND_EDITOR
                        && windowDragAndDrop.getStartingTransfer().getKind() != Constants.MODE_KIND_EDITOR) ||
                        (droppable.getKind() != Constants.MODE_KIND_EDITOR
                        && windowDragAndDrop.getStartingTransfer().getKind() == Constants.MODE_KIND_EDITOR);
            if (c == null) {
                c = new Color(255, 90, 0);
            }
            if( isModeMixing ) {
                g2.setColor(c);
                g2.fillRect(0,0,1,1);
                g2.fillRect(1,1,1,1);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g2.fillRect(1,0,1,1);
                g2.fillRect(0,1,1,1);
            } else {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                g2.fillRect( 0, 0, 2, 2);
            }
            texturePaint = new TexturePaint(image, new Rectangle(0,0,2,2));
            modeKind = droppable.getKind();
        }
        return texturePaint;
    }
    
    private Stroke stroke;
    private Stroke getIndicationStroke() {
        if( null == stroke )
            stroke = new BasicStroke(3);
        return stroke;
    }        
    
    // PENDING Take the color from UI Defaults
    private static final Color FILL_COLOR = new Color( 200, 200, 200, 120 );
    
    
    // >> DropTargetListener implementation >>
    /** Implements <code>DropTargetListener</code> method.
     * accepts/rejects the drag operation if move or copy operation
     * is specified. */
    public void dragEnter(DropTargetDragEvent evt) {
        if(DEBUG) {
            debugLog(""); // NOI18N
            debugLog("dragEnter"); // NO18N
        }
        
        int dropAction = evt.getDropAction();
        // Mask action NONE to MOVE one.
        if(dropAction == DnDConstants.ACTION_NONE) {
            dropAction = DnDConstants.ACTION_MOVE;
        }
        
        if((dropAction & DnDConstants.ACTION_COPY_OR_MOVE) > 0) {
            evt.acceptDrag(dropAction);
        } else {
            evt.rejectDrag();
        }
    }

    /** Implements <code>DropTargetListener</code> method.
     * Unsets the glass pane to show 'drag under' gestures. */
    public void dragExit(DropTargetEvent evt) {
        if(DEBUG) {
            debugLog(""); // NOI18N
            debugLog("dragExit"); // NO18N
        }
        
        Component c = evt.getDropTargetContext().getComponent();
        if(c == this) {
            this.dragExited();
            stopAutoscroll();
        }
    }
    
    /** Implements <code>DropTargetListener</code> method.
     * Informs the glass pane about the location of dragged cursor above
     * the component. */
    public void dragOver(DropTargetDragEvent evt) {
        if(DEBUG) {
            debugLog(""); // NOI18N
            debugLog("dragOver"); // NOI18N
        }
        
        // XXX Eliminate bug, see dragExitedHack.
        observer.setLastDropTarget(this);
    }

    void autoscroll( TopComponentDroppable droppable, Point location ) {
        Component c = droppable.getDropComponent();
        location = SwingUtilities.convertPoint( this, location, c );
        Component child = SwingUtilities.getDeepestComponentAt( c, location.x, location.y );
        Autoscroll as;
        if( child instanceof Autoscroll ) {
            as = ( Autoscroll ) child;
        } else {
            as = ( Autoscroll ) SwingUtilities.getAncestorOfClass( Autoscroll.class, child );
        }
        Autoscroll prev = null == lastAutoscroll ? null : lastAutoscroll.get();
        if( null != prev && prev != as ) {
            prev.autoscroll( new Point(Integer.MIN_VALUE, Integer.MIN_VALUE) );
        }
        if( as != null ) {
            as.autoscroll( location );
            lastAutoscroll = new WeakReference<Autoscroll>( as );
        } else {
            lastAutoscroll = null;
        }
    }

    void stopAutoscroll() {
        Autoscroll as = null == lastAutoscroll ? null : lastAutoscroll.get();
        lastAutoscroll = null;
        if( as != null ) {
            as.autoscroll( new Point(Integer.MIN_VALUE, Integer.MIN_VALUE) );
        }
    }

    /** Implements <code>DropTargetListener</code> method.
     * When changed the drag action accepts/rejects the drag operation
     * appropriatelly */
    public void dropActionChanged(DropTargetDragEvent evt) {
        if(DEBUG) {
            debugLog(""); // NOI18N
            debugLog("dropActionChanged"); // NOI18N
        }
        
        int dropAction = evt.getDropAction();
        boolean acceptDrag;
        
        if((dropAction == DnDConstants.ACTION_MOVE)
        || (dropAction == DnDConstants.ACTION_COPY
            && informer.isCopyOperationPossible())) {
                
            acceptDrag = true;
        } else {
            acceptDrag = false;
        }

        if(acceptDrag) {
            evt.acceptDrag(dropAction);
        } else {
            evt.rejectDrag();
        }
        
        Component c = evt.getDropTargetContext().getComponent();
        if(c == this) {
            this.dragActionChanged(acceptDrag ? evt.getLocation() : null);
        }
    }

    /** Implements <code>DropTargetListener</code> method. 
     * Perfoms the actual drop operation. */
    public void drop(DropTargetDropEvent evt) {
        if(DEBUG) {
            debugLog(""); // NOI18N
            debugLog("drop"); // NOI18N
        }
        
        // Inform glass pane about finished drag operation.
        Component c = evt.getDropTargetContext().getComponent();
        if(c == this) {
            this.dragFinished();
        }

        int dropAction = evt.getDropAction();
        if(dropAction != DnDConstants.ACTION_MOVE
        && dropAction != DnDConstants.ACTION_COPY) {
            // Not supported dnd operation.
            evt.rejectDrop();
            return;
        }
        
        // Accepts drop operation.
        evt.acceptDrop(dropAction);
        
        boolean success = false;

        try {
            Point loc = evt.getLocation();
            // Checks whetger it is in around center panel area.
            // In that case the drop will be tried later.
            // PENDING unify it.
            SwingUtilities.convertPointToScreen(loc, c);
            if(WindowDnDManager.isAroundCenterPanel(loc)) {
                return;
            }

            success = windowDragAndDrop.tryPerformDrop(
                    informer.getController(), informer.getFloatingFrames(),
                    loc, dropAction, evt.getTransferable());
        } finally {
            // Complete the drop operation.
            // XXX #21917.
            observer.setDropSuccess(success);
            evt.dropComplete(false);
            //evt.dropComplete(success);
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    windowDragAndDrop.dragFinished();
                    windowDragAndDrop.dragFinishedEx();
                }
            });
        }
    }
    // >> DropTargetListener implementation >>



    private static void debugLog(String message) {
        Debug.log(DropTargetGlassPane.class, message);
    }
    
    
    // XXX
    /** Glass pane uses this interface to inform about changes. */
    interface Observer {
        public void setDropSuccess(boolean success);
        public void setLastDropTarget(DropTargetGlassPane glassPane);
    } // End of Observer.

    // XXX
    interface Informer {
        public boolean isCopyOperationPossible();
        public Controller getController();
        public Set<Component> getFloatingFrames();
    }
    
}
