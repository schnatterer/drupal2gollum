package info.schnatterer.drupal2gollum

import org.junit.Test

import static org.junit.Assert.assertEquals

class HtmlPreparerTest {

    @Test
    void migrateSyntaxHighlighterPre() throws Exception {
        String html= '''
        <pre class="brush: java;">import org.junit.Assert; import org.junit.Test;

        public class test { ); } } </pre>
        '''
        def processedHtml = HtmlPreparer.migrateSyntaxHighlighterPre(html)
        assert processedHtml == '''
        <pre>{syntaxhighlighter brush: java}import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}</pre>
        '''
    }

    @Test
    void migrateSyntaxHighlighterPreWithTitle() throws Exception {
        String html= '''
        <pre class="brush: java; auto-links: true; collapse: true; first-line: 1; html-script: false; smart-tabs: true; tab-size: 4; toolbar: false; codetag" title="TITLE">import org.junit.Assert; import org.junit.Test;

        public class test { ); } } </pre>
        '''
        def processedHtml = HtmlPreparer.migrateSyntaxHighlighterPre(html).trim()
        def expected = 'TITLE\n' + '''<pre>{syntaxhighlighter brush: java}import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}</pre>'''
        assertEquals(expected, processedHtml)
    }

    @Test(expected = IllegalArgumentException.class)
    void migrateSyntaxHighlighterPreNoBrush() throws Exception {
        String htmlWithoutBrush = '''
        <pre class="auto-links: true; collapse: false; first-line: 1;">import org.junit.Assert; import org.junit.Test;

        public class test { ); } } </pre>
        '''
        HtmlPreparer.migrateSyntaxHighlighterPre(htmlWithoutBrush)
    }

    @Test
    void makeImageToLinks() throws Exception {
        String html = ' <a href="/system/bl">text</a> '

    }

    @Test
    void makeLocalLinksAbsolute() throws Exception {
        String html = ' <a href="/system/bl">text</a> '
        def processedHtml = HtmlPreparer.makeLocalLinksAbsolute(html)

        assert processedHtml == ' <a href="http://LOCALLINKsystem/bl">text</a> '
    }

    @Test
    void makeLocalLinksAbsoluteQuery() throws Exception {
        String html = ' <a href="?q=system/bl">text</a> '
        def processedHtml = HtmlPreparer.makeLocalLinksAbsolute(html)

        assert processedHtml == ' <a href="http://LOCALLINKsystem/bl">text</a> '
    }

    @Test
    void correctNestedLists() {
        def html = '''
        <ul>
            <li><span style="font-family: courier new,courier;">static</span> </li>
            <li><ul><li>correct</li></ul></li>
            
            <ul>
                <li>Worst Practice</li>
            </ul>y
            
            <ul>
                <li>aa</li>
                <ul>
                    <li>Even Worse</li>
                </ul>
            </ul>
        </ul>
        '''
        def processedHtml = HtmlPreparer.correctNestedLists(html)
        assert new XmlParser().parseText(processedHtml).li[2].ul.li.text() == "Worst Practice"
    }
}
