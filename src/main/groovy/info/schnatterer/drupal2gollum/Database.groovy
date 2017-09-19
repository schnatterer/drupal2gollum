package info.schnatterer.drupal2gollum

import groovy.sql.Sql

class Database {

    Sql sql

    Database(String url) {
        this.sql = create(url)
    }

    private def create(String url) {
        def driver = 'org.mariadb.jdbc.Driver'
        return Sql.newInstance(url, driver)
    }

    def createNode2Title() {
        Map node2Title = [:]
        sql.query("select n.nid, n.title from node n\n" +
            "  inner join node_revision r on n.nid = r.nid\n" +
            "ORDER BY r.timestamp desc;") { resultSet ->
            while (resultSet.next()) {
                node2Title.put(resultSet.getLong('nid'),
                        resultSet.getString('title'))
            }
        }
        node2Title
    }

    Map<Long, String> createNode2Taxonomy() {
        Map node2Taxonomy= [:]
        sql.query("select  n.nid, t.name from node n " +
            "inner join taxonomy_index tn on n.nid = tn.nid" +
            " inner join taxonomy_term_data t on tn.tid = t.tid") { resultSet ->
            while (resultSet.next()) {
                def nid = resultSet.getLong('nid')
                def existingTags = node2Taxonomy.get(nid)
                if (existingTags == null) {
                    def tags = [resultSet.getString('name')]
                    node2Taxonomy.put(nid, tags)
                } else {
                    existingTags.add(resultSet.getString('name'))
                }
            }
        }
        node2Taxonomy
    }

    def findRevisions(long nid) {
        sql.rows("SELECT\n" +
                "  n.nid,\n" +
                "  r.timestamp,\n" +
                "  r.log,\n" +
                "  r.title,\n" +
                "  b.body_summary,\n" +
                "  b.body_value\n" +
                "FROM node n\n" +
                "  INNER JOIN node_revision r ON n.nid = r.nid\n" +
                "  INNER JOIN field_revision_body b ON r.vid = b.revision_id\n" +
                "WHERE n.nid = ?\n" +
                "ORDER BY r.timestamp ASC;", nid)
    }

    Map<String, Date> createFilename2Timestamp() {
        Map filename2Timestamp = [:]
        sql.query('select uri, timestamp from file_managed;')  { resultSet ->
            while (resultSet.next()) {
                filename2Timestamp.put(resultSet.getString('uri').replace('private://', '').replace('public://', ''),
                    createDateFromTimestamp(resultSet.getString('timestamp')))
            }
        }
        filename2Timestamp
    }

    static Date createDateFromTimestamp(Object timestampFromDb) {
        // Drupal seems not to store the last three leading zeros
        new Date("${timestampFromDb}000".toLong())
    }

    void close() {
        if (sql != null) {
            sql.close()
        }
    }
}
