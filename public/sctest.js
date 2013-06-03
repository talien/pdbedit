
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
});
