
# TrueRSS version 0.0.2

# [Demo](http://demo.truerss.net/)

# [screenshots](https://github.com/truerss/truerss/wiki/screenshots)

# [group](https://groups.google.com/forum/#!forum/truerss)

is open source feed reader with customizable plugin system for any content (atom, rss, youtube channels...).

# How to run

1. install java
2. download truerss-${version}.jar
3. add config

```
// Sample config (with sqlite backend)
truerss {
  port = 8000
  host = localhost
  wsPort = 8080
  update-parallelism = 10
  db {
    backend = sqlite
    port = ""
    host = ""
    dbname = "/path/to/dbName.db"
    username = ""
    password = ""
  }
  plugins = {}
}
```

4. run with `java -jar -Xmx150m path/to/truerss-${version}.jar &`
5. open in browser localhost:8000/
6. enjoy!

# How to develop

## Before work

1. install java
2. scala, and sbt
3. for ui part install coffeescript

Compile app with `rake` or with `sbt jsbuild` command

## And then...

1. clone repo
2. run sbt
3. for work with UI need install web-dependencies (with `sbt install`) and coffeescript
4. for backend part just open in your IDE and add feature, fix bugs
5. create pull request

# License: MIT




