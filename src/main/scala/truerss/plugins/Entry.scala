package truerss.plugins

import java.util.Date

case class Entry(
  url: String,
  title: String,
  author: String,
  publishedDate: Date,
  description: Option[String],
  content: Option[String]
) {
  override def toString = {
    s"""Entry[url = ${url}, title=${title}; author=${author};date=${publishedDate};
        description=${description};
        content=$content
    ]"""
  }
}
