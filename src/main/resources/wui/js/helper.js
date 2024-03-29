ErrorHelper = {};
(function() {

  ErrorHelper.summarise = function(jqXHR, textStatus, errorThrown) {
    return textStatus + ' ' + jqXHR.status + ' ' + errorThrown;
  };

})();

ClickHelper = {};
(function() {

  var LONG_CLICK_MILLIS = 1000;

  ClickHelper.setupLongClick = function(element, onClick, onLongClick) {
    var pressTimer;
    var longClicked = false;
    var x = -1;
    var y = -1;

    element.bind('contextmenu', function(event){
      event.preventDefault();
    })
    .bind('pointerup', function(event){
      x = -1;
      clearTimeout(pressTimer);
      return false;
    })
    .bind('pointermove', function(event){
      if (x != -1) {
        if (Math.abs(event.screenX - x) > 5
          || Math.abs(event.screenY - y) > 5) {
          clearTimeout(pressTimer);
          x = -1;
          return false;
        }
      }
    })
    .bind('pointerdown', function(event){
      event.preventDefault();
      x = event.screenX;
      y = event.screenY;

      longClicked = false;
      pressTimer = window.setTimeout(function() {
        longClicked = true;
        onLongClick(event);
      }, LONG_CLICK_MILLIS);
      return false;
    })
    .click(function(event){
      if (longClicked) {
        event.preventDefault();
        return;
      }
      onClick(event);
    });
  };

})();

jQuery.fn.setVisibility = function(visibility) {
  return this.css('visibility', visibility ? 'visible' : 'hidden');
};

jQuery.fn.updateText = function(text) {
  if (this.text() === text) return this;
  return this.text(text);
};

// Polyfill https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position){
      position = position || 0;
      return this.substr(position, searchString.length) === searchString;
  };
}
