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
package org.netbeans.core.windows.model;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.netbeans.core.windows.Constants;
import org.netbeans.core.windows.ModeImpl;
import org.netbeans.core.windows.ModeStructureSnapshot;
import org.netbeans.core.windows.SplitConstraint;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import org.netbeans.core.windows.AuxWindowImpl;
import org.netbeans.core.windows.AuxWindowStructureSnapshot.AuxWindowSnapshot;
import org.netbeans.core.windows.ModeStructureSnapshot.WindowModeStructureSnapshot;
import org.netbeans.core.windows.Switches;
import org.netbeans.core.windows.WindowManagerImpl;
import org.openide.windows.TopComponent;

/**
 * Sub-model which keeps modes data strucute, i.e. as split and also separate
 * ones. Note, it keeps editor ones in another field of split model, for better
 * manipulation. See UI spec about editor area and editor/view component types.
 * Note: this instances aren't thread safe, thus they has to be properly
 * synchronized. The client has to synchronize it. (see in DefaultModel, where
 * it is only place supposed to be used.)
 *
 * @author Peter Zavadsky
 */
final class ModesSubModel {

    /**
     * Associated parent model.
     */
    private final Model parentModel;

    /**
     * Set of modes.
     */
    private final Set<ModeImpl> modes = new HashSet<ModeImpl>(10);

    /**
     * Represents split model of modes, also contains special editor area.
     */
    // private final EditorSplitSubModel editorSplitSubModel;
    private final Map<AuxWindowImpl, EditorSplitSubModel> editorSplitSubModel;
    // fast conversion of mode to window 
    private final Map<ModeImpl, AuxWindowImpl> mode2window = new WeakHashMap<ModeImpl, AuxWindowImpl>(4);

    /**
     * Sliding modes model, <ModeImpl, String> mapping of mode and side of
     * presence
     */
    private final HashMap<AuxWindowImpl, HashMap<ModeImpl, String>> slidingModes2Sides = new HashMap<AuxWindowImpl, HashMap<ModeImpl, String>>(5);   //TODO gwi: This should be OK, modes are globally unique
    private final HashMap<AuxWindowImpl, HashMap<String, ModeImpl>> slidingSides2Modes = new HashMap<AuxWindowImpl, HashMap<String, ModeImpl>>(5);   //TODO gwi: This needs to be at window level

    /**
     * Active mode.
     */
    private ModeImpl activeMode;
    /**
     * Last editor active mode.
     */
    private ModeImpl lastActiveEditorMode;
    /**
     * Maximized mode.
     */
    private WeakHashMap<AuxWindowImpl, ModeImpl> editorMaximizedMode = new WeakHashMap<AuxWindowImpl, ModeImpl>();
    private WeakHashMap<AuxWindowImpl, ModeImpl> viewMaximizedMode = new WeakHashMap<AuxWindowImpl, ModeImpl>();

    // (sliding side + TopComponent ID) -> size in pixels (width or height depending on the sliding side)
    private final Map<String, Integer> slideInSizes = new HashMap<String, Integer>(15);   //TODO gwi: This should be OK, as IDs will be globally unique

    /**
     * Creates a new instance of ModesModel
     */
    public ModesSubModel(Model parentModel) {
        this.parentModel = parentModel;        
        
        slidingSides2Modes.put(null, new HashMap<String, ModeImpl>(5));
        
        //this.editorSplitSubModel = new EditorSplitSubModel(parentModel, new SplitSubModel(parentModel));
        // need one editor split sub model per window, null is NbMainWindow
        this.editorSplitSubModel = new HashMap<>(); //gwi
        this.editorSplitSubModel.put(null, new EditorSplitSubModel(parentModel, new SplitSubModel(parentModel))); // gwi
    }

    public void setEditorAreaConstraints(AuxWindowImpl window, SplitConstraint[] editorAreaConstraints) {
        editorSplitSubModel.get(window).setEditorNodeConstraints(editorAreaConstraints);
    }

    public SplitConstraint[] getModelElementConstraints(AuxWindowImpl window, ModelElement element) {
        return editorSplitSubModel.get(window).getModelElementConstraints(element);
    }

