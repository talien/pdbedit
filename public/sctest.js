scmodule = angular.module('sctest', ['ui.bootstrap']);

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

   $scope.add_ruleset = function(ruleset_name)
   {
      $scope.ruleset = { 
         name: ruleset_name,
         id : uuid(),
         patterns : [],
         rules : [],
         tags : [],
  
      };
      $scope.ruleset_show = true;
      $scope.is_rule_editing = false;
      $scope.is_editing = false;
      $scope.test_data.push(ruleset_name);
   };

    $scope.open_add_ruleset_dialog = function()
    {
      var d = $dialog.dialog($scope.add_ruleset_opts);
      d.open().then(function(result){
        if(result)
        {
           $scope.add_ruleset(result);
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
       $scope.test_data = res.data;
     });
   }
   
   $scope.refresh_name_list()

   $scope.loadruleset = function(rname)
   {
      $http.get("ruleset/"+rname).then(function(res) {
         $scope.ruleset = res.data;
      });
     
      $scope.ruleset_show = true;
      $scope.is_rule_editing = false;
      $scope.is_editing = false;
   };

   $scope.on_save_ruleset = function(ruleset_name)
   {
      $http.put("ruleset/" + ruleset_name, $scope.ruleset);
   };

   $scope.on_delete_ruleset = function(ruleset_name)
   {
      $http.delete("ruleset/" + ruleset_name, $scope.ruleset).then(function(res)
      {
        $scope.refresh_name_list()
      });
      $scope.ruleset_show = false;
      $scope.is_rule_editing = false;
      $scope.is_editing = false;
   };

   $scope.edit = function()
   {
     $scope.is_editing = true;
   }

   $scope.set = function()
   {
     $scope.is_editing = false;
   }

   $scope.edit_rule = function()
   {
     $scope.is_rule_editing = true;
   }

   $scope.set_rule = function()
   {
     $scope.is_rule_editing = false;
   }

   $scope.add_rule = function(rules)
   {
     rules.push( { provider: "", rule_class: "", patterns: Array(), tags: Array(), id: uuid() });
     $scope.is_rule_editing = true;
   }

   $scope.addPattern = function(rules)
   {
     rules.push({ text : "" });
   }

   $scope.addTags = function(tags)
   {
     tags.push({ text: ""});
   }

   $scope.removePattern = function(rules, rule)
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
