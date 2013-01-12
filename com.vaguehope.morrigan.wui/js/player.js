(function() {

  var REFRESH_PLAYERS_SECONDS = 5;
  var REFRESH_QUEUE_SECONDS = 15;

  var playersStatusBar;
  var queueStatusBar;

  $(document).ready(function() {
    initStatusBars();
    var playersDiv = $('.players');
    var pid = UrlParams.params['pid'];
    if (pid) {
      initPlayer(playersDiv, pid);
      var queueDiv = $('.queue');
      initQueue(queueDiv, pid);
      setInterval(function() {
        updatePlayer(playersDiv, pid);
      }, REFRESH_PLAYERS_SECONDS * 1000);
      setInterval(function() {
        updateQueue(queueDiv, pid);
      }, REFRESH_QUEUE_SECONDS * 1000);
    }
    else {
      initPlayers(playersDiv);
      setInterval(function() {
        updatePlayers(playersDiv);
      }, REFRESH_PLAYERS_SECONDS * 1000);
    }
  });

  function initStatusBars() {
    playersStatusBar = $('<span>');
    $('.statusbar').append(playersStatusBar);
    queueStatusBar = $('<span>');
    $('.statusbar').append(queueStatusBar);
  }

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
      playersStatusBar.text(msg);
    }, function(players) {
      $.each(players, function(index, value) {
        displayPlayer(playersDiv, players[index], false);
      });
    });
  }

  function updatePlayer(playersDiv, pid) {
    getPlayer(pid, function(msg) {
      playersStatusBar.text(msg);
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
    updatePlayerDisplay(playerDiv, player, detailed);
  }

  function updatePlayerDisplay(playerDiv, player, detailed) {
    $('.name', playerDiv).text(player.name);
    $('.state', playerDiv).text(player.stateName);
    $('.list', playerDiv).text(player.listTitle);

    if (detailed === true) {
      $('.title', playerDiv).text(player.trackTitle + ' (' + player.trackDuration + ' seconds)');
      $('.tagsrow .tags', playerDiv).text('(tags)');
    }
    else {
      $('.title', playerDiv).text(player.trackTitle);
    }

  }

  function makePlayer(playerDiv, pid, detailed) {
    playerDiv.empty();

    if (detailed === true) {
      var btnBlock = $('<div class="block buttons">');
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

    var textBlock = $('<div class="block text">');
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

    if (detailed === true) {
      var tagsRow = $('<p class="tagsrow">');
      textBlock.append(tagsRow);
      tagsRow.append($('<span class="tags">'));
    }
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
    if (player.listTitle === "null") {
      player.listTitle = "(no list)";
    }
    player.trackDuration = parseInt(node.find('trackduration').text());
    player.queueLength = parseInt(node.find('queuelength').text());
    player.queueDuration = parseInt(node.find('queueduration').text());
    return player;
  }

  function playerPause(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'playpause', onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player, true);
    });
  }

  function playerNext(pid, playerDiv, onStatus) {
    writePlayerState(pid, 'next', onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player, true);
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

  function initQueue(queueDiv, pid) {
    queueDiv.empty();
    updateQueue(queueDiv, pid);
  }

  function updateQueue(queueDiv, pid) {
    getQueue(pid, function(msg) {
      queueStatusBar.text(msg);
    }, function(queue) {
      displayQueue(queueDiv, queue);
    });
  }

  function displayQueue(queueDiv, queue) {
    var header = $('.header p', queueDiv);
    if (header.size() < 1) {
      var headerDiv = $('.header', queueDiv);
      if (headerDiv.size() < 1) {
        headerDiv = $('<div class="header">');
        queueDiv.append(headerDiv);
      }
      header = $('<p>');
      headerDiv.append(header);
    }
    header.text(queue.length + ' items, ' + queue.duration + '.');

    var allItemIds = {};
    $.each(queue.items, function(index, item) {
      var itemDivId = 'qitem' + item.id;
      var itemDiv = $('#' + itemDivId, queueDiv);
      if (itemDiv.size() < 1) {
        itemDiv = $('<div class="item">');
        itemDiv.attr('id', itemDivId);
        makeQueueItem(itemDiv, item.id);
        queueDiv.append(itemDiv);
      }
      updateQueueItemDisplay(itemDiv, item);
      allItemIds[item.id] = item.id;
    });
    $('.item', queueDiv).each(function() {
      var item = $(this);
      var itemId = item.data('id');
      if (!allItemIds[itemId]) {
        item.remove();
      }
    });
  }

  function makeQueueItem(itemDiv, id) {
    itemDiv.empty();
    itemDiv.data('id', id);
    var title = $('<p class="title">');
    itemDiv.append(title);
  }

  function updateQueueItemDisplay(queueDiv, item) {
    $('.title', queueDiv).text(item.title);
  }

  function getQueue(pid, onStatus, onQueue) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid + '/queue',
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading queue ' + pid + '...');
      },
      success : function(xml) {
        var queueNode = $(xml).find('queue');
        var queue = parseQueueNode(queueNode);
        onQueue(queue);
        onStatus('Queue ' + pid + ' updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching queue ' + pid + ': ' + textStatus);
      }
    });
  }

  function parseQueueNode(node) {
    var queue = {};
    queue.length = parseInt(node.find('queuelength').text());
    queue.duration = node.find('queueduration').text();
    queue.items = [];
    node.find('entry').each(function() {
      var item = parseQueueItemNode($(this));
      queue.items.push(item);
    });
    return queue;
  }

  function parseQueueItemNode(node) {
    var item = {};
    item.id = parseInt(node.find('id').text());
    item.title = node.find('title').text();
    item.duration = parseInt(node.find('duration').text());
    return item;
  }

})();
