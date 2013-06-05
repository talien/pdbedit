function uuid() {
  var s = [], itoh = '0123456789abcedf';
 
  // Make array of random hex digits. The UUID only has 32 digits in it, but we
  // allocate an extra items to make room for the '-'s we'll be inserting.
  for (var i = 0; i <36; i++) s[i] = Math.floor(Math.random()*0x10);
 
  // Conform to RFC-4122, section 4.4
  s[14] = 4;  // Set 4 high bits of time_high field to version
  s[19] = (s[19] & 0x3) | 0x8;  // Specify 2 high bits of clock sequence
 
  // Convert to hex chars
  for (var i = 0; i <36; i++) s[i] = itoh[s[i]];
 
  // Insert '-'s
  s[8] = s[13] = s[18] = s[23] = '-';
 
  return s.join('');
}

function destroy(val, arr) {
    for (var i = 0; i < arr.length; i++) if (arr[i] === val) arr.splice(i, 1);
}

scmodule = angular.module('sctest', ['ui.bootstrap']);

var qstring_attribute_function = function(token)
{
   return token.charAt(0) + token.charAt(token.length-1);
};

var is_parser = function(token)
{
   return (token.length > 4) && (token.charAt(0) === "@") && (token.charAt(1) !== "@") && (token.charAt(token.length-1) === "@") && (token.charAt(token.length-2) !== "@");
};

var populate_parser_attributes_from_token = function(scope, token)
{
   var str = token.substring(1, token.length - 1 );
   parts = str.split(":");
   scope.selected_parser = { "name" : parts[0] };
   scope.parser_variable = parts[1];
   scope.parser_attributes = parts[2];
};

var is_delimiter = function(character, delimiters)
{
   for (var i=0;i<delimiters.length;i++)
   {
      if (delimiters[i] === character)
      {
         return true;
      }
   }
   return false;
};

var split_by_delimiters = function(str, delimiters)
{
   var res = Array();
   var index = 0;
   var is_in_parser = false;
   var current_token = "";
   for (var i=0; i<str.length; i++)
   {
      if (str[i] == '@')
      {
         is_in_parser = !is_in_parser;
      };

      if (!is_in_parser && is_delimiter(str[i], delimiters))
      {
         if (current_token !== "")
         {
           res.push({ value: current_token, index: index, type: "token"});
           current_token = "";
           index++;
         }
         res.push({ value: str[i], index: index, type: "delimiter"});
         index++;
      }
      else
      {
         current_token = current_token + str[i];
      }
   }
   if (current_token !== "")
   {
      res.push({ value: current_token, index: index, type: "token"});
   };
   return res;
};

var tokenize = function (str)
{
   return split_by_delimiters(str, " -;");
};

var join = function(param)
{
   if (typeof(param) == 'undefined' || param == null) {
        return '';
   }
   var res = "";
   for (var i = 0; i < param.length; i++)
   {
      res = res + param[i].value;
   };
   return res;
};

scmodule.controller("scload", function($scope, $http) {
   $http.get("namelist").then(function(res) {
       $scope.test_data = res.data;
   });

   $scope.loadruleset = function(rname)
   {
      $http.get("ruleset/"+rname).then(function(res) {
         $scope.ruleset = res.data;
      });
     
      $scope.ruleset_show = true;
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
     rules.push({ pattern : "" });
   }

   $scope.addTags = function(tags)
   {
     tags.push("");
   }

   $scope.removePattern = function(rules, rule)
   {
     destroy(rule, rules);
   }

   $scope.openEditor = function (pattern)
   {
     $scope.editing_pattern = pattern;
     $scope.is_pattern_editor_open = true;
     $scope.logelements = tokenize($scope.editing_pattern.pattern);
   }

   $scope.cancelEditor = function ()
   {
     $scope.is_pattern_editor_open = false;
   }

   $scope.saveEditor = function()
   {
     $scope.is_pattern_editor_open = false;
     $scope.editing_pattern.pattern = join($scope.logelements);
   }

   $scope.modal_opts = {
      backdropFade: true,
      dialogFade:true,
      backdrop: true,
      dialogClass : "modal container span13"
   };

    $scope.parser_select = [ {"name":"QSTRING"}, {"name":"ESTRING"}, {"name":"STRING"}];

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