    public SplitConstraint[] getEditorAreaConstraints(AuxWindowImpl window) {
        return editorSplitSubModel.get(window).getEditorNodeConstraints();
    }

    public SplitConstraint[] getModeConstraints(AuxWindowImpl window, ModeImpl mode) {
        if(editorSplitSubModel.get(window) == null) {            
            System.out.println("WARNING USING A SPLIT MODEL FOR A WINDOW THAT HAS BEED DESTROYED!");
            return new SplitConstraint[0];
        }
        if (mode.getKind() == Constants.MODE_KIND_EDITOR) {
            return editorSplitSubModel.get(window).getEditorArea().getModeConstraints(mode);
        } else {
            return editorSplitSubModel.get(window).getModeConstraints(mode);
        }
    }

    /**
     * Find the side (LEFT/RIGHT/BOTTOM) where the TopComponent from the given
     * mode should slide to.
     *
     * @param mode Mode
     * @return The slide side for TopComponents from the given mode.
     */
    public String getSlideSideForMode(AuxWindowImpl window, ModeImpl mode) {
        return editorSplitSubModel.get(window).getSlideSideForMode(mode);
    }

    public String getSlidingModeConstraints(AuxWindowImpl window, ModeImpl mode) {
        return slidingModes2Sides.get(window).get(mode);
    }

    public ModeImpl getSlidingMode(AuxWindowImpl window, String side) {
        HashMap<String, ModeImpl> hm = slidingSides2Modes.get(window);
                
        if(hm == null)
            return null;
        return hm.get(side); 
    }

    public Set<ModeImpl> getSlidingModes(AuxWindowImpl window) {
        HashSet<ModeImpl> modes = new HashSet<ModeImpl>();
        for(ModeImpl mode: slidingModes2Sides.get(window).keySet()) {
            if(getModesForWindow(window) == window) {
            mode2window.put(mode, window);
            }
        }
        return Collections.unmodifiableSet(modes);
    }

    // MODIFIED SIGNATURE
    public boolean addMode(AuxWindowImpl window, ModeImpl mode, SplitConstraint[] constraints) {
        if (modes.contains(mode)) {
            return false;
        }

        boolean result;
        if (mode.getKind() == Constants.MODE_KIND_EDITOR
                && (mode.getState() == Constants.MODE_STATE_JOINED || mode.getState() == Constants.MODE_STATE_AUX)) {
            result = editorSplitSubModel.get(window).getEditorArea().addMode(mode, constraints);
        } else {
            result = editorSplitSubModel.get(window).addMode(mode, constraints);
        }

        if (result) {
            modes.add(mode);
            mode2window.put(mode, window);
        }
        return result;
    }

    // XXX
    public boolean addModeToSide(AuxWindowImpl window, ModeImpl mode, ModeImpl attachMode, String side) {
        if (modes.contains(mode)) {
            return false;
        }

        boolean result;
        // XXX PENDING
        if (mode.getKind() == Constants.MODE_KIND_EDITOR) {
            result = editorSplitSubModel.get(window).getEditorArea().addModeToSide(mode, attachMode, side);
        } else {
            result = editorSplitSubModel.get(window).addModeToSide(mode, attachMode, side);
        }

        if (result) {
            modes.add(mode);
            mode2window.put(mode, window);
        }
        return result;
    }

    // XXX
    public boolean addModeAround(AuxWindowImpl window, ModeImpl mode, String side) {
        if (modes.contains(mode)) {
            return false;
        }

        boolean result;
        // XXX PENDING
        if (mode.getKind() == Constants.MODE_KIND_EDITOR) {
            result = false;
        } else {
            result = editorSplitSubModel.get(window).addModeAround(mode, side);
        }

        if (result) {
            modes.add(mode);
            mode2window.put(mode, window);
        }
        return result;
    }

    // XXX
    public boolean addModeAroundEditor(AuxWindowImpl window, ModeImpl mode, String side) {
        if (modes.contains(mode)) {
            return false;
        }

        boolean result;
        // XXX PENDING
        if (mode.getKind() == Constants.MODE_KIND_EDITOR) {
            result = editorSplitSubModel.get(window).getEditorArea().addModeToSideRoot(mode, side);
        } else {
            result = editorSplitSubModel.get(window).addModeAroundEditor(mode, side);
        }

        if (result) {
            modes.add(mode);
            mode2window.put(mode, window);
        }
        return result;
    }

