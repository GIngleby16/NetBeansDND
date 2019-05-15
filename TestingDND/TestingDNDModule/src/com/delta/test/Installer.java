/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.delta.test;

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.JLabel;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.modules.ModuleInstall;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import static org.openide.windows.TopComponent.PERSISTENCE_ONLY_OPENED;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {                
                // If first launch - add modes, otherewise they will be reloaded from .wswmgr
                
                String[] names = {"One", "Two", "Three", "Four", "Five", "Six"};
                for (String name : names) {
                    if (WindowManager.getDefault().findTopComponent(name) != null) {
                        return; // no further processing
                    }
                }

                // Open some real editors
                
//                try {
//                    File f = new File("U://LICENSE.txt");
//                    FileObject fo = FileUtil.toFileObject(f);
//                    DataObject d = DataObject.find(fo);
//                    EditorCookie ec = (EditorCookie)d.getCookie(EditorCookie.class);
//                    ec.open();
//                } catch(Throwable t) {
//                    t.printStackTrace();
//                }
//
//                try {
//                    File f = new File("U:/DepartureTableRenderer.java");
//                    FileObject fo = FileUtil.toFileObject(f);
//                    DataObject d = DataObject.find(fo);
//                    EditorCookie ec = (EditorCookie)d.getCookie(EditorCookie.class);
//                    ec.open();
//                } catch(Throwable t) {
//                    t.printStackTrace();
//                }
//
//                try {
//                    File f = new File("U:/AirportData.json");
//                    FileObject fo = FileUtil.toFileObject(f);
//                    DataObject d = DataObject.find(fo);
//                    EditorCookie ec = (EditorCookie)d.getCookie(EditorCookie.class);
//                    ec.open();
//                } catch(Throwable t) {
//                    t.printStackTrace();
//                }
//
                // Open One Two Three etc
                for (String name : names) {
                    final String fName = name;
                    TopComponent tc1 = new TopComponent();
                    tc1.setLayout(new BorderLayout());
                    tc1.add(new JLabel(name), BorderLayout.CENTER);
                    tc1.setName(name);
                    tc1.setDisplayName(name);
                    tc1.open();
                }
//                WindowManager.getDefault().getMainWindow().setBounds(100, 100, 600, 240);
//                
//                
//                
//                Mode outputMode = WindowManager.getDefault().findMode("output");
//
//                TopComponent ctc2 = new TopComponent();
//                ctc2.setName("Ubon");
//                ctc2.setDisplayName("Ubon");
////                outputMode.dockInto(ctc2);
//                ctc2.open();

                // Test creating a new window
//                AuxWindow window = WindowManager.getDefault().createAuxWindow("MyWindow", new Rectangle(800, 200, 320, 240));
//                Mode em = WindowManager.getDefault().findMode("MyWindow_editor");
//                TopComponent tc1 = new TopComponent();
//                tc1.setName("TC1");
//                tc1.setDisplayName("TC1");
//                em.dockInto(tc1);
//                tc1.open();
            }
        });
    }
}
