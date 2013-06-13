create_empty_ruleset = (ruleset_name) ->
  ruleset =
     name: ruleset_name
     id : uuid()
     patterns : []
     rules : []
     tags : []

class RulesetEditor
    constructor : (http) ->
        @http = http
        @ruleset = create_empty_ruleset('')
        @show = false
        @is_rule_editing = false
        @is_editing = false

    add_ruleset: (ruleset_name) ->
        @ruleset = create_empty_ruleset(ruleset_name)
        @ruleset_show = true
        @is_rule_editing = false
        @is_editing = false


    delete_ruleset : (callback) ->
   
        @http.delete("ruleset/" + @ruleset.name).then((res) ->
           callback()
        )
        @ruleset_show = false
        @is_rule_editing = false
        @is_editing = false

    load_ruleset : (ruleset_name)->
        @http.get("ruleset/"+ruleset_name).then( (res) ->
           @ruleset = res.data
        )

        @ruleset_show = true
        @is_rule_editing = false
        @is_editing = false

    save_ruleset : () ->
        @http.put("ruleset/" + @ruleset.name, @ruleset)

    edit : () ->
        @is_editing = true

    set : () ->
        @is_editing = false

    edit_rule : () ->
        @is_rule_editing = true

    set_rule : () ->
        @is_rule_editing = false
    
    add_rule : () ->
        @ruleset.rules.push(
            provider: ""
            rule_class: ""
            patterns: Array()
            tags: Array()
            id: uuid()
        )
        @is_rule_editing = true
     
