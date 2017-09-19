package info.schnatterer.drupal2gollum

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node

import java.util.regex.Pattern

import static info.schnatterer.drupal2gollum.Util.*

class HtmlPreparer {
    static final REGEX_LOCAL_LINK = '(/|\\?q=)'

    static def prepare(String html) {
        def processedHtml = migrateSyntaxHighlighterPre(html)
        processedHtml = migrateCourier(processedHtml)
        processedHtml = makeLocalLinksAbsolute(processedHtml)
        //processedHtml = moveUpHeadings(processedHtml)
        processedHtml = migrateImageLinks(processedHtml)
        correctNestedLists(processedHtml)
    }

    static String moveUpHeadings(String html) {
        html.replace('<h2', '<h1')
            .replace('<h3', '<h2')
            .replace('<h4', '<h3')
            .replace('<h5', '<h4')
            .replace('<h6', '<h5')
    }

    /**
     *  <pre class="brush: java;..."> -> {syntaxhighlighter
     *
     *  Otherwise the brush will be lost during markdown conversion.
     */
    static def migrateSyntaxHighlighterPre(String html) {
        html.replaceAll(
            Pattern.compile('<pre class="(brush: ([^;]*?);)?[^>]*">((?is).*?)</pre>'),
            { it ->
                def brush = it[2]
                def code = it[3]
                if (!brush) {
                    throw new IllegalArgumentException("Missing brush in tag ${it[0]}")
                }

                String title = ""
                def titleMatcher = (it[0] =~ /title="([^"]*?)"/)
                if (titleMatcher.size() > 0) {
                    title = "${titleMatcher[0][1]}\n"
                }
                "$title<pre>{syntaxhighlighter brush: $brush}${code}{/syntaxhighlighter}</pre>"
            })
    }

    /**
     * Migrates text in courier new to ``. Otherwise its ignored by markdown conversion.
     */
    static def migrateCourier(String html) {
        html.replaceAll(Pattern.compile('<span style="font-family: courier new,courier;">([^<]*?)</span>'), {
            it -> "`${it[1]}`"
        })
    }

    /**
     * Changes local links, so they are converted by MD converter.
     */
    static def makeLocalLinksAbsolute(String html) {
        html.replaceAll("href=\"$REGEX_LOCAL_LINK", "href=\"${LOCAL_LINK_ALIAS}")
    }

    /**
     *  <img > -> {img>
     *
     *  Otherwise image will be lost during markdown conversion.
     *  Also, Markdown does not have the means for specifying image size, so we keep them HTML-style.
     *
     *  In addition, converts local links to gollum's dir structure right away.
     */
    static String migrateImageLinks(String html) {
        html.replaceAll(
            Pattern.compile('<img.*?>',), { it ->
            // Converts local images links to gollum's dir structure right away
            def imageTag = convertFileLinks(it.replace('/?q=', '/').replace('?q=', '/'), '"', '"')
            imageTag.replace(IMAGE_TAG_HTML, IMAGE_TAG_ALIAS)
        })
    }

    /**
     * The Markdown converter ignores incorrect nested lists, i.e. when an ul or ol tag has a direct ul or ol child.
     * According to W3C ul and ol may only have li children. So, insert a li element.
     *
     * Note: As this is using an HTML parser (JSOUP) the HTML output's formatting is most likely different after calling
     * this method.
     */
    static String correctNestedLists(String html) {
        Document doc = Jsoup.parse(html)

        checkIfListsHaveLiParent(doc)

        return doc.body().html()
    }

    static def checkIfListsHaveLiParent(Node node) {
        if (!node.childNodes().isEmpty()) {
            node.childNodes().each { child ->
                if (child instanceof Node) {
                checkIfListsHaveLiParent(child)
                }
            }
        }
        // For some reasons the lists corrected here are rendered as "*  *" --> Remove in MD posprocessor
        if ((node.nodeName() in ['ul', 'ol']) && node.parent() != null && (node.parent().nodeName() in ['ul', 'ol'])) {
            node.wrap("<li>$REMOVE_UNNECESSARY_MD_LIST_ELEMENT  </li>")
        }
    }

}
