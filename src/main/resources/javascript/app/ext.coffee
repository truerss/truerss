
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

Sirius.View.register_strategy('toggle',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    klass = "count-hidden"
    count = parseInt(result)
    if count == 0 || isNaN(count)
      $(element).addClass(klass)
    else
      if $(element).hasClass(klass)
        $(element).removeClass(klass)
      adapter.swap(element, result)
)

Sirius.View.register_strategy('sum',
  transform: (oldvalue, newvalue) ->
    oldvalue = parseInt(oldvalue, 10)
    newvalue = parseInt(newvalue, 10)
    if isNaN(oldvalue)
      newvalue
    else
      newvalue + oldvalue

  render: (adapter, element, result, attribute) ->
    hidden = "uk-hidden"
    if parseInt(result) <= 0
      $(element).addClass(hidden)
    else
      if $(element).hasClass(hidden)
        $(element).removeClass(hidden)
    adapter.swap(element, result)
)


class UrlValidator extends Sirius.Validator

  validate: (url, attrs) ->
    return false unless url
    re = /^(http[s]?:\/\/){0,1}(www\.){0,1}[a-zA-Z0-9\.\-]+\.[a-zA-Z]{2,5}[\.]{0,1}/
    if !re.test(url)
      @msg = "Url '#{url}' is not valid"
      false
    else
      true

Sirius.BaseModel.register_validator("url_validator", UrlValidator)

String::htmlize = () ->
  @replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/&/g,'&amp;')
  .replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&')
  .replace(/@/,'`at`')

String::camelize = () ->
  @split(/(?=[A-Z])/).join('_').toLowerCase()

Array::group_by = (key) ->
  o = {}
  for a in @
    k = a[key]
    if o[k]
      o[k].push(a)
    else
      o[k] = [a]
  o

Array::each_cons = (num) ->
  Array.from(
    {length: @length - num + 1},
    (_, i) => @slice(i, i + num)
  )

Array::uniq = () ->
  Array.from(new Set(@))

Array::add_to = (el) ->
  if @length == 0 || @[@length-1] != el
    @push(el)
  @

Array::contains = (el) ->
  @indexOf(el) != -1

Array::first = () ->
  @[0]

Array::last = () ->
  @[@.length - 1]

Array::is_empty = () ->
  @length == 0

String::contains = (str) ->
  @indexOf(str) != -1

String::is_empty = () ->
  @length == 0