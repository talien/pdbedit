uuid  = () ->
  s = []
  itoh = '0123456789abcedf'
 
  # Make array of random hex digits. The UUID only has 32 digits in it, but we
  # allocate an extra items to make room for the '-'s we'll be inserting.
  for i in [0..36]
      s[i] = Math.floor(Math.random()*0x10)
 
  # Conform to RFC-4122, section 4.4
  s[14] = 4  # Set 4 high bits of time_high field to version
  s[19] = (s[19] & 0x3) | 0x8  # Specify 2 high bits of clock sequence
 
  # Convert to hex chars
  for i in [0..36]
      s[i] = itoh[s[i]]
 
  # Insert '-'s
  s[8] = s[13] = s[18] = s[23] = '-'
 
  s.join ""

remove_from_array = (item, arr) ->
  for i in [0..arr.length]
      arr.splice(i, 1) if arr[i] is item

qstring_attribute_function = (token) ->
  token.charAt(0) + token.charAt(token.length - 1)

is_parser = (token) ->
  (token.length >= 4) and (token.charAt(0) is "@") and (token.charAt(1) isnt "@") and (token.charAt(token.length - 1) is "@") and (token.charAt(token.length - 2) isnt "@")

populate_parser_attributes_from_token = (scope, token) ->
  str = token.substring(1, token.length - 1)
  parts = str.split(":")
  scope.selected_parser = name: parts[0]
  scope.parser_variable = parts[1]
  scope.parser_attributes = parts[2]

class Tokenizer
  constructor: (@delimiters) ->
  
  is_delimiter: (character) =>
    i = 0

    while i < @delimiters.length
      return true  if @delimiters[i] is character
      i++
    false

  add_item : (value, type) ->
    @res.push
      value:value
      index:@index
      type:type
    @index++

  add_token : (value) ->
    @add_item(value, "token")
   
  add_delimiter : (value) ->
    @add_item(value, "delimiter")

  add_parser : (value) ->
    @add_item(value, "parser")

  add_current_token: () ->
    if @current_token isnt ""
      @add_token @current_token
      @current_token = ""

  add_current_token_as_parser: () ->
    if @current_token isnt ""
      @add_parser @current_token
      @current_token = ""

  extend_current_token_with: (chr) ->
    @current_token = @current_token + chr

  is_parser_special: (chr) ->
    chr is "@"

  split : (str) ->
    @res = Array()
    @index = 0
    is_in_parser = false
    @current_token = ""
    i = 0

    while i < str.length
      if is_in_parser and @is_parser_special str[i]
        @extend_current_token_with str[i]
        @add_current_token_as_parser()
        is_in_parser = false
      else
        if @is_parser_special str[i]
          is_in_parser = true
          @add_current_token()
        if not is_in_parser and @is_delimiter str[i]
          @add_current_token()
          @add_delimiter str[i]
        else
          @extend_current_token_with str[i]
      i++
    @add_current_token()
    @res

split_by_delimiters = (str, delimiters) ->
  tokenizer = new Tokenizer(delimiters)
  tokenizer.split(str)

tokenize = (str) ->
  split_by_delimiters str, " -;"

join = (param) ->
  return ""  if typeof (param) is "undefined" or not param?
  res = ""
  i = 0

  while i < param.length
    res = res + param[i].value
    i++
  res
