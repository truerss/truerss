# old ejs interface
class EJS
  constructor: (@_template) ->

  render: (params) ->
    ejs.render(@_template, params)


Templates =
  source_list: null
  list: null
  feeds_list: null
  all_sources_template: null
  source_list_view: new Sirius.View("#source-list")
  feed_template: null
  article_view: new Sirius.View("#main")
  modal_element: "#add-modal"
  modal_view: new Sirius.View("#add-modal")
  feeds_view: new Sirius.View("#feeds")
  feeds_template: null
  favorites_template: null
  plugins_template: null
  pagination_view: new Sirius.View("#pagination")

