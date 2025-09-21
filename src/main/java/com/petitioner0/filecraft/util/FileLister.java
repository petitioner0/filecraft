package com.petitioner0.filecraft.util;

import com.petitioner0.filecraft.network.s2c.DirListS2CPayload;
import java.io.File;
import java.util.*;

public class FileLister {
    /** 仅在客户端执行：读取方块实体当前路径的子目录/文件 */
    public static List<DirListS2CPayload.RequestEntry> list(String basePath, boolean isDir) {
        // 文件节点没有子项
        if (!isDir) return Collections.emptyList();
        if (basePath == null || basePath.isBlank()) return Collections.emptyList();

        File f = new File(basePath);
        if (!f.exists() || !f.isDirectory()) return Collections.emptyList();

        File[] arr = f.listFiles();
        if (arr == null || arr.length == 0) return Collections.emptyList();

        // 目录优先，再按名称排序
        Arrays.sort(arr, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        List<DirListS2CPayload.RequestEntry> out = new ArrayList<>(arr.length);
        for (File child : arr) {
            out.add(new DirListS2CPayload.RequestEntry(
                    child.getName(),
                    child.isDirectory(),
                    child.getAbsolutePath(),
                    child.isDirectory() ? "" : extOf(child.getName())
            ));
        }
        return out;
    }

    private static String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }
}