package net.truerss

import java.time.LocalDateTime
import java.util.{Date, Random, UUID}

import truerss.db.{Feed, Source, SourceStates}
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, FeedDto, NewSetup, NewSourceDto, SourceViewDto, UpdateSourceDto}

object Gen {
  import truerss.util.CommonImplicits._

  def genId = UUID.randomUUID().toString

  def now: LocalDateTime = LocalDateTime.now().withNano(0)

  def genName(xs: Vector[String]): String = {
    val name: String = genName
    if (xs.contains(name))
      genName(xs)
    else
      name
  }

  def genUrl(xs: Vector[String]): String = {
    val url: String = genUrl
    if (xs.contains(url))
      genUrl(xs)
    else
      url
  }

  def genName = genId

  def genUrl = s"http://example${genId.replaceAll("-", "")}.com"

  def genAuthor = genId

  def genText = Vector.fill(10)(genId).mkString("\n")

  def genInt = new Random().nextInt(11) + 1

  def genLong: Long = new Date().toInstant.toEpochMilli

  def tOf = if (genInt / 2 == 0) true else false

  def genNewSource: NewSourceDto = {
    val name = genName
    NewSourceDto(
      url = genUrl,
      name = name,
      interval = genInt
    )
  }

  def genUpdSource(id: Long): UpdateSourceDto = {
    UpdateSourceDto(
      id = id,
      url = genUrl,
      name = genName,
      interval = genInt
    )
  }

  def genSource(id: Option[Long] = None) = {
    val name = genName
    val z = LocalDateTime.now()

    Source(id = id,
      url = genUrl,
      name = name,
      interval = genInt,
      state = SourceStates.Neutral,
      normalized = name.normalize,
      lastUpdate = now.plusDays(1)
    )
  }

  def genView: SourceViewDto = {
    val n = genName
    SourceViewDto(
      id = genInt.toLong,
      url = genUrl,
      name = n,
      interval = genInt,
      state = SourceStates.Neutral,
      normalized = n.normalize,
      lastUpdate = now
    )
  }

  def genFeed(sourceId: Long, sourceUrl: String) = {
    val title = genName
    Feed(
      id = None,
      sourceId = sourceId,
      url = s"$sourceUrl/feed/$genId",
      title = title,
      author = genAuthor,
      publishedDate = now,
      description = Some(genText),
      content = None,
      normalized = title.normalize,
      favorite = tOf,
      read = tOf,
      delete = false
    )
  }

  def genFeedDto: FeedDto = {
    FeedDto(
      id = 1,
      sourceId = 1,
      url = genUrl,
      title = Gen.genName,
      author = Gen.genName,
      publishedDate = now,
      description = None,
      content = None,
      normalized = Gen.genName
    )
  }

  def genSetup: AvailableSetup[Int] = {
    AvailableSetup(
      key = Gen.genName,
      description = Gen.genName,
      options = AvailableSelect(Iterable(1, 2, 3)),
      value = CurrentValue(1)
    )
  }

  def genNewSetup: NewSetup[Int] = {
    NewSetup(Gen.genName, CurrentValue(1))
  }

}
