ErrorHelper = {};
(function() {

  ErrorHelper.summarise = function(jqXHR, textStatus, errorThrown) {
    return textStatus + ' ' + jqXHR.status + ' ' + errorThrown;
  };

})();

ClickHelper = {};
(function() {

  var LONG_CLICK_MILLIS = 2000;

  ClickHelper.setupLongClick = function(element, onClick, onLongClick) {
    var pressTimer;
    var longClicked = false;

    element.bind('pointerup', function(event){
      clearTimeout(pressTimer);
      return false;
    })
    .bind('pointerdown', function(event){
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

// Polyfill https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position){
      position = position || 0;
      return this.substr(position, searchString.length) === searchString;
  };
}
