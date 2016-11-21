/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

angular.module(PKG.name + '.commons')
  .directive('myMapWidget', function() {
    return {
      restrict: 'E',
      scope: {
        model: '=ngModel',
        config: '=',
        isDropdown: '='
      },
      templateUrl: 'widget-container/widget-map/widget-map.html',
      controller: function($scope, myHelpers) {
        $scope.keyPlaceholder = myHelpers.objectQuery($scope.config, 'widget-attributes', 'key-placeholder') || 'key';
        $scope.valuePlaceholder = myHelpers.objectQuery($scope.config, 'widget-attributes', 'value-placeholder') || 'value';

        // initializing
        function initialize() {
          var map = $scope.model;
          $scope.properties = [];

          if (!map || Object.keys(map).length === 0) {
            $scope.properties.push({
              key: '',
              value: ''
            });
            return;
          }

          Object.keys(map).forEach((key) => {
            $scope.properties.push({
              key: key,
              value: map[key]
            });
          });
        }

        initialize();

        $scope.$watch('properties', function() {

          var map = {};

          angular.forEach($scope.properties, function(p) {
            if(p.key.length > 0){
              map[p.key] = p.value;
            }
          });
          $scope.model = map;
        }, true);

        $scope.addProperty = function() {
          $scope.properties.push({
            key: '',
            value: '',
            newField: 'add'
          });
        };

        $scope.removeProperty = function(property) {
          var index = $scope.properties.indexOf(property);
          $scope.properties.splice(index, 1);
        };

        $scope.enter = function (event, last) {
          if (last && event.keyCode === 13) {
            $scope.addProperty();
          }
        };
      }
    };
  });
