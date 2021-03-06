LineAnnotate = Rickshaw.Class.create({

  initialize: function(args) {
    var graph = this.graph = args.graph;
    var element = this.element = document.createElement('div');
    element.className = 'line_annotation_parent';

    this.visible = true;
    graph.element.appendChild(element);

    this.lastEvent = null;
    this._addListeners();

    this.onShow = args.onShow;
    this.onHide = args.onHide;
    this.onRender = args.onRender;

    this.shortFormatter = args.shortFormatter;
    this.detailedFormatter = args.detailedFormatter;
  },

  update: function() {
    var graph = this.graph;

    var j = 0;
    var points = [];

    this.graph.series.active().forEach(function(series) {
      var data = graph.stackedData[j++];

      if (data.length < 2)
        return;

      var prev = data[0];
      data.forEach(function(value, index){
        if (index == 0)
          return;

        var point = {
          prev: prev,
          series: series,
          value: value
        };

        points.push(point);
        prev = value;
      }, this);
    }, this);

    this.element.innerHTML = '';
    this.element.style.left = '0px';

    this.visible && this.render(points);
  },

  hide: function() {
    this.visible = false;
    this.element.classList.add('inactive');

    if (typeof this.onHide == 'function') {
      this.onHide();
    }
  },

  show: function() {
    this.visible = true;
    this.element.classList.remove('inactive');

    if (typeof this.onShow == 'function') {
      this.onShow();
    }
  },

  render: function(points) {
    var graph = this.graph;
    var alignables = [];
    this.element.innerHTML = '';
    points.forEach(function(point){
      if (point.value.y === null || point.prev.y === null) return;

      var series = point.series;
      var shortFormatted = this.shortFormatter(series, point.value);
      if (shortFormatted === null) return;
      var detailedFormatted = this.detailedFormatter(series, point.value);

      var outer = document.createElement('div');
      outer.className = 'line_annotation_outer';
      var x = graph.x(point.value.x);
      outer.style.left = x + 'px';
      outer.style.top = graph.y(point.value.y0 + (point.value.y + point.prev.y) / 2) + 'px';

      var item = document.createElement('div');
      item.className = 'line_annotation';
      function displayShort() {
        item.innerHTML = shortFormatted;
        outer.classList.remove('detailed');
      }
      function displayDetailed() {
        item.innerHTML = detailedFormatted;
        outer.classList.add('detailed');
      }
      displayShort();
      item.addEventListener('mouseenter', displayDetailed);
      item.addEventListener('mouseleave', displayShort);

      outer.appendChild(item);
      this.element.appendChild(outer);

      item.classList.add('active');

      // Assume left alignment until the element has been displayed and
      // bounding box calculations are possible.
      alignables.push({
        orignalX: x,
        outer: outer,
        inner: item
      });
    }, this);

    alignables.forEach(function(el) {
      el.outer.classList.add('fromleft');
    }, false);

    this.show();

    // If left-alignment results in any error, try right-alignment.
    alignables.forEach(function(el) {
      var leftAlignError = this._calcLayoutError(el.inner);
      if (leftAlignError > 0) {
        el.outer.style.left = (el.orignalX - 500) + "px";
        el.outer.classList.remove('fromleft');
        el.outer.classList.add('fromright');

        // If right-alignment is worse than left alignment, switch back.
        var rightAlignError = this._calcLayoutError(el.inner);
        if (rightAlignError > leftAlignError) {
          el.outer.style.left = el.orignalX + "px";
          el.outer.classList.remove('fromright');
          el.outer.classList.add('fromleft');
        }
      }
    }, this);

    if (typeof this.onRender == 'function') {
      this.onRender(points);
    }
  },

  _calcLayoutError: function(el) {
    // Layout error is calculated as the number of linear pixels by which
    // an alignable extends past the left or right edge of the parent.
    var parentRect = this.element.parentNode.getBoundingClientRect();

    var error = 0;
    var rect = el.getBoundingClientRect();
    if (!rect.width) {
      return;
    }

    if (rect.right > parentRect.right) {
      error += rect.right - parentRect.right;
    }

    if (rect.left < parentRect.left) {
      error += parentRect.left - rect.left;
    }
    return error;
  },

  _addListeners: function() {
    this.graph.onUpdate( function() { this.update() }.bind(this) );
  }
});