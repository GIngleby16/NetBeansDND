package org.openide.windows;

/**
 * A service that determines what type of AuxWindow to create based on the first TopComponent being added.
 *      An AuxWindow can be either an AuxWindowFrame (typically used for editors)
 *      or an AuxWindowDialog (typically used for views)
 * @author D9255343
 */
public interface AuxWindowSelector {
    /**
     * Determine (based on TopComponent) if an AuxWindow should be Frame or Dialog
     * 
     * @param tc
     * @return 
     */
    public Boolean isDialogRequested(TopComponent tc);
}
