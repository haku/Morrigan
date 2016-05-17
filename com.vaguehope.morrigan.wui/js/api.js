MnApi = {};
(function() {

// --- Players ---

  MnApi.getPlayers = function(msgHandler, onPlayers) {
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
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching players: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.getPlayer = function(pid, msgHandler, onPlayer) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid,
      dataType : 'xml',
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching player ' + pid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
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
    player.state = parseInt(node.find('playstate').text(), 10);
    player.stateName = PLAYER_STATE_NAMES[parseInt(player.state, 10)];
    player.stateIcon = PLAYER_STATE_ICONS[parseInt(player.state, 10)];

    player.playOrder = parseInt(node.find('playorder').text(), 10);
    player.playOrderTitle = node.find('playordertitle').text();

    player.listTitle = node.find('listtitle').text();
    if (player.listTitle === "null") {
      player.listTitle = "(no list)";
    }
    player.trackTitle = node.find('tracktitle').text();
    player.position = parseInt(node.find('playposition').text(), 10);
    player.duration = parseInt(node.find('trackduration').text(), 10);

    player.queueVersion = parseInt(node.find('queueversion').text(), 10);
    player.queueLength = parseInt(node.find('queuelength').text(), 10);
    player.queueDuration = parseInt(node.find('queueduration').text(), 10);

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

    var trackNode = node.find('track');
    if (trackNode.length > 0) {
      player.item = parseItemNode($(trackNode[0]), player.mid, listView);
    }

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

  MnApi.playerPause = function(pid, msgHandler, onPlayer) {
    writePlayerState(pid, 'playpause', msgHandler, onPlayer);
  }

  MnApi.playerStop = function(pid, msgHandler, onPlayer) {
    writePlayerState(pid, 'stop', msgHandler, onPlayer);
  }

  MnApi.playerNext = function(pid, msgHandler, onPlayer) {
    writePlayerState(pid, 'next', msgHandler, onPlayer);
  }

  MnApi.PLAYBACK_ORDERS = [
    {id: "SEQUENTIAL",   title: "Sequential"},
    {id: "RANDOM",       title: "Random"},
    {id: "BYSTARTCOUNT", title: "By Start-Count"},
    {id: "BYLASTPLAYED", title: "By Last-Played"},
    {id: "MANUAL",       title: "Manual"}
  ];

  MnApi.playerPlaybackOrder = function(pid, order, msgHandler, onPlayer) {
    writePlayerState(pid, 'playbackorder&order=' + order.id, msgHandler, onPlayer);
  }

  MnApi.playerFullscreen = function(pid, monitor, msgHandler, onPlayer) {
    writePlayerState(pid, 'fullscreen&monitor=' + monitor.id, msgHandler, onPlayer);
  }

  function writePlayerState(pid, action, msgHandler, onPlayer) {
    $.ajax({
      type : 'POST',
      cache : false,
      url : 'players/' + pid,
      data : 'action=' + action,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo(action + '-ing...');
      },
      success : function(xml) {
        var playerNode = $(xml).find('player');
        var player = parsePlayerNode(playerNode);
        onPlayer(player);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

// --- Queue ---

  MnApi.getQueue = function(pid, msgHandler, onQueue) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid + '/queue',
      dataType : 'xml',
      success : function(xml) {
        var queueNode = $(xml).find('queue');
        var queue = parseQueueNode(pid, queueNode);
        onQueue(queue);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching queue ' + pid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.writeQueueItem = function(pid, item, action, msgHandler, onQueue) {
    $.ajax({
      type : 'POST',
      cache : false,
      url : 'players/' + pid + '/queue' + (item ? '/' + item.id : ''),
      data : 'action=' + action,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo(action + '-ing...');
      },
      success : function(xml) {
        var queueNode = $(xml).find('queue');
        var newQueue = parseQueueNode(pid, queueNode);
        onQueue(newQueue);
        msgHandler.onInfo('Queue ' + pid + ' updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseQueueNode(pid, node) {
    var queue = {};
    queue.pid = pid;
    queue.version = parseInt(node.find('queueversion').text(), 10);
    queue.length = parseInt(node.find('queuelength').text(), 10);
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
    item.id = parseInt(node.find('id').text(), 10);
    item.title = node.find('title').text();
    item.duration = parseInt(node.find('duration').text(), 10);
    return item;
  }

// --- DBs ---

  MnApi.getDbs = function(msgHandler, onDbs) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists',
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Reading DBs...');
      },
      success : function(xml) {
        var mlists = [];
        var entires = $(xml).find('entry');
        entires.each(function() {
          var node = $(this);
          var mlist = parseMlistNode(node);
          mlists.push(mlist);
        });
        mlists.sort(function(a, b) {
          return a.mid - b.mid;
        });
        onDbs(mlists);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching media lists: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.getDb = function(mid, view, msgHandler, onDb) {
    var url = 'mlists/' + mid;
    if (view) url += '?view=' + encodeURIComponent(view);
    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Reading DB ' + mid + '...');
      },
      success : function(xml) {
        var mlistNode = $(xml).find('mlist');
        var mlist = parseMlistNode(mlistNode);
        onDb(mlist);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching media list ' + mid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseMlistNode(node) {
    var mlist = {};
    mlist.mid = node.find('link[rel="self"]').attr('href').replace('/mlists/', '');
    mlist.type = node.attr('type');
    mlist.title = node.find('title').text();
    mlist.count = parseInt(node.find('count').text(), 10);
    mlist.duration = parseInt(node.find('duration').text(), 10);
    mlist.durationComplete = (node.find('durationcomplete').text() === 'true');
    return mlist;
  }

  MnApi.getQuery = function(mid, view, query, sortColumn, sortOrder, msgHandler, onItems) {
    if (!query || query.length < 1) query = '*';
    var url = 'mlists/' + mid + '/query/' + encodeURIComponent(query) + '?';
    if (view) url += '&view=' + encodeURIComponent(view);
    if (sortColumn) url += "&column=" + sortColumn;
    if (sortOrder) url += "&order=" + sortOrder;
    url += "&includedisabled=true";

    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Querying ' + mid + ' view=' + view + ' query=' + query + ' col=' + sortColumn + ' order=' + sortOrder + ' ...');
      },
      success : function(xml) {
        var itemsNode = $(xml).find('mlist');
        var items = parseItemsNode(itemsNode, mid, view);
        onItems(items);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error querying ' + mid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseItemsNode(node, mid, view) {
    var items = [];
    node.find('entry').each(function() {
      var item = parseItemNode($(this), mid, view);
      items.push(item);
    });
    return items;
  }

  function parseItemNode(node, mid, view) {
    var item = {
      mid: mid,
      view: view,
      relativeUrl: node.find('link[rel="self"]').attr('href'),
      title: node.find('title').text(),
      duration: parseInt(node.find('duration').text(), 10),
      startCount: parseInt(node.find('startcount').text(), 10),
      endCount: parseInt(node.find('endcount').text(), 10),
      enabled: node.find('enabled').text() == "true"
    };

    item.tags = [];
    node.find('tag').each(function() {
      var node = $(this);
      if (node.attr('t') === '0') {
        item.tags.push(node.text());
      }
    });

    item.url = '/mlists/' + mid + '/items/' + item.relativeUrl;

    return item;
  }

  MnApi.getAlbums = function(mid, view, msgHandler, onAlbums) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists/' + mid + '/albums?view=' + encodeURIComponent(view),
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Fetching ' + mid + ' view=' + view + ' ...');
      },
      success : function(xml) {
        var albumsNode = $(xml).find('albums');
        var albums = parseAlbumsNode(albumsNode, mid, view);
        onAlbums(albums);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching albums ' + mid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseAlbumsNode(node, mid, view) {
    var albums = [];
    node.find('entry').each(function() {
      var album = parseAlbumNode($(this), mid, view);
      albums.push(album);
    });
    return albums;
  }

  function parseAlbumNode(node, mid, view) {
    var album = {
      mid: mid,
      view: view,
      title: node.find('name').text(),
      relativeUrl: node.find('link[rel="self"]').attr('href'),
      coverRelativeUrl: node.find('link[rel="cover"]').attr('href'),
    };

    album.url = '/mlists/' + mid + '/albums/' + album.relativeUrl;
    if (album.coverRelativeUrl) album.coverUrl = '/mlists/' + mid + '/items/' + album.coverRelativeUrl;

    return album;
  }

  MnApi.enqueueItems = function(items, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    actionItem(items, 'queue', args, msgHandler, onComplete);
  };

  MnApi.enqueueItemsTop = function(items, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    actionItem(items, 'queue_top', args, msgHandler, onComplete);
  };

  MnApi.enqueueView = function(mid, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    actionItem({url: '/mlists/' + mid}, 'queue', args, msgHandler, onComplete);
  };

  MnApi.addTag = function(item, tag, msgHandler, onComplete) {
    actionItem(item, 'addtag', 'tag=' + encodeURIComponent(tag), msgHandler, onComplete);
  };

  MnApi.rmTag = function(item, tag, msgHandler, onComplete) {
    actionItem(item, 'rmtag', 'tag=' + encodeURIComponent(tag), msgHandler, onComplete);
  };

  MnApi.setEnabled = function(item, enabled, msgHandler, onComplete) {
    actionItem(item, 'set_enabled', 'enabled=' + enabled, msgHandler, onComplete);
  };

  function actionItem(item, action, args, msgHandler, onComplete) {
    var data = 'action=' + action;
    if (args) data += '&' + args;

    var params = {
      type : 'POST',
      cache : false,
      data : data,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'text',
      beforeSend : function() {
        msgHandler.onInfo(action + '-ing...');
      },
      success : function(text) {
        msgHandler.onInfo('');
        onComplete(text);
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    };

    if ($.isArray(item)) {
      var queue = item.slice();
      function f() {
        if (queue.length === 0) return;
        var p = jQuery.extend({}, params);
        p.url = queue.shift().url;
        $.ajax(p).then(f);
      };
      f();
    }
    else {
      params.url = item.url;
      $.ajax(params);
    }
  }

// --- Util ---

  MnApi.formatSeconds = function(s) {
    if (s < 1) return '0:00';
    if (s >= 3600) return parseInt(s / 3600) + ':' + zeroPad(parseInt((s % 3600) / 60)) + ':' + zeroPad(s % 60);
    return parseInt((s % 3600) / 60) + ':' + zeroPad(s % 60);
  }

  function zeroPad(s) {
    if (s < 10) return '0' + s;
    return s;
  }

})();
