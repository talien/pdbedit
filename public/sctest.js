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
   }

   $scope.closeEditor = function ()
   {
     $scope.is_pattern_editor_open = false;
   }

   $scope.modal_opts = {
      backdropFade: true,
      dialogFade:true,
      backdrop: true
   };

});

/*scmodule.directive ('unfocus', function() { return {
  restrict: 'A',
  link: function (scope, element, attribs) {
      
    element[0].focus();
      
    element.bind ("blur", function() {
        scope.$apply(attribs["unfocus"]);
        //console.log("??");
      });
                                   
  } 
    
 } 
});

function sccontrol($scope) {
    $scope.name= "Freewind";

    $scope.enableEdit = function() { $scope.edit = true; }
    $scope.disableEdit = function() { $scope.edit = false;  }

}*/
