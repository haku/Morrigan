ErrorHelper = {};
(function() {

  ErrorHelper.summarise = function(jqXHR, textStatus, errorThrown) {
    return textStatus + ' ' + jqXHR.status + ' ' + errorThrown;
  };

})();

jQuery.fn.setVisibility = function(visibility) {
  return this.css('visibility', visibility ? 'visible' : 'hidden');
};
