`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"
    "ws:new": controller: WSController, action: "fresh"
    "ws:create": controller: WSController, action: "create"
    "ws:deleted": controller: WSController, action: "deleted"
    "ws:updated": controller: WSController, action: "updated"
    "/sources" : controller: SourcesController, action: "all"
    "/" : controller: MainController, action: "view"
    "/favorites" : controller: FeedsController, action: "favorites"
    "/plugins" : controller: MainController, action: "plugin_list"
    "/about" : controller: MainController, action: "about"
    "/show/:source-name/:feed-name": controller: FeedsController, action: "show"
    "/show/:source-name" : controller: SourcesController, action: "show"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"
    "click i.favorite": controller: FeedsController, action: "favorite", data: "data-favorite"
    "click i.unfavorite": controller: FeedsController, action: "unfavorite", data: "data-favorite"
    "click a[href='#update-source']" : controller: SourcesController, action: "refresh_one", data: "data-source-id"
    "click a[href='#delete-source']": controller: SourcesController, action: "remove", data: "data-source-id"
    "click a[href='#edit-source']": controller: SourcesController, action: "edit", data: "data-source-id"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    mix_logger_into_controller: true
    controller_wrapper: ControllerStatesExt
    log: false