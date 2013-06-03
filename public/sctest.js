
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
   };
   $scope.edit = function()
   {
     $scope.is_editing = true;
   }

   $scope.set = function()
   {
     $scope.is_editing = false;
   }


   $scope.addPattern = function(rules)
   {
     rules.push({ pattern : "" });
   }

   $scope.removePattern = function(rules, rule)
   {
     destroy(rule, rules);
   }

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
