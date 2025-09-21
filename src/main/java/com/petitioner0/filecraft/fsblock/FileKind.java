package com.petitioner0.filecraft.fsblock;

import net.minecraft.util.StringRepresentable;

public enum FileKind implements StringRepresentable {
    FOLDER("folder"),
    TXT("txt"),
    PNG("png"),
    PDF("pdf"),
    JPG("jpg"),
    JPEG("jpeg"),
    GIF("gif"),
    MP3("mp3"),
    OGG("ogg"),
    MP4("mp4"),
    JSON("json"),
    XML("xml"),
    OTHER("other");

    private final String name;
    FileKind(String n) { this.name = n; }
    @Override public String getSerializedName() { return name; }

    public static FileKind fromExtension(String ext, boolean isDir) {
        if (isDir) return FOLDER;
        if (ext == null) return OTHER;
        String e = ext.toLowerCase();
        return switch (e) {
            case "txt" -> TXT;
            case "png" -> PNG;
            case "pdf" -> PDF;
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "gif" -> GIF;
            case "mp3" -> MP3;
            case "ogg" -> OGG;
            case "mp4" -> MP4;
            case "json" -> JSON;
            case "xml" -> XML;
            default -> OTHER;
        };
    }
}