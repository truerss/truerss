
# TrueRSS version 1.0.0

# [screenshots](https://github.com/truerss/truerss/wiki/screenshots)

# [group](https://groups.google.com/forum/#!forum/truerss)

# [![Codewake](https://www.codewake.com/badges/ask_question.svg)](https://www.codewake.com/p/truerss)

is open source feed reader with customizable plugin system for any content (atom, rss, youtube channels...).

# docs

read [wiki](https://github.com/truerss/truerss/wiki)


# How to run

1. install java
2. download release zip file 
3. add configuration file: 

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

4. unzip 
5. run with `bin/truerss`
6. open in browser localhost:8000/
7. enjoy!

# How to develop

## Before work

1. install java
2. install docker (need for run tests with different dbs: postgresql, and mysql)
3. scala, and sbt
4. for ui part install coffeescript (run `sbt install` <- will install all js/css deps)

### run tests:

`sbt test` - unit tests

`sbt 'Real / test'` - integration tests 
 

Compile app with `rake` or with `sbt jsbuild` command

## And then...

1. clone repo
2. run sbt
3. for work with UI need install web-dependencies (with `sbt install`) and coffeescript
4. for backend part just open in your IDE and add feature, fix bugs
5. create pull request

# License: MIT




