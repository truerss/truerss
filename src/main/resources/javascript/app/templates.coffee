# old ejs interface
class EJS
  constructor: (@_template) ->

  render: (params) ->
    ejs.render(@_template, params)


Templates =
  source_list: null
  feeds_list: null
  source_list_view: new Sirius.View("#source-list")
  article_view: new Sirius.View("#main")
  modal_element: "#add-modal"
  modal_view: new Sirius.View("#upload-modal")
  feeds_view: new Sirius.View("#feeds")               # deprecated
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

