package info.schnatterer.drupal2gollum

import java.text.Normalizer
import java.util.regex.Pattern;

class Util {

    public static final LOCAL_LINK_ALIAS = 'http://LOCALLINK'
    public static final LOCAL_LINK_REPLACEMENT = '/'
    public static final IMAGE_TAG_HTML = '<img'
    public static final IMAGE_TAG_ALIAS = '$img'
    public static final REMOVE_UNNECESSARY_MD_LIST_ELEMENT = 'FORSOMEREASONTHISISRENDEREDASEXTRALISTELEMENT'
    public static final GOLLUM_UPLOADS_DIR = 'uploads'
    public static final DRUPAL_FILES_DIR = 'system/files'

    static def toFilename(String title) {
        // Gollum can only handle ascii file names.
        // Some tiles may contain characters that cannot be used in filenames. Replace them.
        makeValidFilename(toAscii(title))
    }

    static String makeValidFilename(String title) {
        // invalid chars? invalidChars = "\/:*?\"<>|"
        title.replaceAll("\\\\|/|:|\\*|\\?|\"|<|>|\\|",'-')
    }

    private static String toAscii(String filename) {
        def replacedUmlauts = filename.replace('ä', 'ae')
            .replace('ö', 'oe')
            .replace('ü', 'ue')
        // See https://stackoverflow.com/a/2413228/1845976
        // TODO this does not convert unicode chars such as ®, α, etc.
        Normalizer.normalize(replacedUmlauts, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    }

    /**
     * /system/files/xyä -> /uploads/xya
     */
    static String convertFileLinks(String markdownOrHtml, String delimiterBegin, String delimiterEnd) {
        def delimiterBeginEscaped = Pattern.quote(delimiterBegin)
        def delimiterEndEscaped = Pattern.quote(delimiterEnd)

        markdownOrHtml.replaceAll(
            Pattern.compile("$delimiterBeginEscaped/$DRUPAL_FILES_DIR/(.*?)$delimiterEndEscaped"), { it ->
            String filename = it[1]
            "$delimiterBegin/$GOLLUM_UPLOADS_DIR/${toFilename(URLDecoder.decode(filename, "UTF-8"))}$delimiterEnd"
        })
    }
}
