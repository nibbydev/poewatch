function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }

function getY(max, height, diff, value) {
  return parseFloat((height - value * height / max + diff).toFixed(2)); 
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

function scaleValues(values) {
  let firstVal = null;
  let min = 0;

  for (let i = 0; i < values.length; i++) {
    if (values[i] !== null) {
      if (firstVal === null) {
        firstVal = values[i];
      }

      values[i] = values[i] / firstVal * 100 - 100;

      if (values[i] < min) {
        min = values[i];
      }
    }
  }
  
  if (min < 0) {
    min *= -1;

    for (let i = 0; i < values.length; i++) {
      if (values[i] !== null) {
        values[i] += min;
      }
    }
  }
}

function sparkline(svg, entries, options) {
  if (entries.length <= 1) {
    return;
  }

  options = options || {};

  entries = entries.map(function (entry) {
    return { value: entry };
  });

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

  scaleValues(values);

  // The rendering width will account for the spot size.
  var width = parseFloat(svg.attributes.width.value);

  // Get the SVG element's full height.
  // This is used
  var fullHeight = parseFloat(svg.attributes.height.value);

  // The rendering height accounts for stroke width and spot size.
  var height = fullHeight - strokeWidth * 2;

  // The maximum value. This is used to calculate the Y coord of
  // each sparkline datapoint.
  var max = Math.max.apply(Math, _toConsumableArray(values));

  // Cache the last item index.
  var lastItemIndex = values.length - 1;

  // Calculate the X coord base step.
  var offset = parseFloat((width / lastItemIndex).toFixed(2));
  
  // Hold all datapoints, which is whatever we got as the entry plus
  // x/y coords and the index.
  var datapoints = [];

  // Hold the line coordinates.
  var pathCoords = "";

  values.forEach(function (value, index) {
    if (value === null) return; //123

    var x = index * offset;
    var y = getY(max, height, strokeWidth, value);

    if (isNaN(y)) y = height; //123

    datapoints.push(Object.assign({}, entries[index], {
      index: index,
      x: x,
      y: y
    }));

    pathCoords += !pathCoords ? "M " : " L "; //123
    pathCoords += x + " " + y; //123
  });

  var path = buildElement("path", {
    class: "sparkline--line",
    d: pathCoords,
    fill: "none"
  });

  var fillCoords = !pathCoords ? "" : pathCoords + " V " + fullHeight + " L 0 " + fullHeight + " Z"; //123
  
  var fill = buildElement("path", {
    class: "sparkline--fill",
    d: fillCoords,
    stroke: "none"
  });

  svg.appendChild(fill);
  svg.appendChild(path);
}
