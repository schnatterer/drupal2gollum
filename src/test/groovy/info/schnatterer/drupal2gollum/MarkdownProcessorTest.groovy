package info.schnatterer.drupal2gollum

import org.junit.Test

import static org.junit.Assert.assertEquals

class MarkdownProcessorTest {

    def nid2fileName = [42L: "The question of everything"]
    MarkdownProcessor markdownProcessor = new MarkdownProcessor(nid2fileName)

    @Test
    void migrateNodeLinks() throws Exception {
        String markdownWithNodeLinks = '''# Heading
        \\[node:42\\]
        ## Other heading
        \\[node:42,title="WithTitle"\\]
        \\[node:42, title ="WithTitle"\\]
        \\[node:42, title = "WithTitle"\\]
        '''

        def processedMarkdown = markdownProcessor.migrateNodeLinks(markdownWithNodeLinks)
        assert processedMarkdown == '''# Heading
        [The question of everything](The question of everything)
        ## Other heading
        [WithTitle](The question of everything)
        [WithTitle](The question of everything)
        [WithTitle](The question of everything)
        '''
    }

    @Test
    void migrateToc() throws Exception {
        String markdownWithNodeLinks = '''
        \\[TOC\\]
        \\[toc\\]
        # Heading
        \\[TOC\\]
        \\[ToC\\]
        '''
        def processedMarkdown = markdownProcessor.migrateToc(markdownWithNodeLinks)
        assert processedMarkdown == '''
        [[_TOC_]]
        [[_TOC_]]
        # Heading
        [[_TOC_]]
        [[_TOC_]]
        '''
    }

    @Test
    void migrateSyntaxHighlighter() throws Exception {
        String markdownWithNodeLinks = '''
        ``````````
        {syntaxhighlighter brush: java }import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}
        ``````````
        '''
        def processedMarkdown = markdownProcessor.migrateSyntaxHighlighter(markdownWithNodeLinks)
        assertEquals('''
        ```java
        import org.junit.Assert; import org.junit.Test;

        public class test { ); } } 
        ```
        ''', processedMarkdown)
    }

    @Test
    void migrateSyntaxHighlighterOneLine() throws Exception {
        String markdownWithNodeLinks = "\\{syntaxhighlighter brush:bash\\}echo -n hallo\\{/syntaxhighlighter\\}"
        def processedMarkdown = markdownProcessor.migrateSyntaxHighlighter(markdownWithNodeLinks)
        assertEquals('```bash\necho -n hallo\n```', processedMarkdown)
    }

    @Test
    void migrateSyntaxHighlighterWithTitle() throws Exception {
        String markdownWithNodeLinks = '''
        ``````````
        {syntaxhighlighter brush: java title: "someTitle"}import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}
        ``````````
        '''
        def processedMarkdown = markdownProcessor.migrateSyntaxHighlighter(markdownWithNodeLinks)
        assertEquals('''
        someTitle
        ```java
        import org.junit.Assert; import org.junit.Test;

        public class test { ); } } 
        ```
        ''', processedMarkdown)
    }

    @Test
    void migrateSyntaxHighlighterWithOtherStuff() throws Exception {
        String markdownWithNodeLinks = '''
        ``````````
        {syntaxhighlighter brush: java;fontsize: 100; first-line: 1;}import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}
        ``````````
        '''
        def processedMarkdown = markdownProcessor.migrateSyntaxHighlighter(markdownWithNodeLinks)
        assertEquals('''
        ```java
        import org.junit.Assert; import org.junit.Test;

        public class test { ); } } 
        ```
        ''', processedMarkdown)
    }

    @Test(expected = IllegalArgumentException.class)
    void migrateSyntaxHighlighterNoBrush() throws Exception {
        String markdownWithoutBrush = '''``````````
        {syntaxhighlighter}import org.junit.Assert; import org.junit.Test;

        public class test { ); } } {/syntaxhighlighter}
        ``````````
        '''
        markdownProcessor.migrateSyntaxHighlighter(markdownWithoutBrush)
    }

    @Test
    void convertLocalHtmlLinks() {
        def processedLink = markdownProcessor.convertLocalHtmlLinks('<a href="/system/files/nöt-äscii">text</a>')
        assertEquals '<a href="/uploads/noet-aescii">text</a>', processedLink
    }

    @Test
    void convertImageTagsBackToHtml() {
        def processedLink = markdownProcessor.convertImageTagsBackToHtml('$img src="/uploads/XSLTEngine\\_1.png" width="515" height="313">')
        assertEquals '<img src="/uploads/XSLTEngine_1.png" width="515" height="313">', processedLink
    }

    @Test
    void removeUnnecessaryListElement() {
        String markdown = '''cc
        *  fds
        *  FORSOMEREASONTHISISRENDEREDASEXTRALISTELEMENT
          *  Sub list
        1.
        22. FORSOMEREASONTHISISRENDEREDASEXTRALISTELEMENT
          1.  Sub list
        '''
        String expected = '''cc
        *  fds
          *  Sub list
        1.
          1.  Sub list
        '''
        def processedMarkdown = markdownProcessor.removeUnnecessaryListElement(markdown)
        assertEquals(expected, processedMarkdown)
    }

}
