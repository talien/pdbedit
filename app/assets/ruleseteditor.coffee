enrich_model = (data, constructor, propagate) ->
  model = new constructor()
  angular.extend(model, data)
  model.enrich() if propagate
  return model

class Example
    constructor : () ->
        @test_message = ""
        @test_program = ""
        @test_values = Array()
        @test_tags = Array()

    add_value : () =>
        @test_values.push(
           name : ""
           value : ""
        )

    add_tag : () =>
        @test_tags.push(
           text: ""
        )

    remove_value : (value) =>
        remove_from_array(value, @test_values)

    remove_tag : (tag) =>
        remove_from_array(tag, @test_tags)

class Rule
    constructor : () ->
        @provider = ""
        @rule_class = ""
        @patterns = Array()
        @tags = Array()
        @values = Array()
        @id = uuid()
        @examples = Array()

    add_value : () =>
        @values.push(
           name : ""
           value : ""
        )

    add_tag : () =>
        @tags.push(
           text: ""
        )

    add_pattern : () =>
        @patterns.push(
           text: ""
        )

    add_example : () =>
        @examples.push(new Example)

    remove_value : (value) =>
        remove_from_array(value, @values)

    remove_tag : (tag) =>
        remove_from_array(tag, @tags)

    remove_pattern : (pattern) =>
        remove_from_array(pattern, @patterns)

    remove_example : (example) =>
        remove_from_array(example, @examples)

    enrich : () =>
        new_examples = []
        angular.forEach(@examples, (obj) ->
            new_examples.push(enrich_model(obj, Example, false)))
        @examples = new_examples
       
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
            new_rules.push(enrich_model(obj, Rule, true)))
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
