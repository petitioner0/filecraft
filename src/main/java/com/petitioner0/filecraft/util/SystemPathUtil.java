package com.petitioner0.filecraft.util;


import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class SystemPathUtil {

    /**
     * 获取系统的第一个根目录路径
     * @return 根目录路径字符串，如果没有则返回 null
     */
    public static String getFirstRootPath() {
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            return roots[0].getAbsolutePath();
        }
        return null;
    }

    /**
     * 获取系统的第一个根目录显示名（更友好，比如 Windows 下显示“本地磁盘 (C:)”）
     * @return 显示名，如果没有则返回 null
     */
    public static String getFirstRootDisplayName() {
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            FileSystemView fsv = FileSystemView.getFileSystemView();
            return fsv.getSystemDisplayName(roots[0]);
        }
        return null;
    }
}