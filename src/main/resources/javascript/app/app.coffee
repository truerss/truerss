`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"
    "/sources" : controller: SourcesController, action: "all"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    controller_wrapper: {
      redirect: Sirius.redirect
      log: (msg) -> console.log(msg)
    }
    log: false