enrich_model = (data, constructor, propagate) ->
  model = new constructor()
  angular.extend(model, data)
  model.enrich() if propagate
  return model

class Rule
    constructor : () ->
        @provider = ""
        @rule_class = ""
        @patterns = Array()
        @tags = Array()
        @values = Array()
        @id = uuid()

    add_value : () =>
        @values.push(
           name : ""
           value : ""
        )

class Ruleset
    constructor : (name) ->
        @name = name
        @id = uuid()
        @description = ""
        @url = ""
        @patterns = []
        @rules = []
        @tags = []

    test : () ->
        alert 42

    enrich : () ->
        new_rules = []
        angular.forEach(@rules, (obj) ->
            new_rules.push(enrich_model(obj, Rule, false)))
        @rules = new_rules

class RulesetEditor
    constructor : (http) ->
        @http = http
        @ruleset = new Ruleset('')
        @show = false
        @is_rule_editing = false
        @is_editing = false

    add_ruleset: (ruleset_name) ->
        @ruleset = new Ruleset(ruleset_name)
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

    enrich_ruleset : (data) ->
        return enrich_model(data, Ruleset, true)

    load_ruleset : (ruleset_name) ->
        @http.get("ruleset/"+ruleset_name).then( (res) =>
           @ruleset = @enrich_ruleset(res.data)
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
        @ruleset.rules.push(new Rule())
        @is_rule_editing = true
