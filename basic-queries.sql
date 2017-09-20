-- Node & Revisions
select n.nid, n.title, r.vid, r.timestamp, r.log, b.body_summary, b.body_value from node n
  inner join node_revision r on n.nid = r.nid
  inner join field_revision_body b on r.vid = b.revision_id
ORDER BY r.timestamp desc;

-- Node: Only last body
select n.nid, n.title, r.timestamp, b.body_summary, b.body_value from node n
  inner join field_data_body b on n.nid = b.entity_id
  inner join node_revision r on r.vid = b.revision_id
  ORDER BY r.timestamp asc;

-- Join: Node -> Taxonomy
select  n.nid, n.title, tn.created, t.name from node n
  inner join taxonomy_index tn on n.nid = tn.nid
  inner join taxonomy_term_data t on tn.tid = t.tid

-- Same result from different table (no timestamp)
select t.* from node n
  inner join field_data_taxonomy_vocabulary_1 t on n.nid = t.entity_id

-- Files and creation date
select filename, timestamp from file_managed

-- All comments
select c.nid, c.subject, b.comment_body_value, c.created from field_revision_comment_body b
inner join comment c on c.cid = b.entity_id


-- All revisions. Oldest first
select n.nid, n.title, r.timestamp, r.log, b.body_summary, b.body_value from node n
  inner join node_revision r on n.nid = r.nid
  inner join field_revision_body b on r.vid = b.revision_id
ORDER BY r.timestamp asc;


-- All revisions and first version of each file. Oldest first. Files have nid=0;
select n.nid, n.title, r.timestamp, r.log, b.body_summary, b.body_value from node n
  inner join node_revision r on n.nid = r.nid
  inner join field_revision_body b on r.vid = b.revision_id
UNION
select 0, uri as title, min(timestamp), '', '', '' from file_managed
  GROUP BY title
ORDER BY timestamp asc;


select  uri, filename, TIMEstamp from file_managed
where lower(filename) like '%forum%'
order by filename, timestamp desc;

select  uri, filename as title, min(TIMEstamp) from file_managed order by filename, timestamp desc group by title;

  GROUP BY title
