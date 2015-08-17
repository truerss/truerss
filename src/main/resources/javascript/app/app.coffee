`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    controller_wrapper: {
      redirect: Sirius.redirect
      log: (msg) -> console.log(msg)
    }
    log: false