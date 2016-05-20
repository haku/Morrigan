ErrorHelper = {};
(function() {

  ErrorHelper.summarise = function(jqXHR, textStatus, errorThrown) {
    return textStatus + ' ' + jqXHR.status + ' ' + errorThrown;
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
