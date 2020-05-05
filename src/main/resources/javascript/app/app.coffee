`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"
    "plugins:load": controller: PluginsController, action: "load"
    "settings:load": controller: SettingsController, action: "load"
    "sources:fetch": controller: SourcesController, action: "fetch_unread"
    "ws:new": controller: WSController, action: "fresh"
    "ws:notify": controller: WSController, action: "notify"
    "/" : controller: MainController, action: "view"
    "/favorites" : controller: FeedsController, action: "favorites"
    "/plugins" : controller: PluginsController, action: "show"
    "/about" : controller: MainController, action: "about"
    "/show/:source-name/:feed-name": controller: FeedsController, action: "show"
    "/show/:source-name" : controller: SourcesController, action: "show"
    "/show/:source-name/page/:page" : controller: SourcesController, action: "show"
    "/show/feeds/content/:feed": controller: FeedsController, action: "view_content"
    "/opml": controller: SourcesController, action: "download"
    "/settings" : controller: SettingsController, action: "show"
    #"click a.feed-link": controller: FeedsController, action: "view0", data: "data-feed-id"
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
    "input #search": controller: SourcesController, action: "filter"
#    "mouseenter .tippy-count": controller: FeedsController, action: 'draw_tooltip', data: "data-source-id"

  setup =
    enable_logging: true
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
    