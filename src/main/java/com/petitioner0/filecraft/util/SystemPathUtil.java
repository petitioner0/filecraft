package com.petitioner0.filecraft.util;


import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class SystemPathUtil {

    public static String getFirstRootPath() {
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            return roots[0].getAbsolutePath();
        }
        return null;
    }


    public static String getFirstRootDisplayName() {
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            FileSystemView fsv = FileSystemView.getFileSystemView();
            return fsv.getSystemDisplayName(roots[0]);
        }
        return null;
    }
}