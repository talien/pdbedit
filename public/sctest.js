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

   $scope.ruleset_names = [] // meh, it's the ruleset name list, rename it ASAP

   $scope.ruleset_editor = {
       ruleset : create_empty_ruleset(''),
       show : false,
       is_rule_editing : false,
       is_editing : false,
   }

   $scope.add_ruleset = function(ruleset_name, ruleset_editor)
   {
      ruleset_editor.ruleset = create_empty_ruleset(ruleset_name)
      ruleset_editor.ruleset_show = true;
      ruleset_editor.is_rule_editing = false;
      ruleset_editor.is_editing = false;
      $scope.ruleset_names.push(ruleset_name);
   };

    $scope.open_add_ruleset_dialog = function()
    {
      var d = $dialog.dialog($scope.add_ruleset_opts);
      d.open().then(function(result){
        if(result)
        {
           $scope.add_ruleset(result, $scope.ruleset_editor);
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

   $scope.load_ruleset = function(ruleset_name, ruleset_editor)
   {
      $http.get("ruleset/"+rname).then(function(res) {
         ruleset_editor.ruleset = res.data;
      });
     
      ruleset_editor.ruleset_show = true;
      ruleset_editor.is_rule_editing = false;
      ruleset_editor.is_editing = false;
   };

   $scope.on_save_ruleset = function(ruleset_editor)
   {
      $http.put("ruleset/" + ruleset_editor.ruleset.ruleset_name, ruleset_editor.ruleset);
   };

   $scope.on_delete_ruleset = function(ruleset_editor)
   {
      $http.delete("ruleset/" + ruleset_editor.ruleset.ruleset_name, ruleset_editor.ruleset).then(function(res)
      {
        $scope.refresh_name_list()
      });
      ruleset_editor.ruleset_show = false;
      ruleset_editor.is_rule_editing = false;
      ruleset_editor.is_editing = false;
   };

   $scope.edit = function(ruleset_editor)
   {
     ruleset_editor.is_editing = true;
   }

   $scope.set = function(ruleset_editor)
   {
     ruleset_editor.is_editing = false;
   }

   $scope.edit_rule = function(ruleset_editor)
   {
     ruleset_editor.is_rule_editing = true;
   }

   $scope.set_rule = function(ruleset_editor)
   {
     ruleset_editor.is_rule_editing = false;
   }

   $scope.add_rule = function(ruleset_editor)
   {
     ruleset_editor.ruleset.rules.push( { provider: "", rule_class: "", patterns: Array(), tags: Array(), id: uuid() });
     ruleset_editor.is_rule_editing = true;
   }

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

    $scope.parser_select = [ {name:"QSTRING"}, {name:"ESTRING"}, {name:"STRING"}, {name:"ANYSTRING"}, {name: "DOUBLE"}, {name:"EMAIL"}, {name:"FLOAT"}, {name:"HOSTNAME"},
                             {name:"IPv4"}, {name:"IPv6"}, {name: "IPvANY"}, {name:"LLADDR"}, {name:"MACADDR"}, {name:"NUMBER"}, {name:"PCRE"}, {name:"SET"} ];

    $scope.test_change = function(model, evt)
    {
        var index = parseInt(evt.target.id.split("_")[1]);
        $scope.index = index;
        $scope.token = $scope.logelements[index];
        if ($scope.token.type === 'delimiter')
           return;
        $scope.editing = true;
        $scope.selected_parser = { "name":"" };
        $scope.parser_variable = "";
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
