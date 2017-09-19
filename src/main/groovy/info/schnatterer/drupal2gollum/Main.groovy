package info.schnatterer.drupal2gollum

class Main {

    static void main(String[] args) {
        def jdbcUrl = args[0]
        def targetPath = args[1]
        def filesPath = args[2] // Path to uploaded files
        def gitAuthorName = args[3]
        def gitAuthorEmail = args[4]
        new Migration().run(jdbcUrl, targetPath, filesPath, gitAuthorName, gitAuthorEmail)
    }
}
