package info.schnatterer.drupal2gollum

import org.junit.Test

import static org.junit.Assert.assertEquals

class UtilTest {

    @Test
    void convertFileLinksMarkdown() throws Exception {
        String markdown = '[text](/system/files/some-image-with-long-name-nöt-äscii.png)'
        def processedMarkdown = Util.convertFileLinks(markdown, '(', ')')
        assertEquals("[text](/uploads/some-image-with-long-name-noet-aescii.png)", processedMarkdown)
    }

    @Test
    void convertFileLinksHtml() throws Exception {
        String html = '<img src="/system/files/some-image-with-long-name-nöt-äscii.png" width="500" height="151">'
        def processedHtml = Util.convertFileLinks(html, '"', '"')
        assertEquals('<img src="/uploads/some-image-with-long-name-noet-aescii.png" width="500" height="151">', processedHtml)
    }

    @Test
    void createsFilenameBackslash(){
        expectEscaped('\\')
    }

    @Test
    void createsFilenameSlash(){
        expectEscaped('/')
    }

    @Test
    void createsFilenameColon(){
        expectEscaped(':')
    }

    @Test
    void createsFilenameAsterisk(){
        expectEscaped('*')
    }

    @Test
    void createsFilenameQuestionMark(){
        expectEscaped('?')
    }

    @Test
    void createsFilenameQuotes(){
        expectEscaped('"')
    }

    @Test
    void createsFilenameLessThan(){
        expectEscaped('<')
    }

    @Test
    void createsFilenameGreaterThan(){
        expectEscaped('>')
    }

    @Test
    void createsFilenamePipe(){
        expectEscaped('|')
    }

    void expectEscaped(String filname) {
        assert '-' == Util.makeValidFilename(filname)
    }

}
