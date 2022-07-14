(function() {

  $(document).ready(function() {
    updatePageTitle();
  });

  function updatePageTitle() {
    $.ajax({
      type: 'GET',
      cache: false,
      url: 'hostinfo',
      dataType: 'xml',
      success: function(xml) {
        var hostName = $(xml).find('hostname').text();
        if (hostName && hostName.length > 0) {
          document.title = hostName + "'s Morrigan";
        }
      }
    });
  }

})();
