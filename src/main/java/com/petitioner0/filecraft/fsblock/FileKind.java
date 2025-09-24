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
    BMP("bmp"),
    TIFF("tiff"),
    TIF("tif"),
    WEBP("webp"),
    SVG("svg"),
    ICO("ico"),
    MP3("mp3"),
    OGG("ogg"),
    WAV("wav"),
    FLAC("flac"),
    AAC("aac"),
    WMA("wma"),
    M4A("m4a"),
    MP4("mp4"),
    AVI("avi"),
    MKV("mkv"),
    MOV("mov"),
    FLV("flv"),
    WMV("wmv"),
    WEBM("webm"),
    JSON("json"),
    XML("xml"),
    MD("md"),
    CSV("csv"),
    TSV("tsv"),
    YML("yml"),
    YAML("yaml"),
    INI("ini"),
    LOG("log"),
    CONF("conf"),
    JAVA("java"),
    C("c"),
    CPP("cpp"),
    H("h"),
    CS("cs"),
    PY("py"),
    JS("js"),
    TS("ts"),
    HTML("html"),
    HTM("htm"),
    CSS("css"),
    PHP("php"),
    RB("rb"),
    GO("go"),
    RS("rs"),
    SWIFT("swift"),
    KT("kt"),
    APK("apk"),
    DMG("dmg"),
    JAR("jar"),
    BIN("bin"),
    EXE("exe"),
    SH("sh"),
    BAT("bat"),
    CMD("cmd"),
    PS1("ps1"),
    APP("app"),
    DEB("deb"),
    RPM("rpm"),
    MSI("msi"),
    DOC("doc"),
    DOCX("docx"),
    PPT("ppt"),
    PPTX("pptx"),
    PPTM("pptm"),
    XLS("xls"),
    XLSX("xlsx"),
    XLSM("xlsm"),
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
            case "bmp" -> BMP;
            case "tiff" -> TIFF;
            case "tif" -> TIF;
            case "webp" -> WEBP;
            case "svg" -> SVG;
            case "ico" -> ICO;
            case "mp3" -> MP3;
            case "ogg" -> OGG;
            case "wav" -> WAV;
            case "flac" -> FLAC;
            case "aac" -> AAC;
            case "wma" -> WMA;
            case "m4a" -> M4A;
            case "mp4" -> MP4;
            case "avi" -> AVI;
            case "mkv" -> MKV;
            case "mov" -> MOV;
            case "flv" -> FLV;
            case "wmv" -> WMV;
            case "webm" -> WEBM;
            case "json" -> JSON;
            case "xml" -> XML;
            case "md" -> MD;
            case "csv" -> CSV;
            case "tsv" -> TSV;
            case "yml" -> YML;
            case "yaml" -> YAML;
            case "ini" -> INI;
            case "log" -> LOG;
            case "conf" -> CONF;
            case "java" -> JAVA;
            case "c" -> C;
            case "cpp" -> CPP;
            case "h" -> H;
            case "cs" -> CS;
            case "py" -> PY;
            case "js" -> JS;
            case "ts" -> TS;
            case "html" -> HTML;
            case "htm" -> HTM;
            case "css" -> CSS;
            case "php" -> PHP;
            case "rb" -> RB;
            case "go" -> GO;
            case "rs" -> RS;
            case "swift" -> SWIFT;
            case "kt" -> KT;
            case "apk" -> APK;
            case "dmg" -> DMG;
            case "jar" -> JAR;
            case "bin" -> BIN;
            case "exe" -> EXE;
            case "sh" -> SH;
            case "bat" -> BAT;
            case "cmd" -> CMD;
            case "ps1" -> PS1;
            case "app" -> APP;
            case "deb" -> DEB;
            case "rpm" -> RPM;
            case "msi" -> MSI;
            case "doc" -> DOC;
            case "docx" -> DOCX;
            case "ppt" -> PPT;
            case "pptx" -> PPTX;
            case "pptm" -> PPTM;
            case "xls" -> XLS;
            case "xlsx" -> XLSX;
            case "xlsm" -> XLSM;
            default -> OTHER;
        };
    }
}