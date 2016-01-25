MnApi = {};
(function() {

// --- Players ---

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
    player.state = parseInt(node.find('playstate').text(), 10);
    player.stateName = PLAYER_STATE_NAMES[parseInt(player.state, 10)];
    player.stateIcon = PLAYER_STATE_ICONS[parseInt(player.state, 10)];

    player.playOrder = parseInt(node.find('playorder').text(), 10);
    player.playOrderTitle = node.find('playordertitle').text();

    player.trackTitle = node.find('tracktitle').text();
    player.listTitle = node.find('listtitle').text();
    if (player.listTitle === "null") {
      player.listTitle = "(no list)";
    }

    player.trackDuration = parseInt(node.find('trackduration').text(), 10);
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
    {id: "SEQUENTIAL",   title: "Sequential"},
    {id: "RANDOM",       title: "Random"},
    {id: "BYSTARTCOUNT", title: "By Start-Count"},
    {id: "BYLASTPLAYED", title: "By Last-Played"},
    {id: "MANUAL",       title: "Manual"}
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

// --- Queue ---

  MnApi.getQueue = function(pid, onStatus, onQueue) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'players/' + pid + '/queue',
      dataType : 'xml',
      success : function(xml) {
        var queueNode = $(xml).find('queue');
        var queue = parseQueueNode(pid, queueNode);
        onQueue(queue);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching queue ' + pid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.writeQueueItem = function(pid, item, action, onStatus, onQueue) {
    $.ajax({
      type : 'POST',
      cache : false,
      url : 'players/' + pid + '/queue' + (item ? '/' + item.id : ''),
      data : 'action=' + action,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'xml',
      beforeSend : function() {
        onStatus(action + '-ing...');
      },
      success : function(xml) {
        var queueNode = $(xml).find('queue');
        var newQueue = parseQueueNode(pid, queueNode);
        onQueue(newQueue);
        onStatus('Queue ' + pid + ' updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseQueueNode(pid, node) {
    var queue = {};
    queue.pid = pid;
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

  MnApi.getDbs = function(onStatus, onDbs) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists',
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading DBs...');
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
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching media lists: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.getDb = function(mid, view, onStatus, onDb) {
    var id = midToId(mid);
    var url = 'mlists/' + mid;
    if (view) url += '?view=' + encodeURIComponent(view);
    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading DB ' + id + '...');
      },
      success : function(xml) {
        var mlistNode = $(xml).find('mlist');
        var mlist = parseMlistNode(mlistNode);
        onDb(mlist);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching media list ' + id + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseMlistNode(node) {
    var mlist = {};
    mlist.mid = node.find('link[rel="self"]').attr('href').replace('/mlists/', '');
    mlist.id = midToId(mlist.mid);
    mlist.type = node.attr('type');
    mlist.title = node.find('title').text();
    mlist.count = parseInt(node.find('count').text(), 10);
    mlist.duration = parseInt(node.find('duration').text(), 10);
    mlist.durationComplete = (node.find('durationcomplete').text() === 'true');
    return mlist;
  }

  MnApi.getQuery = function(mid, view, query, sortColumn, sortOrder, onStatus, onItems) {
    var id = midToId(mid);

    if (!query || query.length < 1) query = '*';
    var url = 'mlists/' + mid + '/query/' + encodeURIComponent(query) + '?';
    if (view) url += '&view=' + encodeURIComponent(view);
    if (sortColumn) url += "&column=" + sortColumn;
    if (sortOrder) url += "&order=" + sortOrder;

    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Querying ' + id + ' view=' + view + ' query=' + query + ' col=' + sortColumn + ' order=' + sortOrder + ' ...');
      },
      success : function(xml) {
        var itemsNode = $(xml).find('mlist');
        var items = parseItemsNode(itemsNode, mid);
        onItems(items);
        onStatus('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error querying ' + id + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  function parseItemsNode(node, mid) {
    var items = [];
    node.find('entry').each(function() {
      var item = parseItemNode($(this), mid);
      items.push(item);
    });
    return items;
  }

  function parseItemNode(node, mid) {
    var item = {};
    item.relativeUrl = node.find('link[rel="self"]').attr('href');
    item.title = node.find('title').text();
    item.duration = parseInt(node.find('duration').text(), 10);
    item.startCount = parseInt(node.find('startcount').text(), 10);
    item.endCount = parseInt(node.find('endcount').text(), 10);

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

  function midToId(mid) {
    return mid.match(/\/(.+?)\./)[1];
  }

  MnApi.enqueueItems = function(items, view, pid, onStatus, onComplete) {
    actionItem(items, view, pid, 'queue', onStatus, onComplete);
  }

  MnApi.enqueueView = function(mid, view, pid, onStatus, onComplete) {
    actionItem({url: '/mlists/' + mid}, view, pid, 'queue', onStatus, onComplete);
  }

  function actionItem(item, view, pid, action, onStatus, onComplete) {
    var data = 'action=' + action + '&playerid=' + pid;
    if (view) data += '&view=' + encodeURIComponent(view);

    var params = {
      type : 'POST',
      cache : false,
      data : data,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'text',
      beforeSend : function() {
        onStatus(action + '-ing...');
      },
      success : function(text) {
        onStatus('');
        onComplete(text);
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
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