    public boolean addModeSliding(AuxWindowImpl window, ModeImpl mode, String side, Map<String, Integer> slideInSizes) {
        if (modes.contains(mode) || (mode.getKind() != Constants.MODE_KIND_SLIDING)) {
            return false;
        }
        
        HashMap<ModeImpl, String> map = slidingModes2Sides.get(window);
        if(map == null) {
            map = new HashMap<ModeImpl, String>();
            slidingModes2Sides.put(window, map);
        }          
                
        
        map.put(mode, side);
        
        HashMap<String, ModeImpl> modeMap = slidingSides2Modes.get(window);
        if(modeMap == null) {
            modeMap = new HashMap<String, ModeImpl>();
            slidingSides2Modes.put(window, modeMap);
        }
        
        modeMap.put(side, mode);

        modes.add(mode);
        mode2window.put(mode, window);

        if (null != slideInSizes) {
            for (Iterator<String> i = slideInSizes.keySet().iterator(); i.hasNext();) {
                String tcId = i.next();
                this.slideInSizes.put(side + tcId, slideInSizes.get(tcId));
            }
        }

        return true;
    }

    public Map<String, Integer> getSlideInSizes(String side) {
        Map<String, Integer> res = new HashMap<String, Integer>(5);
        for (Iterator<String> i = slideInSizes.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            if (key.startsWith(side)) {
                String tcId = key.substring(side.length());
                Integer size = slideInSizes.get(key);
                res.put(tcId, size);
            }
        }
        return res;
    }

    public Map<TopComponent, Integer> getSlideInSizes(ModeImpl mode) {
        WindowManagerImpl wm = WindowManagerImpl.getInstance();
        TopComponent[] tcs = mode.getTopComponents();
        Map<TopComponent, Integer> res = new HashMap<TopComponent, Integer>(tcs.length);
        for (TopComponent tc : tcs) {
            String tcId = wm.findTopComponentID(tc);
            Integer size = slideInSizes.get(mode.getSide(getWindowForMode(mode)) + tcId);
            if (null != size) {
                res.put(tc, size);
            }
        }
        return res;
    }

    public void setSlideInSize(String side, TopComponent tc, int size) {
        if (null != tc && null != side) {
            String tcId = WindowManagerImpl.getInstance().findTopComponentID(tc);
            slideInSizes.put(side + tcId, new Integer(size));
        }
    }

    public boolean removeMode(ModeImpl mode, boolean destructive) {
        int kind = mode.getKind();
        if (kind == Constants.MODE_KIND_SLIDING && !destructive) {
            return true;
            // don't remove the sliding modes, to make dnd easier..
//            slidingSides2Modes.remove(side);
//            return slidingModes2Sides.remove(mode) != null;
        }
        
        modes.remove(mode);
        mode2window.remove(mode);
                        
        if (mode.equals(lastActiveEditorMode)) {
            lastActiveEditorMode = null;
        }
        AuxWindowImpl window = getWindowForMode(mode);
        if (mode.getKind() == Constants.MODE_KIND_EDITOR) {
            return editorSplitSubModel.get(window).getEditorArea().removeMode(mode);
        } else {
            return editorSplitSubModel.get(window).removeMode(mode);
        }
    }
    
