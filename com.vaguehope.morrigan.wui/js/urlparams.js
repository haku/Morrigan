UrlParams = {};
(function() {

  var match, pl = /\+/g, // Regex for replacing addition symbol with a space
  search = /([^&=]+)=?([^&]*)/g, decode = function(s) {
    return decodeURIComponent(s.replace(pl, " "));
  }, query = window.location.search.substring(1);
  var params = {};
  while ( match = search.exec(query)) {
    params[decode(match[1])] = decode(match[2]);
  }
  UrlParams.params = params;

  UrlParams.withParam = function(key, value) {
    var clean = document.location.search.replace(new RegExp('[&?]?(' + key + '=[^&]+)'), '');
    return clean + '&' + key + '=' + value;
  };

  UrlParams.withoutParam = function(key) {
    return document.location.search.replace(new RegExp('[&?]?(' + key + '=[^&]+)'), '');
  };

})();
