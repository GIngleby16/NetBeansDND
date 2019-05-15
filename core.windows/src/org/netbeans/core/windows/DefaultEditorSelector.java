package org.netbeans.core.windows;

import org.openide.loaders.DataObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.EditorSelector;
import org.openide.windows.TopComponent;

/**
 * Default implementation for EditorSelector.
 *      Determines if the specified TopComponent is an editor
 * 
 * @author Graeme Ingleby
 */
@ServiceProvider(service=EditorSelector.class)
public class DefaultEditorSelector implements EditorSelector {

    @Override
    public boolean isEditor(TopComponent tc) {
        return (tc.getLookup().lookup(DataObject.class) != null);
    }    
}
