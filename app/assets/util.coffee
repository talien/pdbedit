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
  for i in [0, arr.length]
      arr.splice(i, 1) if arr[i] is item

qstring_attribute_function = (token) ->
  token.charAt(0) + token.charAt(token.length - 1)

is_parser = (token) ->
  (token.length > 4) and (token.charAt(0) is "@") and (token.charAt(1) isnt "@") and (token.charAt(token.length - 1) is "@") and (token.charAt(token.length - 2) isnt "@")

populate_parser_attributes_from_token = (scope, token) ->
  str = token.substring(1, token.length - 1)
  parts = str.split(":")
  scope.selected_parser = name: parts[0]
  scope.parser_variable = parts[1]
  scope.parser_attributes = parts[2]

is_delimiter = (character, delimiters) ->
  i = 0

  while i < delimiters.length
    return true  if delimiters[i] is character
    i++
  false

split_by_delimiters = (str, delimiters) ->
  res = Array()
  index = 0
  is_in_parser = false
  current_token = ""
  i = 0

  while i < str.length
    is_in_parser = not is_in_parser  if str[i] is "@"
    if not is_in_parser and is_delimiter(str[i], delimiters)
      if current_token isnt ""
        res.push
          value: current_token
          index: index
          type: "token"

        current_token = ""
        index++
      res.push
        value: str[i]
        index: index
        type: "delimiter"

      index++
    else
      current_token = current_token + str[i]
    i++
  if current_token isnt ""
    res.push
      value: current_token
      index: index
      type: "token"

  res

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
