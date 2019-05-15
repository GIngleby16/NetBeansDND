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


package org.netbeans.core.windows.view;


import org.netbeans.core.windows.ModeImpl;
import org.netbeans.core.windows.ModeStructureSnapshot;
import org.netbeans.core.windows.ModeStructureSnapshot.ElementSnapshot;
import org.netbeans.core.windows.model.ModelElement;
import org.openide.windows.TopComponent;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.netbeans.core.windows.AuxWindowImpl;
import org.netbeans.core.windows.AuxWindowStructureSnapshot.AuxWindowSnapshot;


/**
 * Used to pass information of modes model state to view in nice fashion.
 * Need to figure out to which package this class belongs, temporary here.
 *
 * @author  Peter Zavadsky
 */
final class ModeStructureAccessorImpl implements ModeStructureAccessor {
    private final Map<AuxWindowAccessor, WindowModeStructureAccessor> windowModeStructureAccessors;
    private final Set<ModeAccessor> separateModeAccessors;
    

    /** Creates a new instance of ModesModelAccessorImpl. */
    public ModeStructureAccessorImpl(Map<AuxWindowAccessor, WindowModeStructureAccessor> windowModeStructureAccessors,Set<ModeAccessor> separateModeAccessors) {
        this.windowModeStructureAccessors = windowModeStructureAccessors;
        this.separateModeAccessors = separateModeAccessors;
    }

    @Override
    public Map<AuxWindowAccessor, WindowModeStructureAccessor> getWindowModeStructureAccessor() {
        return windowModeStructureAccessors;
    }
        

    @Override
    public ModeAccessor[] getSeparateModeAccessors() {
        return separateModeAccessors.toArray(new ModeAccessor[0]);
    }
    
    /** @param name name of mode */
    public ModeAccessor findModeAccessor(String name) {
        ModeAccessor ma;
        
        for(WindowModeStructureAccessor windowAccessor: windowModeStructureAccessors.values()) {        
             ma = findModeAccessorOfName(windowAccessor.getSplitRootAccessor(), name);
            if(ma != null) {
                return ma;
            }
        }
                
        for(Iterator it = separateModeAccessors.iterator(); it.hasNext(); ) {
            ma = (ModeAccessor)it.next();
            if(name.equals(ma.getName())) {
                return ma;
            }
        }

        for(WindowModeStructureAccessor windowAccessor: windowModeStructureAccessors.values()) {        
            for(Iterator it = windowAccessor.getSlidingModeAccessors().iterator(); it.hasNext(); ) {
                ma = (ModeAccessor)it.next();
                if(name.equals(ma.getName())) {
                    return ma;
                }
            }
        }
        
        return null;
    }
    
    private static ModeAccessor findModeAccessorOfName(ElementAccessor accessor, String name) {
        if(accessor instanceof ModeAccessor) {
            ModeAccessor ma = (ModeAccessor)accessor;
            if(name.equals(ma.getName())) {
                return ma;
            }
        } else if(accessor instanceof SplitAccessor) {
            SplitAccessor split = (SplitAccessor)accessor; 
            ElementAccessor[] children = split.getChildren();
            for( int i=0; i<children.length; i++ ) {
                ModeAccessor ma = findModeAccessorOfName(children[i], name);
                if(ma != null) {
                    return ma;
                }
            }
        } else if(accessor instanceof EditorAccessor) {
            EditorAccessor editorAccessor = (EditorAccessor)accessor;
            ModeAccessor ma = findModeAccessorOfName(editorAccessor.getEditorAreaAccessor(), name);
            if(ma != null) {
                return ma;
            }
        }
        
        return null;
    }
    

    /** Superclass for accessor of model element.
     * There are three types, split, mode, and editor (represents editor area) type. */
    static abstract class ElementAccessorImpl implements ElementAccessor {
        // PENDING revise
        /** Corresponding object in model (SplitNode or ModeNode for separate mode). */
        private final ModelElement originator;
        /** Corresponding snapshot. */
        private final ModeStructureSnapshot.ElementSnapshot snapshot;
        
        
        public ElementAccessorImpl(ModelElement originator, ModeStructureSnapshot.ElementSnapshot snapshot) {
            this.originator = originator;
            this.snapshot = snapshot;
        }

        /** Gets originator object. Used only in model. */
        @Override
        public final ModelElement getOriginator() {
            return originator;
        }
        
        @Override
        public final ModeStructureSnapshot.ElementSnapshot getSnapshot() {
            return snapshot;
        }
        
