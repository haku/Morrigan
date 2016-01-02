(function() {

  var REFRESH_PLAYERS_SECONDS = 5;

  $(document).ready(function() {
    updatePageTitle();
    startPoller();
  });

  function updatePageTitle() {
    $.ajax({
      type: 'GET',
      cache: false,
      url: '/hostinfo',
      dataType: 'xml',
      success: function(xml) {
        var hostName = $(xml).find('hostname').text();
        if (hostName && hostName.length > 0) {
          document.title = hostName + "'s Morrigan";
          $('#drawer_title').text(hostName);
        }
      }
    });
  }

  function startPoller() {
    setInterval(function() {
      fetchAndDisplayPlayers();
    }, REFRESH_PLAYERS_SECONDS * 1000);
    fetchAndDisplayPlayers();
  }

  function fetchAndDisplayPlayers(){
    MnApi.getPlayers(function(msg) {
      if (msg && msg.length > 0) console.log(msg);
    }, displayPlayers);
  }

  function displayPlayers(players) {
    $.each(players, function(index, player) {
      var playerElem = $('#player_' + player.pid);
      if (playerElem.size() < 1) {
        playerElem = $(
          '<a class="mdl-navigation__link" href="">'
          + '<span class="mdl-button mdl-js-button mdl-button--icon">'
          + '<i class="material-icons">stop</i>'
          + '</span>'
          + '<span class="name">Name</span>'
          + '</a>');
        $('#players_list').append(playerElem);
        playerElem.attr('id', 'player_' + player.pid);
      }
      $('.name', playerElem).text(player.name);
      $('.material-icons', playerElem).text(player.stateIcon);
    });
    // TODO remove gone players.
  }

})();
