## _NOTE:_ A newer Apache NetBeans Window Manager project can be found here: 
https://github.com/GIngleby16/netbeans

# NetBeansDND
NetBeans Window Manager with support for splitting floating frames.  This project is based on the NetBeans 8.2 code base.

see: <http://gingleby.com/2017/11/08/a-customized-netbeans-window-manager-drag-drop-top-components-in-floating-separated-windows/>

### core.windows:
Updated to support AuxWindows.  ModesSubModel updated to associate a Mode to a window.  ZOrder support corrected.

### o.n.swing.tabcontrol
Updated to support icons in View modes.  Known rendering issue (label placement) on OSX.

### openide.windows
Added AuxWindow support

### TestingDND
NetBeans project consisting of two modules:

WindowManagerSpy - displays a debug window that allows you to view internal state of various Window Manager classes (ModesSubModel, ViewHierarchy, etc).

### TestingDNDModule
Displays a test application with various TopLevel components that can be used for testing drag/drop functionality and AuxWindow creation.


### NOTE:
Some methods may have had their visiblity changed to public for debugging purposes - these will need to be changed back once the code is working correctly.

This project was based on the NetBeans 8.2 release.  
