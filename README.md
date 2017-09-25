# drupal2gollum

A basicly working drupal (html) to [gollum](https://github.com/gollum/gollum) (markdown) migration.
100% groovy.

## Usage
Download from [here](https://github.com/schnatterer/drupal2gollum/releases) and extract.

Exemplary call: 

```
drupal2gollum jdbc:mariadb://localhost:3306/databse?user=root&password=pw targetFolder drupalFilesFolder "Git author" "git-em@i.l"
```

## More Details

I built this to be just enough to fulfil my needs of converting a 10-year old Drupal CMS (with ~400 pages and some plugins) to a gollum wiki.
The program was rather hacked on a couple of afternoons try-and-error-style just to get my data ASAP to the new system with as less errors as possible. Not my best work.

Having said that, drupal2gollum still offers a couple of features. So it might be a starting point for others.

☑ Converts all drupal node revisions to Git revisions (HTML files)  
☑ Converts all drupal files to gollum uploads  
☑ Converts lastest node revision HTML to Markdown  
☑ Converts HTML links to drupal files into links to gollum files (also for images)  
☑ Converts taxonomy (latest revision only) to `Tag` heading at the end of each markdown page. The individual tags are converted to a list starting in `tag=` so you can use the gollum search to find tags using `tag=...` as search term.  
☑ Converts [Link node plugin](https://www.drupal.org/project/link_node) syntax to gollum links  
☑ Converts [Footnode plugin](https://www.drupal.org/project/link_node) syntax to links   
☑ Converts [Table of Concent plugin](https://www.drupal.org/project/tableofcontents) to [gollum TOC tag](https://github.com/gollum/gollum/wiki#table-of-contents-toc-tag)  
☑ Converts [Syntax highliger plugin](https://www.drupal.org/project/syntaxhighlighter) syntax to Fenced Code Blocks. Example: `{syntaxhighlighter brush: java}` or `<pre class="brush: java; ...>` to \```java ...   
☑ Converts courier new fonts to back-ticks  
☑ Keeps images as HTML Tags (in order to preserve `height` and `width`)  
☑ Keeps tables as HTML Tags (in order to support nested and more complex tables)  
☑ Corrects invalid nested lists: Example: \<ul\>\<ul\> to \<ul\>\<li\>\<ul\>
