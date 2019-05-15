package org.openide.windows;

/**
 * A ServiceProvider interface to determine if a TopComponent is an Editor or a View
 * 
 * @author Graeme Ingleby
 */
public interface EditorSelector {
    /**
     * 
     * @param tc
     * @return true if the supplied TopComponent is an Editor
     */
    public boolean isEditor(TopComponent tc);
}