        @Override
        public boolean originatorEquals(ElementAccessor o) {
            if(o instanceof ElementAccessorImpl) {
                return getClass().equals(o.getClass()) // To prevent mismatch between split and mode accessor.
                                                       // Split has now originator corresponding to first child.
                    && ((ElementAccessorImpl)o).originator == originator;
            }
            return false;
        }
        
        @Override
        public String toString() {
            return super.toString() + "[originatorHash=" + (originator != null ? Integer.toHexString(originator.hashCode()) : "null") + "]"; // NOI18N
        }
    }
    
    /** */ 
    static final class SplitAccessorImpl extends ElementAccessorImpl implements SplitAccessor {
        private final int orientation;
        private final double[] splitPositions; // relative
        private final ElementAccessor[] children;
        private final double resizeWeight;
        
        public SplitAccessorImpl(ModelElement originator, ElementSnapshot snapshot,
        int orientation, double[] splitPositions,
        ElementAccessor[] children, double resizeWeight) {
            super(originator, snapshot); // It correspond to the first child model element.
            
            this.orientation = orientation;
            this.splitPositions = splitPositions;
            this.children = children;
            this.resizeWeight = resizeWeight;
        }

        @Override
        public int getOrientation() {
            return orientation;
        }
        
        @Override
        public double[] getSplitWeights() {
            return splitPositions;
        }
        
        @Override
        public ElementAccessor[] getChildren() {
            return children;
        }
        
        @Override
        public double getResizeWeight() {
            return resizeWeight;
        }
        
        @Override
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append( super.toString() );
            buffer.append( "[orientation=" + orientation );// NOI18N
            buffer.append( ", splitPosition=" ); // NOI18N
            for( int i=0; i<splitPositions.length; i++ ) {
                buffer.append( splitPositions[i] );
                if( i < splitPositions.length-1 )
                    buffer.append( " : " ); // NOI18N
            }
            buffer.append( "]" ); // NOI18N
            return buffer.toString();
        }
    }

    /** */
    static class ModeAccessorImpl extends ElementAccessorImpl implements ModeAccessor { 
        
        public ModeAccessorImpl(ModelElement originator, ModeStructureSnapshot.ModeSnapshot snapshot) {
            super(originator, snapshot);
        }
        
        private ModeStructureSnapshot.ModeSnapshot getModeSnapShot() {
            return (ModeStructureSnapshot.ModeSnapshot)getSnapshot();
        }
        
        
        @Override
        public boolean originatorEquals(ElementAccessor o) {
            if(!super.originatorEquals(o)) {
                return false;
            }
            
            // XXX Even if originators are same, they differ if their states are different.
            // Difference -> split vs. separate representations.
            ModeAccessor me = (ModeAccessor)o;
            return getState() == me.getState();
        }
        
        @Override
        public ModeImpl getMode() {
            return getModeSnapShot().getMode();
        }
        
        @Override
        public String getName() {
            return getModeSnapShot().getName();
        }

        @Override
        public int getState() {
            return getModeSnapShot().getState();
        }

        @Override
        public int getKind() {
            return getModeSnapShot().getKind();
        }

        @Override
        public Rectangle getBounds() {
            return getModeSnapShot().getBounds();
        }

        @Override
        public int getFrameState() {
            return getModeSnapShot().getFrameState();
        }

        @Override
        public TopComponent getSelectedTopComponent() {
            return getModeSnapShot().getSelectedTopComponent();
        }

        @Override
        public TopComponent[] getOpenedTopComponents() {
            return getModeSnapShot().getOpenedTopComponents();
        }
        
        @Override
        public double getResizeWeight() {
            return getModeSnapShot().getResizeWeight();
        }
        
        @Override
        public String toString() {
            return super.toString() + "[name=" + getName() + " ]"; // NOI18N
        }

    }

    /** Data accessor for sliding view */
    static final class SlidingAccessorImpl extends ModeAccessorImpl implements SlidingAccessor { 

        private final String side;
        private final Map<TopComponent,Integer> slideInSizes;
        private final AuxWindowSnapshot windowSnapshot;  
        
        public SlidingAccessorImpl(AuxWindowSnapshot windowSnapshot, ModelElement originator, 
                ModeStructureSnapshot.ModeSnapshot snapshot, 
                String side, Map<TopComponent,Integer> slideInSizes) {
            super(originator, snapshot);

            this.side = side;
            this.slideInSizes = slideInSizes;
            this.windowSnapshot = windowSnapshot;
        }
        
    
        @Override
        public String getSide() {
            return side;
        }
        
        @Override
        public Map<TopComponent,Integer> getSlideInSizes() {
            return slideInSizes;
        }
        
        @Override
        public boolean originatorEquals(ElementAccessor o) {
            if(!super.originatorEquals(o)) {
                return false;
            }
            
            // XXX Even if originators are same, they differ if their side are different.
            SlidingAccessorImpl me = (SlidingAccessorImpl)o;
            boolean isSameSide = getSide() == me.getSide();
            boolean isSameWindow = (this.windowSnapshot.getAuxWindow() == me.windowSnapshot.getAuxWindow()); // gwi had to compare window too!
            return isSameSide && isSameWindow;
        }
        
    } // end of SlidingAccessorImpl
        
    
    /** */
    static final class EditorAccessorImpl extends ElementAccessorImpl implements EditorAccessor {
        private final ElementAccessor editorAreaAccessor;
        private final double resizeWeight;
        
        public EditorAccessorImpl(ModelElement originator, ElementSnapshot snapshot,
        ElementAccessor editorAreaAccessor, double resizeWeight) {
            super(originator, snapshot);
            
            this.editorAreaAccessor = editorAreaAccessor;
            this.resizeWeight = resizeWeight;
        }
        
        @Override
        public double getResizeWeight() {
            return resizeWeight;
        }
        
        @Override
        public ElementAccessor getEditorAreaAccessor() {
            return editorAreaAccessor;
        }
        
        @Override
        public String toString() {
            return super.toString() + "\n" + editorAreaAccessor; // NOI18N
        }
    }

    
        @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();        
        sb.append("ModesAccessorImpl hashCode=" + Integer.toHexString(hashCode()) + "\n"); // NOI18N
        for(AuxWindowAccessor windowAccessor: windowModeStructureAccessors.keySet()) {
            WindowModeStructureAccessor wmsa = windowModeStructureAccessors.get(windowAccessor);
            
            AuxWindowImpl window = windowAccessor.getAuxWindow();
            
            sb.append("Window " + (window==null?"NbMainWindow":window.getName()) + "\n");            
            sb.append("SplitRoot\n");
            sb.append(dumpAccessor(wmsa.getSplitRootAccessor(), 0)); 
            sb.append("Sliding\n");
            sb.append(dumpSet(wmsa.getSlidingModeAccessors()));
        }        
        sb.append("\nSeparate Modes:"); // NOI18N
        sb.append(dumpSet(separateModeAccessors));
        return sb.toString();


