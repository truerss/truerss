
Sirius.View.register_strategy('html',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    if attribute == 'text'
      $(element).html(result)
    else
      throw new Error("Html strategy work only for text, not for #{attribute}")
)

Sirius.View.register_strategy('add_class',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    $(element).addClass(result)
)

Sirius.View.register_strategy('remove_class',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    $(element).removeClass(result)
)

Sirius.View.register_strategy('hide',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    klass = "count-hidden"
    if parseInt(result) == 0
      $(element).addClass(klass)
    else
      if $(element).hasClass(klass)
        $(element).removeClass(klass)
      adapter.swap(element, result)
)


class UrlValidator extends Sirius.Validator

  validate: (url, attrs) ->
    re = /^(http[s]?:\/\/){0,1}(www\.){0,1}[a-zA-Z0-9\.\-]+\.[a-zA-Z]{2,5}[\.]{0,1}/
    if !re.test(url)
      @msg = "Url not valid"
      false
    else
      true

Sirius.BaseModel.register_validator("url_validator", UrlValidator)

String::htmlize = () ->
  @replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/&/g,'&amp;')
  .replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&')
  .replace(/@/,'`at`')
