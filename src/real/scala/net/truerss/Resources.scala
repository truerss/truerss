package net.truerss

import akka.actor.ActorSystem

trait Resources {
  def system: ActorSystem
  def port: Int
  def host: String
  def wsPort: Int

  def serverPort: Int

  def url: String = s"http://$host:$port"
  def rssUrl: String = s"http://$host:$serverPort/rss"
  def rssUrl1: String = s"http://$host:$serverPort/rss1"

  def opmlFile: String

  def getRssStats: Int

  def sleep = Thread.sleep(1000)

  def produceNewEntities: Unit

  def content: String
}

object Resources {
  def load(file: String, host: String, port: Int): String = {
    scala.io.Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(file)
    ).mkString
      .replaceAll("%%HOST%%", host)
      .replaceAll("%%PORT%%", s"$port")
  }
}
