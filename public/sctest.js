scmodule = angular.module('sctest', ['ui.bootstrap']);

create_empty_ruleset = function(ruleset_name)
{
 var ruleset = { 
     name: ruleset_name,
     id : uuid(),
     patterns : [],
     rules : [],
     tags : [],

  };
 return ruleset;
}

scmodule.directive('onEnter', function() {
        return function(scope, element, attrs) {
            element.bind("keydown keypress", function(event) {
                if(event.which === 13) {
                    scope.$apply(function(){
                        scope.$eval(attrs.onEnter);
                    });

                    event.preventDefault();
                }
            });
        };
    });

scmodule.controller("search", function($scope, $http) {
   $scope.from = 0;
   $scope.loading = false;
   $scope.query = "";

   $scope.getmessages = function() {
       $scope.loading = true;
       $http.post("/elastictest/"+ $scope.from, { 'query':$scope.query }, { headers: { 'Content-Type':'application/json' } } ).then(function(res){
           $scope.messages = res.data;
           $scope.loading = false;
       })
   }

   $scope.next = function()
   {
       $scope.from = $scope.from + 50;
       $scope.getmessages();
   }

   $scope.prev = function()
   {
       if (($scope.from - 50) < 0)
       {
          $scope.from = 0;
          $scope.getmessages();
       }
       else
       {
          $scope.from = $scope.from - 50;
          $scope.getmessages();
       }
   }

   $scope.set_message = function(message) 
   {
      $scope.display_message = message
   }

   $scope.getmessages();
});

scmodule.controller("scload", function($scope, $http, $dialog) {
   var add_ruleset_dialog_template = '<div class="modal-header">'+
          '<h1>Adding new ruleset</h1>'+
          '</div>'+
          '<div class="modal-body">'+
          '<p>Enter the new ruleset name <input ng-model="result" /></p>'+
          '</div>'+
          '<div class="modal-footer">'+
          '<button ng-click="close(result)" class="btn btn-primary" >Close</button>'+
          '</div>';


   $scope.add_ruleset_opts = {
    backdrop: true,
    keyboard: true,
    backdropClick: true,
    template:  add_ruleset_dialog_template, // OR: templateUrl: 'path/to/view.html',
    controller: 'AddRulesetDialogController',
    dialogClass: 'modal span7',
   };
   
   $scope.is_search_active = true;

   $scope.ruleset_names = [] // meh, it's the ruleset name list, rename it ASAP

   $scope.ruleset_editor = new RulesetEditor($http);

   $scope.add_ruleset_and_edit = function(ruleset_name, ruleset_editor)
   {
      $scope.ruleset_editor.add_ruleset(ruleset_name)
      $scope.ruleset_names.push(ruleset_name);
   };

    $scope.open_add_ruleset_dialog = function()
    {
      var d = $dialog.dialog($scope.add_ruleset_opts);
      d.open().then(function(result){
        if(result)
        {
           $scope.add_ruleset_and_edit(result, $scope.ruleset_editor);
        }
      });
   };

   $scope.on_add_ruleset = function()
   {
      $scope.open_add_ruleset_dialog();
   }

   $scope.refresh_name_list = function()
   {
     $http.get("namelist").then(function(res) {
       $scope.ruleset_names = res.data;
     });
   }
   
   $scope.refresh_name_list()

   $scope.add_string_object = function(items)
   {
     items.push({ text : "" });
   }

   $scope.remove_object = function(rules, rule) //Wehehe, what is this?
   {
     remove_from_array(rule, rules);
   }

   $scope.openEditor = function (pattern)
   {
     $scope.editing_pattern = pattern;
     $scope.is_pattern_editor_open = true;
     $scope.logelements = tokenize($scope.editing_pattern.text);
   }

   $scope.cancelEditor = function ()
   {
     $scope.is_pattern_editor_open = false;
   }

   $scope.saveEditor = function()
   {
     $scope.is_pattern_editor_open = false;
     $scope.editing_pattern.text = join($scope.logelements);
   }

   $scope.modal_opts = {
      backdropFade: true,
      dialogFade:true,
      backdrop: true,
      dialogClass : "modal container span13"
   };

    $scope.parser_select = patterndb_parsers

    $scope.on_token_click = function(model, evt)
    {
        var index = parseInt(evt.target.id.split("_")[1]);
        $scope.index = index;
        $scope.token = $scope.logelements[index];
        if ($scope.token.type === 'delimiter')
           return;
        $scope.editing = true;
        $scope.selected_parser = { "name":"" };
        $scope.parser_variable = "";
        $scope.parser_attributes = "";
        if (is_parser($scope.token.value))
        {
            populate_parser_attributes_from_token($scope, $scope.token.value);
        };
    };

    $scope.save_item = function()
    {
        $scope.editing = false;
        var res = Array();
        var items = $scope.logelements;
        var index = $scope.index;
        for (var i=0;i<items.length; i++)
        {
           if (i != index)
           {
              res[i] = items[i];
           }
           else
           {
              res[i] = { "value": "@" + $scope.selected_parser.name + ":" + $scope.parser_variable+ ":" + $scope.parser_attributes + "@", "index" : index , type:'token'};
           }
        }
        $scope.logelements = res;
        $scope.parser_variable = "";
        $scope.parser_attributes = "";
    };

    $scope.cancel_item = function()
    {
        $scope.editing = false;
    };

    $scope.to_html = function(str)
    {
        return str;
    };

});


scmodule.directive('logLine', function () {
  return {
    require: 'ngModel',
    link: function(scope, element, attr, ngModelCtrl) {

      ngModelCtrl.$formatters.unshift( function (valueFromModel) {
        return join(valueFromModel);
      });

      ngModelCtrl.$parsers.push(function(valueFromInput) {
        scope.show_click_on_tokens = true;
        return tokenize(valueFromInput);
      });

    }
  };
});

function AddRulesetDialogController($scope, dialog){
  $scope.close = function(result){
    dialog.close(result);
  };
}
