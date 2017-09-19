package info.schnatterer.drupal2gollum

import com.overzealous.remark.Options
import com.overzealous.remark.Remark
import org.slf4j.LoggerFactory

import java.util.regex.Pattern
import static Util.*

class MarkdownProcessor {
    private static final LOG = LoggerFactory.getLogger(MarkdownProcessor.class)
    Map<Long, String> node2Title
    Remark remark

    MarkdownProcessor(Map<Long, String> node2Title) {
        this.node2Title = node2Title

        // See http://remark.overzealous.com/manual/usage.html
        Options options = Options.github()
        /*   [link](http://some.link)  instead of
        *   [link][]
        *   ..
        *   [link]: http://some.link*/
        options.inlineLinks = true
        // Also convert relative links (e.g. the ones to our files)
        options.preserveRelativeLinks = true
        options.tables = Options.Tables.LEAVE_AS_HTML
        remark = new Remark(options)
    }

    String convert(String html) {
        def plainMarkdown = remark.convertFragment(html)
        postProcess(plainMarkdown)
    }

    String postProcess(String plainMarkdown) {
        String processedMarkdown = migrateNodeLinks(plainMarkdown)
        processedMarkdown = migrateToc(processedMarkdown)
        processedMarkdown = migrateSyntaxHighlighter(processedMarkdown)
        processedMarkdown = migrateCourier(processedMarkdown)
        processedMarkdown = convertLocalLinkAliasToLocalLink(processedMarkdown)
        processedMarkdown = convertFileLinks(processedMarkdown, '(', ')')
        processedMarkdown = convertImageTagsBackToHtml(processedMarkdown)
        processedMarkdown = convertFootNotes(processedMarkdown)
        processedMarkdown = removeUnnecessaryListElement(processedMarkdown)
        convertLocalHtmlLinks(processedMarkdown)
    }

    /**
     * \[fn\][Simplify Spring Apps](http://www.developer.com/java/other/article.php/3756831)\[/fn\]
     * ->
     * ([Simplify Spring Apps](http://www.developer.com/java/other/article.php/3756831))
     */
    String convertFootNotes(String markdown) {
        markdown
            .replace('\\[fn\\]', '(')
            .replace('\\[/fn\\]', ')')
    }

    /**
     * Remaining hrefs (e.g. in TABLES) are moved from /system/files to new gollum upload file
     * <a href="/system/files/
     */
    String convertLocalHtmlLinks(String markdown) {
        markdown.replaceAll(
            Pattern.compile('<a.*?>',), { it ->
            // Converts local images links to gollum's dir structure right away
            convertFileLinks(it.replace('/?q=', '/').replace('?q=', '/'), '"', '"')
        })
    }

    /**
     *{syntaxhighlighter brush: java}...{/syntaxhighlighter}* to
     * ```java
     * ...
     * ```
     */
    String migrateSyntaxHighlighter(String markdown) {
        markdown
            .replaceAll(Pattern.compile("\\\\\\{syntaxhighlighter([^\\}]*)\\\\\\}"),
            { it ->
                "{syntaxhighlighter${it[1]}}"
            })
            .replace("\\{/syntaxhighlighter\\}", "{/syntaxhighlighter}")
            .replaceAll(
            Pattern.compile("([ |\\t]*)?(``````````\\s*)?\\{syntaxhighlighter( brush:( )?([\\w]*))?( title: \"([^\"]*)\")?[^\\}]*?\\}((?is).*?)\\{/syntaxhighlighter}(\\s*``````````)?"),
            { it ->
                def blanksAndTabs = it[1]
                def title = it[7]
                def code = it[8]
                def brush = it[5]
                if (!brush) {
                    throw new IllegalArgumentException("Missing brush in tag ${it[0]}")
                }
                String titleOrEmpty = ""
                if (title) {
                    titleOrEmpty = "$blanksAndTabs$title\n"
                }
                "$titleOrEmpty$blanksAndTabs```${brush}\n$blanksAndTabs$code\n$blanksAndTabs```"
            })
    }

    /**
     * \[TOC\] -> [[_TOC_]]
     */
    String migrateToc(String markdown) {
        markdown.replaceAll(Pattern.compile('(?i)\\\\\\[TOC\\\\\\]'),
            { it ->
                "[[_TOC_]]"
            })
    }

    /**
     * * \[node:90]} -> [title](title) and
     * * \[node:90,title="link text"\] > [link text](title) and
     */
    String migrateNodeLinks(String markdown) {
        markdown.replaceAll(Pattern.compile('\\\\\\[node:(\\d*)(,( )?title( )?=( )?"([^"]*)")?\\\\\\]'),
            { it ->
                def nid = Long.valueOf(it[1])
                def linkText = it[6]
                def title = node2Title.get(nid)

                if (!title) {
                    LOG.warn("Node {} not found, can't convert link {}", nid, it[0])
                }

                // The file names in gollum must be ascii encoded
                def newLink = toFilename(title)
                if (linkText) {
                    // \[node:90,title="link text"\] > [link text](title)
                    "[$linkText]($newLink)"
                } else {
                    // \[node:90]\ -> [title](title)
                    "[$title]($newLink)"
                }
            })
    }

    String migrateCourier(String markdown) {
        markdown.replace("\\`", "`")
    }

    /**
     * See {@link HtmlPreparer#makeLocalLinksAbsolute(java.lang.String)}
     */
    String convertLocalLinkAliasToLocalLink(String markdown) {
        markdown.replace(LOCAL_LINK_ALIAS, LOCAL_LINK_REPLACEMENT)
    }

    /**
     * See {@link HtmlPreparer#migrateImageLinks(java.lang.String)}
     */
    String convertImageTagsBackToHtml(String markdown) {
        markdown.replaceAll(
            Pattern.compile("\\$IMAGE_TAG_ALIAS.*?>",), { it ->
            // Remove any escape backslashes introduced by markdown converter
            it.replace('\\', '')
                .replace(IMAGE_TAG_ALIAS, IMAGE_TAG_HTML)
        })

    }

    /**
     * See {@link HtmlPreparer#correctNestedLists(java.lang.String)}
     */
    String removeUnnecessaryListElement(String markdown) {
        markdown.replaceAll(".*[\\*|\\d+\\.] +$REMOVE_UNNECESSARY_MD_LIST_ELEMENT(?is).*?\n", '')
    }
}