    /**
     * Sets active mode.
     */
    public boolean setActiveMode(ModeImpl activeMode) {
        if (activeMode == null || modes.contains(activeMode)) {
            this.activeMode = activeMode;
            if ((activeMode != null) && (activeMode.getKind() == Constants.MODE_KIND_EDITOR)) {
                if (activeMode.getState() != Constants.MODE_STATE_SEPARATED
                        || !Switches.isOpenNewEditorsDocked()) {
                    lastActiveEditorMode = activeMode;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Gets active mode.
     */
    public ModeImpl getActiveMode() {
        return this.activeMode;
    }

    /**
     * Gets last active editor mode.
     */
    public ModeImpl getLastActiveEditorMode() {
        return this.lastActiveEditorMode;
    }

    /**
     * Sets maximized mode for editor components.
     */
    public boolean setEditorMaximizedMode(AuxWindowImpl window, ModeImpl maximizedMode) {
        if (maximizedMode == null || modes.contains(maximizedMode)) {
            this.editorMaximizedMode.put(window, maximizedMode);
            return true;
        }

        return false;
    }

    /**
     * Gets maximized mode for editor components.
     */
    public ModeImpl getEditorMaximizedMode(AuxWindowImpl window) {
        return this.editorMaximizedMode.get(window);
    }

    /**
     * Sets maximized mode for non-editor components.
     */
    public boolean setViewMaximizedMode(AuxWindowImpl window, ModeImpl maximizedMode) {
        if (maximizedMode == null || modes.contains(maximizedMode)) {
            this.viewMaximizedMode.put(window, maximizedMode);
            return true;
        }

        return false;
    }

    /**
     * Gets maximized mode for non-editor components.
     */
    public ModeImpl getViewMaximizedMode(AuxWindowImpl window) {
        return this.viewMaximizedMode.get(window);
    }

    public Set<ModeImpl> getModes() {
        return new HashSet<ModeImpl>(modes);
    }

    public void setSplitWeights(AuxWindowImpl window, ModelElement[] snapshots, double[] splitWeights) {
        editorSplitSubModel.get(window).setSplitWeights(snapshots, splitWeights);
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) // NOI18N
                + "\n" + editorSplitSubModel; // NOI18N
    }

    /////////////////////////////////////////////
    // used when creating snapshot of this model.
    
    
    public Map<AuxWindowSnapshot, WindowModeStructureSnapshot> createWindowModeStructureSnapshots() {
        HashMap<AuxWindowSnapshot, WindowModeStructureSnapshot> map = new HashMap<AuxWindowSnapshot, WindowModeStructureSnapshot>();
        // for each window (null will be main window)
        for (AuxWindowImpl window : editorSplitSubModel.keySet()) {
            WindowModeStructureSnapshot windowSnapshot = new WindowModeStructureSnapshot(editorSplitSubModel.get(window).createSplitSnapshot(),  createSlidingModeSnapshots(window));        
            
//            StringBuffer buffer = new StringBuffer();
//            buffer.append("ModesSubModel.createWindowModeStructureSnapshots\n");
//            buffer.append(window == null?"NbMainWindow":window.getName());
//            buffer.append("\n");
//            for(ModeStructureSnapshot.SlidingModeSnapshot i: windowSnapshot.getSlidingModeSnapshots())
//                buffer.append(i.getName() +" " + i.getSide() + " has " + i.getOpenedTopComponents().length + "\n");
//            JOptionPane.showMessageDialog(null, buffer.toString());
            map.put(new AuxWindowSnapshot(window), windowSnapshot); 
        }
        return map;
    }
    
//    public ModeStructureSnapshot.ElementSnapshot createSplitSnapshot(AuxWindowImpl window) {
//        return editorSplitSubModel.get(window).createSplitSnapshot();
//    }

    /**
     * Set of mode element snapshots.
     */
    public Set<ModeStructureSnapshot.ModeSnapshot> createSeparateModeSnapshots(AuxWindowImpl window) {
        Set<ModeStructureSnapshot.ModeSnapshot> s
                = new HashSet<ModeStructureSnapshot.ModeSnapshot>();

        s.addAll(editorSplitSubModel.get(window).createSeparateSnapshots());

        return s;
    }

    public Set<ModeStructureSnapshot.SlidingModeSnapshot> createSlidingModeSnapshots(AuxWindowImpl window) {
        Set<ModeStructureSnapshot.SlidingModeSnapshot> result = new HashSet<ModeStructureSnapshot.SlidingModeSnapshot>();
        HashMap<ModeImpl, String> map = slidingModes2Sides.get(window);
        if(map != null) {
            for (Map.Entry<ModeImpl, String> curEntry : map.entrySet()) {
                final ModeImpl key = curEntry.getKey();

                if(getWindowForMode(key) != window)
                    continue;

                AbstractMap<TopComponent, Integer> lazy = new AbstractMap<TopComponent, Integer>() {
                    Map<TopComponent, Integer> delegate;

                    @Override
                    public Set<Entry<TopComponent, Integer>> entrySet() {
                        if (delegate == null) {
                            delegate = getSlideInSizes(key);
                        }
                        return delegate.entrySet();
                    }
                };

                result.add(new ModeStructureSnapshot.SlidingModeSnapshot(
                        curEntry.getKey(), curEntry.getValue(), lazy
                ));
            }
        }
        return result;
        
    }

    public void dock(ModeImpl prevMode, ModeImpl floatingMode) {
        AuxWindowImpl window = getWindowForMode(prevMode);
        assert modes.contains(prevMode);
        assert modes.contains(floatingMode);
        SplitConstraint[] constraints = getModeConstraints(window, prevMode);
        boolean editorMode = prevMode.getKind() == Constants.MODE_KIND_EDITOR;
        if (editorMode) {
            editorSplitSubModel.get(window).getEditorArea().addMode(floatingMode, constraints);
            removeMode(prevMode, false);
            editorSplitSubModel.get(window).removeMode(floatingMode);
        } else {
            editorSplitSubModel.get(window).removeMode(floatingMode);
            editorSplitSubModel.get(window).addMode(floatingMode, constraints);
            removeMode(prevMode, false);
        }
    }

    ////////////////////////////////////////////
    //////////////////////////////
    // Controller updates >>
    public ModeImpl getModeForOriginator(ModelElement originator) {
        ModeImpl mode = EditorSplitSubModel.getModeForOriginator(originator);

        if (modes.contains(mode)) {
            return mode;
        } else {
            return null;
        }
    }

    // Controller updates <<
    ///////////////////////////////
    // NEW --------------------------------------------------------------------
    public void createAuxWindowEditorSplitSubModel(AuxWindowImpl window) {
        if (editorSplitSubModel.containsKey(window)) {
            throw new RuntimeException("Window already exists");
        }
        editorSplitSubModel.put(window, new EditorSplitSubModel(parentModel, new SplitSubModel(parentModel)));
    }

    //TODO gwi: created createAuxSplitSnaphot
    public Map<AuxWindowSnapshot, ModeStructureSnapshot.ElementSnapshot> createAuxSplitSnapshot() {
        Map<AuxWindowSnapshot, ModeStructureSnapshot.ElementSnapshot> roots = new HashMap<AuxWindowSnapshot, ModeStructureSnapshot.ElementSnapshot>();
        for (AuxWindowImpl window : editorSplitSubModel.keySet()) {
            if (window == null) {
                continue; // skip main window
            }
            EditorSplitSubModel editorSplitModel = editorSplitSubModel.get(window);
            roots.put(new AuxWindowSnapshot(window), editorSplitModel.createSplitSnapshot());
        }
        return roots;
    }
    
    public void removeAuxWindow(AuxWindowImpl window) {
        editorSplitSubModel.remove(window);
        
        Set<ModeImpl> modes = new HashSet<ModeImpl>(mode2window.keySet());
        for(ModeImpl mode: modes) {
            AuxWindowImpl win = mode2window.get(mode);
            if(win == null) 
                continue;
            if(win.equals(window)) {
                mode2window.remove(mode);
            }
        }
        
        slidingModes2Sides.remove(window);
        slidingSides2Modes.remove(window);
        
        editorMaximizedMode.remove(window);
        viewMaximizedMode.remove(window);
    }

    public Set<ModeImpl> getModesForWindow(AuxWindowImpl window) {
        Set<ModeImpl> modes4Window = new HashSet<ModeImpl>();
        // consider a multi-map for performance
        for (ModeImpl mode : modes) {
            AuxWindowImpl win = mode2window.get(mode);
//            if(win == null)
//                continue;            
            if (Objects.equals(win, window)) {
                modes4Window.add(mode);
            }
        }
        return modes4Window;
    }
    
    public AuxWindowImpl getWindowForMode(ModeImpl mode) {
        return mode2window.get(mode);
    }    
}
