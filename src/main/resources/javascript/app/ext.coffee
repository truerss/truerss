
Sirius.View.register_strategy('html',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    if attribute == 'text'
      $(element).html(result)
    else
      throw new Error("Html strategy work only for text, not for #{attribute}")
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