//        StringBuffer sb = new StringBuffer();
//        sb.append("\nModesAccessorImpl hashCode=" + hashCode()); // NOI18N
//        sb.append("\nSplit modes:\n"); // NOI18N
//        sb.append(dumpAccessor(splitRootAccessor, 0)); 
//        sb.append("\nSeparate Modes:"); // NOI18N
//        sb.append(dumpSet(separateModeAccessors));
//        return sb.toString();
    }
    
    private static String dumpAccessor(ElementAccessor accessor, int indent) {
        StringBuffer sb = new StringBuffer();
        String indentString = createIndentString(indent);
        
        if(accessor instanceof SplitAccessor) {
            SplitAccessor splitAccessor = (SplitAccessor)accessor;
            sb.append(indentString + "split="+splitAccessor); // NOI18N
            indent++;
            ElementAccessor[] children = splitAccessor.getChildren();
            for( int i=0; i<children.length; i++ ) {
                sb.append("\n" + dumpAccessor(children[i], indent)); // NOI18N
            }
        } else if(accessor instanceof ModeAccessor) {
            sb.append(indentString + "mode=" + accessor); // NOI18N
        } else if(accessor instanceof EditorAccessor) {
            sb.append(indentString + "editor=" + accessor); // NOI18N
            sb.append(dumpAccessor(((EditorAccessor)accessor).getEditorAreaAccessor(), ++indent));
        }
        
        return sb.toString();
    }
    
    private static String createIndentString(int indent) {
        StringBuffer sb = new StringBuffer(indent);
        for(int i = 0; i < indent; i++) {
            sb.append(' ');
        }
        
        return sb.toString();
    }
    
    private static String dumpSet(Set separateModes) {
        StringBuffer sb = new StringBuffer();
        
        for(java.util.Iterator it = separateModes.iterator(); it.hasNext(); ) {
            sb.append("\nmode=" + it.next());
        }
        
        return sb.toString();
    }
    
    //TODO gwi: implemented getAuxSplitRootAccessors
//    @Override
//    public Map<AuxWindowStructureSnapshot.AuxWindowSnapshot, ElementAccessor> getAuxSplitRootAccessors() {
//        return auxSplitRootAccessors;
//    }    
}

