var sparkline =
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.sparkline = sparkline;

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }

function getY(max, height, diff, value) {
  var y = parseFloat((height - value * height / max + diff).toFixed(2));
  return isNaN(y) ? height : y;
}

function removeChildren(svg) {
  [].concat(_toConsumableArray(svg.querySelectorAll("*"))).forEach(function (element) {
    return svg.removeChild(element);
  });
}

function defaultFetch(entry) {
  return entry.value;
}

function buildElement(tag, attrs) {
  var element = document.createElementNS("http://www.w3.org/2000/svg", tag);

  for (var name in attrs) {
    element.setAttribute(name, attrs[name]);
  }

  return element;
}

function sparkline(svg, entries, options) {
  //removeChildren(svg);

  if (entries.length <= 1) {
    return;
  }

  options = options || {};

  if (typeof entries[0] === "number") {
    entries = entries.map(function (entry) {
      return { value: entry };
    });
  }

  // This function will be called whenever the mouse moves
  // over the SVG. You can use it to render something like a
  // tooltip.
  var onmousemove = options.onmousemove;

  // This function will be called whenever the mouse leaves
  // the SVG area. You can use it to hide the tooltip.
  var onmouseout = options.onmouseout;

  // Should we run in interactive mode? If yes, this will handle the
  // cursor and spot position when moving the mouse.
  var interactive = "interactive" in options ? options.interactive : !!onmousemove;

  // Define how big should be the spot area.
  var spotRadius = options.spotRadius || 2;
  var spotDiameter = spotRadius * 2;

  // Define how wide should be the cursor area.
  var cursorWidth = options.cursorWidth || 2;

  // Get the stroke width; this is used to compute the
  // rendering offset.
  var strokeWidth = parseFloat(svg.attributes["stroke-width"].value);

  // By default, data must be formatted as an array of numbers or
  // an array of objects with the value key (like `[{value: 1}]`).
  // You can set a custom function to return data for a different
  // data structure.
  var fetch = options.fetch || defaultFetch;

  // Retrieve only values, easing the find for the maximum value.
  var values = entries.map(function (entry) {
    return fetch(entry);
  });

  // The rendering width will account for the spot size.
  var width = parseFloat(svg.attributes.width.value) - spotDiameter * 2;

  // Get the SVG element's full height.
  // This is used
  var fullHeight = parseFloat(svg.attributes.height.value);

  // The rendering height accounts for stroke width and spot size.
  var height = fullHeight - strokeWidth * 2 - spotDiameter;

  // The maximum value. This is used to calculate the Y coord of
  // each sparkline datapoint.
  var max = Math.max.apply(Math, _toConsumableArray(values));

  // Some arbitrary value to remove the cursor and spot out of
  // the viewing canvas.
  var offscreen = -1000;

  // Cache the last item index.
  var lastItemIndex = values.length - 1;

  // Calculate the X coord base step.
  var offset = width / lastItemIndex;

  // Hold all datapoints, which is whatever we got as the entry plus
  // x/y coords and the index.
  var datapoints = [];

  // Hold the line coordinates.
  var pathY = getY(max, height, strokeWidth + spotRadius, values[0]);
  var pathCoords = "M" + spotDiameter + " " + pathY;

  values.forEach(function (value, index) {
    var x = index * offset + spotDiameter;
    var y = getY(max, height, strokeWidth + spotRadius, value);

    datapoints.push(Object.assign({}, entries[index], {
      index: index,
      x: x,
      y: y
    }));

    pathCoords += " L " + x + " " + y;
  });

  var path = buildElement("path", {
    d: pathCoords,
    fill: "none"
  });

  svg.appendChild(path);
}

exports.default = sparkline;

/***/ })
/******/ ]);
//# sourceMappingURL=sparkline.js.map