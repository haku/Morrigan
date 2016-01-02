MnApi = {};
(function() {

  MnApi.getPlayers = function(onStatus, onPlayers) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players',
      dataType : 'xml',
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
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching players: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.getPlayer = function(pid, onStatus, onPlayer) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid,
      dataType : 'xml',
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching player ' + pid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  var PLAYER_STATE_NAMES = ['Stopped', 'Playing', 'Paused', 'Loading']
  var PLAYER_STATE_ICONS = ['stop', 'play_arrow', 'pause', 'hourglass_full']

  function parsePlayerNode(node) {
    var player = {};
    player.pid = node.find('playerid').text();
    player.name = node.find('playername').text();
    player.title = node.find('title').text();
    player.state = node.find('playstate').text();
    player.stateName = PLAYER_STATE_NAMES[parseInt(player.state)];
    player.stateIcon = PLAYER_STATE_ICONS[parseInt(player.state)];

    player.playOrder = node.find('playorder').text();
    player.playOrderTitle = node.find('playordertitle').text();

    player.trackTitle = node.find('tracktitle').text();
    player.listTitle = node.find('listtitle').text();
    if (player.listTitle === "null") {
      player.listTitle = "(no list)";
    }

    player.trackDuration = parseInt(node.find('trackduration').text());
    player.queueLength = parseInt(node.find('queuelength').text());
    player.queueDuration = parseInt(node.find('queueduration').text());

    player.tags = [];
    node.find('tracktag').each(function() {
      var node = $(this);
      if (node.attr('t') === '0') {
        player.tags.push(node.text());
      }
    });

    var listHref = node.find('link[rel="list"]').attr('href');
    if (listHref) {
      player.mid = listHref.replace('/mlists/', '');
    }

    var listView = node.find('listview').text();
    if (listView && listView.length > 0) player.listView = listView;

    player.monitors = [];
    node.find('monitor').each(function() {
      var monitor = {};
      var text = $(this).text();
      var x = text.indexOf(":");
      monitor.id = text.substring(0, x);
      monitor.name = text.substring(x + 1);
      player.monitors.push(monitor);
    });

    return player;
  }

  MnApi.playerPause = function(pid, onStatus, onPlayer) {
    writePlayerState(pid, 'playpause', onStatus, onPlayer);
  }

  MnApi.playerNext = function(pid, onStatus, onPlayer) {
    writePlayerState(pid, 'next', onStatus, onPlayer);
  }

  MnApi.PLAYBACK_ORDERS = [
    {id: "SEQUENTIAL",   title: "sequential"},
    {id: "RANDOM",       title: "random"},
    {id: "BYSTARTCOUNT", title: "by start-count"},
    {id: "BYLASTPLAYED", title: "by last-played"},
    {id: "MANUAL",       title: "manual"}
  ];

  MnApi.playerPlaybackOrder = function(pid, order, onStatus, onPlayer) {
    writePlayerState(pid, 'playbackorder&order=' + order.id, onStatus, onPlayer);
  }

  MnApi.playerFullscreen = function(pid, monitor, onStatus, onPlayer) {
    writePlayerState(pid, 'fullscreen&monitor=' + monitor.id, onStatus, onPlayer);
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
        onStatus('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

})();
