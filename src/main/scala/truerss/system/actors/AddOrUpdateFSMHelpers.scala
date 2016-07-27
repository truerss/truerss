package truerss.system.actors

import truerss.models.Source

trait AddOrUpdateFSMHelpers {
  def urlError(source: Source) = s"Url '${source.url}' already present in db"
  def nameError(source: Source) = s"Name '${source.name}' not unique"
}
