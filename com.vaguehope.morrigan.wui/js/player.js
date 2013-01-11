(function() {

  var REFRESH_SECONDS = 5;

  var statusBar;

  $(document).ready(function() {
    statusBar = $('<span>');
    $('.statusbar').append(statusBar);
    var playersDiv = $('.players');
    initPlayers(playersDiv);
    setInterval(function() {
      updatePlayers(playersDiv);
    }, REFRESH_SECONDS * 1000);
  });

  function initPlayers(playersDiv) {
    playersDiv.empty();
    updatePlayers(playersDiv);
  }

  function updatePlayers(playersDiv) {
    getPlayers(function(msg) {
      statusBar.text(msg);
    }, function(players) {
      $.each(players, function(index, value) {
        var player = players[index];
        var playerDivId = 'player' + player.pid;
        var playerDiv = $('#' + playerDivId, playersDiv);
        if (playerDiv.size() < 1) {
          playerDiv = $('<div class="player">');
          playerDiv.attr('id', playerDivId);
          makePlayer(playerDiv, player.pid);
          playersDiv.append(playerDiv);
        }
        updatePlayer(playerDiv, player);
      });
    });
  }

  function updatePlayer(playerDiv, player) {
    $('.name', playerDiv).text(player.name);
    $('.state', playerDiv).text(player.stateName);
    $('.title', playerDiv).text(player.trackTitle);
  }

  function makePlayer(playerDiv, pid) {
    var onStatus = function(msg) {
      $('.status', playerDiv).text(msg);
    };
    var btnPause = $('<button class="pause">||</button>');
    var btnNext = $('<button class="next">&gt;&gt;|</button>');
    btnPause.click(function() {
      playerPause(pid, playerDiv, onStatus);
    });
    btnNext.click(function() {
      playerNext(pid, playerDiv, onStatus);
    });
    playerDiv.empty();
    playerDiv.append(btnPause);
    playerDiv.append(btnNext);
    playerDiv.append($('<span class="status">'));
    playerDiv.append($('<span class="state">'));
    playerDiv.append($('<span class="name">'));
    playerDiv.append($('<span class="title">'));
  }

  function getPlayers(onStatus, onPlayers) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players',
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading players...');
      },
      success : function(xml) {
        var players = [];
        var entires = $(xml).find('entry');
        entires.each(function() {
          var node = $(this);
          var player = parsePlayerNode(node);
          players.push(player);
        });
        players.sort(function(a, b) {
          return a.pid - b.pid;
        });
        onPlayers(players);
        onStatus('Players updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching players: ' + textStatus);
      }
    });
  }

  function parsePlayerNode(node) {
    var player = {};
    player.pid = node.find('playerid').text();
    player.name = node.find('playername').text();
    player.state = node.find('playstate').text();
    player.stateName = playerStateToLabel(parseInt(player.state));
    player.trackTitle = node.find('tracktitle').text();
    player.listTitle = node.find('listtitle').text();
    return player;
  }

  function playerPause(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'playpause', onStatus, function(player) {
      updatePlayer(playerDiv, player);
    });
  }

  function playerNext(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'next', onStatus, function(player) {
      updatePlayer(playerDiv, player);
    });
  }

  function writePlayerState(pid, action, onStatus, onPlayer) {
    $.ajax({
      type : 'POST',
      cache : false,
      url : 'players/' + pid,
      data : 'action=' + action,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Player ' + pid + ': ' + action + '-ing...');
      },
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error: ' + textStatus);
      }
    });
  }

  /**
   * Argument must be int not String.
   */
  function playerStateToLabel(state) {
    switch (state) {
      case 0:
        return 'Stopped';
      case 1:
        return 'Playing';
      case 2:
        return 'Paused';
      case 3:
        return 'Loading';
      default:
        return 'Unknown';
    }
  }

})();
