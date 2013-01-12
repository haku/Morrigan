(function() {

  var REFRESH_SECONDS = 5;

  var statusBar;

  $(document).ready(function() {
    statusBar = $('<span>');
    $('.statusbar').append(statusBar);
    var playersDiv = $('.players');

    var pid = UrlParams.params['pid'];
    if (pid) {
      initPlayer(playersDiv, pid);
      setInterval(function() {
        updatePlayer(playersDiv, pid);
      }, REFRESH_SECONDS * 1000);
    }
    else {
      initPlayers(playersDiv);
      setInterval(function() {
        updatePlayers(playersDiv);
      }, REFRESH_SECONDS * 1000);
    }

  });

  function initPlayers(playersDiv) {
    playersDiv.empty();
    updatePlayers(playersDiv);
  }

  function initPlayer(playersDiv, pid) {
    playersDiv.empty();
    updatePlayer(playersDiv, pid);
  }

  function updatePlayers(playersDiv) {
    getPlayers(function(msg) {
      statusBar.text(msg);
    }, function(players) {
      $.each(players, function(index, value) {
        displayPlayer(playersDiv, players[index], false);
      });
    });
  }

  function updatePlayer(playersDiv, pid) {
    getPlayer(pid, function(msg) {
      statusBar.text(msg);
    }, function(player) {
      displayPlayer(playersDiv, player, true);
    });
  }

  function displayPlayer(playersDiv, player, detailed) {
    var playerDivId = 'player' + player.pid;
    var playerDiv = $('#' + playerDivId, playersDiv);
    if (playerDiv.size() < 1) {
      playerDiv = $('<div class="player">');
      playerDiv.attr('id', playerDivId);
      makePlayer(playerDiv, player.pid, detailed);
      playersDiv.append(playerDiv);
    }
    updatePlayerDisplay(playerDiv, player);
  }

  function updatePlayerDisplay(playerDiv, player) {
    $('.name', playerDiv).text(player.name);
    $('.state', playerDiv).text(player.stateName);
    $('.title', playerDiv).text(player.trackTitle);
    $('.list', playerDiv).text(player.listTitle);
  }

  function makePlayer(playerDiv, pid, detailed) {
    playerDiv.empty();

    if (detailed === true) {
      var btnBlock = $('<div class="block">');
      playerDiv.append(btnBlock);
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
      btnBlock.append(btnPause);
      btnBlock.append(btnNext);
    }

    var textBlock = $('<div class="block">');
    playerDiv.append(textBlock);

    if (detailed === false) {
      playerDiv.append($('<a class="details" href="?pid=' + pid + '">'));
    }

    var topRow = $('<p class="toprow">');
    var mainRow = $('<p class="mainrow">');
    textBlock.append(topRow);
    textBlock.append(mainRow);
    topRow.append($('<span class="name">'));
    topRow.append($('<span class="state">'));
    topRow.append($('<span class="list">'));
    topRow.append($('<span class="status">'));
    mainRow.append($('<span class="title">'));
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

  function getPlayer(pid, onStatus, onPlayer) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid,
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading player ' + pid + '...');
      },
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        onStatus('Player ' + pid + ' updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching player ' + pid + ': ' + textStatus);
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
    if (player.listTitle === "null")
      player.listTitle = "(no list)";
    return player;
  }

  function playerPause(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'playpause', onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player);
    });
  }

  function playerNext(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'next', onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player);
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
        onStatus(action + '-ing...');
      },
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        console.log(jqXHR, textStatus, errorThrown);
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
