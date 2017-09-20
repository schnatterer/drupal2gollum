package info.schnatterer.drupal2gollum

import groovy.io.FileType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.slf4j.LoggerFactory

import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

import static info.schnatterer.drupal2gollum.Util.*

class Migration {
    static final LOG = LoggerFactory.getLogger(Migration.class)

    static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder()

    Database database
    Git git
    MarkdownProcessor markdownProcessor
    Map<Long, String> node2Taxonomy

    String gitAuthorName
    String gitAuthorEmail
    String targetPath
    String filesPath

    void run(String jdbcUrl, String targetPath, String filesPath, String gitAuthorName, String gitAuthorEmail) {
        this.targetPath = targetPath
        this.filesPath = filesPath
        this.gitAuthorName = gitAuthorName
        this.gitAuthorEmail = gitAuthorEmail

        git = Git.init().setDirectory(new File(targetPath)).call()
        database = new Database(jdbcUrl)
        try {
            migrate(database)
        } finally {
            database.close()
            git.close()
        }
    }

    private void migrate(Database database) {
        migrateFiles(database)
        migrateNodes(database)
    }

    void createNid2TitleRedirects() {
        database.createNode2Title().each {
            nid, title ->
            println "redir /node/$nid /${URLEncoder.encode(toFilename(title).trim(), "UTF-8")}"
        }
    }

    private void migrateNodes(database) {
        def allRevisions = database.findRevisions()
        Map<Long, String> currentTitles = [:]

        LOG.info("Committing {} HTML-revisions", allRevisions.size())

        allRevisions.each { currentRevision ->
            long nid = currentRevision.get('nid')
            String currentTitle = currentRevision.get('title')
            def formerTitle = currentTitles.get(nid)
            if (formerTitle != null && formerTitle != currentTitle) {
                LOG.debug("Node {}: changed title from {} to {}. Date: {}",
                    nid, formerTitle, currentTitle, createDate(currentRevision).toString())
                gitMv(createHtmlFileName(formerTitle), createHtmlFileName(currentTitle), createDate(currentRevision))
            }
            currentTitles.put(nid, currentTitle)

            String filenameHtml = createHtmlFileName(currentTitle)
            File targetFile = new File(targetPath, filenameHtml)
            targetFile.write(createHtml(currentRevision))

            addAndCommit(filenameHtml, currentRevision.get('log'), createDate(currentRevision))
        }


        Map<Long, String> node2Title = database.createNode2Title()
        node2Taxonomy = database.createNode2Taxonomy()
        this.markdownProcessor = new MarkdownProcessor(node2Title)

        // Iterate in reverse order so that the last edited files will be the newest one in the git history
        database.findLastRevisions().each {
            migrateLastRevisionToMarkdown(it)
        }
    }

    private Date createDate(currentRevision) {
        Database.createDateFromTimestamp(currentRevision.get('timestamp'))
    }

    private String createHtmlFileName(String currentTitle) {
        "${toFilename(currentTitle)}.html"
    }

    /**
     * Git has a rename command git mv, but that is just a convenience. The effect is indistinguishable from removing the file and adding another with different name and the same content
     */
    void gitMv(String formerNameHtml, String newNameHtml, Date originalDate) {
        new File(targetPath, formerNameHtml).renameTo(new File(targetPath, newNameHtml))
        git.rm().addFilepattern(formerNameHtml).call()
        git.add().addFilepattern(newNameHtml).call()
        git.commit()
            .setMessage("Rename \"$formerNameHtml\" to \"$newNameHtml\"")
            .setCommitter(new PersonIdent(gitAuthorName, gitAuthorEmail, originalDate, TimeZone.getDefault()))
            .call()
    }

    private void migrateLastRevisionToMarkdown(def currentRevision) {

        long nid = currentRevision.get('nid')

        String baseFilename = toFilename(currentRevision.get('title'))
        String filenameMarkdown = "${baseFilename}.md"

        // Mv HTML to MD file
        gitMv(createHtmlFileName(currentRevision.get('title')), filenameMarkdown, new Date())

        File targetFile = new File(targetPath, filenameMarkdown)
        LOG.info("Node {}: Converting to markdown (revision date: {}). Filename: {}",
            nid, createDate(currentRevision), targetFile)

        def html = HtmlPreparer.prepare(createHtml(currentRevision))

        def markdown = markdownProcessor.convert(html)
        markdown = addTaxonomy(markdown, nid)

        validate(markdown, baseFilename)

        targetFile.write(markdown)

        addAndCommit(filenameMarkdown,
            "\"$baseFilename\" - converted to markdown using drupal2gollum", new Date())
    }

