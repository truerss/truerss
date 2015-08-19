
SourcesController =
  all: () ->
    html = Templates.all_sources_template.render({sources: Sources.all()})
    Templates.article_view.render(html).html()
    state.to(States.Sources)

