# old ejs interface
class EJS
  constructor: (@_template) ->

  render: (params) ->
    ejs.render(@_template, params)


Templates =
  source_list: null
  feeds_list: null
  sources_all_view: new Sirius.View("#source-all") # li
  source_list_view: new Sirius.View("#source-list")
  article_view: new Sirius.View("#main")
  modal_view: new Sirius.View("#upload-modal")
  favorites_template: null
  plugins_template: null
  plugins_view: new Sirius.View("#plugins-modal")
  pagination_view: new Sirius.View("#pagination")
  tippy_template: null
  settings_template: null
  settings_view: new Sirius.View("#settings-modal")
  source_overview_template: null
  source_overview_view: new Sirius.View("#source-overview")
  about_view: new Sirius.View("#about-modal")
  about_template: null
  short_view_feeds_list_template: null