    def validate(String markdown, String baseFilename) {
        warnIfContains(markdown, 'node[', baseFilename)
        warnIfContains(markdown, "/$DRUPAL_FILES_DIR", baseFilename)
        warnIfContains(markdown, 'syntaxhighlighter', baseFilename)
        warnIfContains(markdown, LOCAL_LINK_ALIAS, baseFilename)
        warnIfContains(markdown, IMAGE_TAG_ALIAS, baseFilename)
        warnIfContains(markdown, REMOVE_UNNECESSARY_MD_LIST_ELEMENT, baseFilename)
        // Note '``````````' is remarks way for marking code sections. Let's just be okay with this...
        if (!asciiEncoder.canEncode(baseFilename)) {
            LOG.warn('Filename "{}" is not ascii. Gollum might not be able to handle it', baseFilename)
        }
    }

    private void warnIfContains(String markdown, String snippet, filename) {
        if (markdown.contains(snippet)) {
            LOG.warn("Transformed markdown for node {}, contains \"{}\"", filename, snippet)
        }
    }

    /**
     * Adds the taxonomy as "tags" at the end of the file
     */
    private String addTaxonomy(String markdown, long nid) {
        def tags = node2Taxonomy.get(nid)
        if (tags) {
            markdown = markdown + "\n\n## Tags\n\n"
            tags.each { markdown = markdown + "* tag=${it}\n" }
            return markdown
        }
        markdown
    }

    private void addAndCommit(String filename, String commitMessage, Date originalDate) {
        git.add()
            .addFilepattern(filename)
            .call()
        git.commit()
            .setMessage(commitMessage)
            .setCommitter(new PersonIdent(gitAuthorName, gitAuthorEmail, originalDate, TimeZone.getDefault()))
            .call()
        LOG.debug("{}, original Date: {}", commitMessage, originalDate)
    }


    private void migrateFiles(Database database) {
        Paths.get(targetPath, GOLLUM_UPLOADS_DIR).toFile().mkdirs()
        def unprocessedFiles = findExistingFilenames()

        database.createFilename2Timestamp().each { filename, timestamp ->
            unprocessedFiles.remove(filename)
            copyAndCommitFile(filename, timestamp)
        }

        if (!unprocessedFiles.isEmpty()) {
            LOG.warn("The following files exist, but are not listed in database: {}. Committing anyway.", unprocessedFiles)
            unprocessedFiles.each { filename -> copyAndCommitFile(filename, new Date())
            }
        } else {
            LOG.info("All files from folder have been found in drupal DB and were migrated to gollum.")
        }
    }

    private List<String> findExistingFilenames() {
        def existingFilenames = []
        new File(filesPath).eachFile(FileType.FILES) { file ->
            existingFilenames << file.getName()
        }
        existingFilenames
    }

    private void copyAndCommitFile(String filename, Date timestamp) {
        def asciiFilename = toFilename(filename)
        if (asciiFilename != filename) {
            LOG.info("Filename {} is not ASCII encoding, therefore gollum will not be able to handle it. Converting to {. This will also be renamed in local links and [node:..] tags",
                filename, asciiFilename)
        }

        try {
            Files.copy(
                Paths.get(filesPath, filename),
                Paths.get(targetPath, GOLLUM_UPLOADS_DIR, asciiFilename))
            addAndCommit("$GOLLUM_UPLOADS_DIR/$asciiFilename", "Uploaded \"$asciiFilename\"", timestamp)
        } catch (NoSuchFileException ignored) {
            LOG.debug("File listed in database no found in files path: $filename, originally created $timestamp. Skipping...")
        } catch (FileAlreadyExistsException ignored) {
            LOG.warn("File listed in database already exists: $asciiFilename, originally created $timestamp. Skipping...")
        }
    }

    private String createHtml(revision) {
        String summary = revision.get('body_summary')
        if (summary == null) {
            summary = ""
        }

        "${summary}\n${revision.get('body_value')}"
    }
}

