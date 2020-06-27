`var c = function(m){console.log(m);};`

$ ->

  source_overview = document.getElementById("source-overview")
  source_overview_offset_top = source_overview.offsetTop

  window.onscroll = () ->
    if window.pageYOffset > source_overview_offset_top
      source_overview.classList.add("sticky")
    else
      source_overview.classList.remove("sticky");


  routes =
    "application:run" : controller: MainController, action: "start"
    "plugins:load": controller: PluginsController, action: "load"
    "settings:load": controller: SettingsController, action: "load"
    "sources:fetch": controller: SourcesController, action: "fetch_unread"
    "sources:load": controller: SourcesController, action: "load"
    "sources:reload": controller: SourcesController, action: "reload"
    "about:load": controller: AboutController, action: "load"
    "ws:new": controller: WSController, action: "fresh"
    "ws:notify": controller: WSController, action: "notify"
    "/favorites" : controller: FeedsController, action: "favorites"
    "/favorites/search": controller: SearchController, action: "show", after: () -> scroll_to_top()
    "/favorites/search/page/:page": controller: SearchController, action: "show", after: () -> scroll_to_top()
    "/favorites/page/:page" : controller: FeedsController, action: "favorites"
    "/plugins" : controller: PluginsController, action: "show"
    "/about" : controller: AboutController, action: "show"
    "/show/all": controller: SourcesController, action: "show_all"
    "/show/all/page/:page": controller: SourcesController, action: "show_all"
    "/show/sources/:source-name" : controller: SourcesController, action: "show"
    "/show/sources/:source-name/page/:page" : controller: SourcesController, action: "show", after: () -> scroll_to_top()
    "/show/sources/:source-name/all": controller: SourcesController, action: "show_all_in_source"
    "/show/sources/:source-name/all/page/:page": controller: SourcesController, action: "show_all_in_source", after: () -> scroll_to_top()
    "/show/feeds/content/:feed": controller: FeedsController, action: "view_content"
    "/search/page/:page": controller: SearchController, action: "show", after: () -> scroll_to_top()
    "/settings" : controller: SettingsController, action: "show"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"
    "click a.favorite": controller: FeedsController, action: "favorite", data: ["data-feed-id", "data-source-id"]
    "click a.unfavorite": controller: FeedsController, action: "unfavorite", data: ["data-feed-id", "data-source-id"]
    "click a.read": controller: FeedsController, action: "read", data: ["data-feed-id", "data-source-id"]
    "click a.unread": controller: FeedsController, action: "unread", data: ["data-feed-id", "data-source-id"]
    "click a[href='#refresh-source']" : controller: SourcesController, action: "refresh_one", data: "data-source-id"
    "click a[href='#remove-source']": controller: SourcesController, action: "remove", data: "data-source-id"
    "click a[href='#edit-source']": controller: SourcesController, action: "edit", data: "data-source-id"
    "click a.source-count": controller: SourcesController, action: "mark_by_click_on_count_button", data: "data-source-id"
    "click a[href='#add-modal']": controller: UploadController, action: "upload"
    "input #search": controller: SearchController, action: "filter"

  #    "mouseenter .tippy-count": controller: FeedsController, action: 'draw_tooltip', data: "data-source-id"

  setup =
    enable_logging: false
    route: routes
    adapter: new JQueryAdapter()
    controller_wrapper: ControllerExt
    log_to: (log_level, log_source, message) ->
      msg = message.trim()
      is_define = msg.startsWith("define")
      is_set = msg.endsWith("to 'feeds'")
      is_render = msg.indexOf("Call render for") != -1

      if !is_define && !is_set && !is_render
        console.log("#{log_level} [#{log_source}] #{message}")


  Sirius.Application.run(setup)
    