MnApi = {};
(function() {

// --- Players ---

  MnApi.getPlayers = function(msgHandler, onPlayers) {
    return $.ajax({
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
    return $.ajax({
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
    player.stateName = PLAYER_STATE_NAMES[player.state];
    player.stateIcon = PLAYER_STATE_ICONS[player.state];

    player.volume = parseInt(node.find('volume').text(), 10);
    player.volumemaxvalue = parseInt(node.find('volumemaxvalue').text(), 10);

    player.playOrderId = node.find('playorderid').text();
    player.playOrderTitle = node.find('playordertitle').text();
    player.playOrderOverrideId = node.find('playorderoverrideid').text();

    player.transcode = node.find('transcode').text();
    player.transcodeTitle = node.find('transcodetitle').text();

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
      player.mid = listHref.replace('mlists/', '');
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
    {id: "FOLLOWTAGS",   title: "Follow Tags"},
    {id: "MANUAL",       title: "Manual"}
  ];

  var idToPlaybackOrder = {};
  $.each(MnApi.PLAYBACK_ORDERS, function(index, order) {
    idToPlaybackOrder[order.id] = order;
  });

  MnApi.playbackOrderFromId = function(id) {
    return idToPlaybackOrder[id];
  };

  MnApi.TRANSCODES = [
    {id: "",                  title: "No Transcode"},
    {id: "common_audio_only", title: "Common Audio Only"},
    {id: "mobile_audio",      title: "Mobile Audio"},
    {id: "mp3_only",          title: "MP3 Only"},
  ];

  MnApi.playerSeek = function(pid, position, msgHandler, onPlayer) {
    writePlayerState(pid, 'seek&position=' + position, msgHandler, onPlayer);
  }

  MnApi.playerPlaybackOrder = function(pid, order, msgHandler, onPlayer) {
    writePlayerState(pid, 'playbackorder&order=' + order.id, msgHandler, onPlayer);
  }

  MnApi.playerTranscode = function(pid, transcode, msgHandler, onPlayer) {
    writePlayerState(pid, 'transcode&transcode=' + transcode.id, msgHandler, onPlayer);
  }

  MnApi.playerFullscreen = function(pid, monitor, msgHandler, onPlayer) {
    writePlayerState(pid, 'fullscreen&monitor=' + monitor.id, msgHandler, onPlayer);
  }

  MnApi.playerSetVolume  = function(pid, newVolume, msgHandler, onPlayer) {
    writePlayerState(pid, 'setvolume&volume=' + newVolume, msgHandler, onPlayer);
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

// --- Saved Views ---

  MnApi.getSavedViews = function(msgHandler, onSavedViews) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists/savedviews',
      dataType : 'json',
      success : function(json) {
        onSavedViews(json);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching saved views ' + pid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

// --- DBs ---

  MnApi.getDbs = function(msgHandler, onDbs) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'media',
      dataType : 'json',
      beforeSend : function() {
        msgHandler.onInfo('Reading DBs...');
      },
      success : function(dbs) {
        onDbs(dbs);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching media lists: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.getNode = function(mid, nodeId, parentNodeId, msgHandler, onNode) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'media/' + mid + '/node/' + nodeId,
      dataType : 'json',
      beforeSend : function() {
        msgHandler.onInfo('Reading Node...');
      },
      success : function(node) {
        // stuffing mid everywhere seems silly, but seems to make everything else simpler.
        node.mid = mid;
        node.parentNodeId = parentNodeId;  // for implementing ".." in the UI.  showing proper browing breadcome would hopefully make this not needed.
        node.nodes.forEach(n => n.mid = mid);
        node.items.forEach(i => {
          i.mid = mid;
          i.url = 'mlists/' + mid + "/items/" + i.id;  // for now point to old handler so playback etc works.
        });
        onNode(node);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching node: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
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
    mlist.mid = node.find('link[rel="self"]').attr('href').replace('mlists/', '');
    mlist.type = node.attr('type');
    mlist.title = node.find('title').text();
    mlist.count = parseInt(node.find('count').text(), 10);
    mlist.duration = parseInt(node.find('duration').text(), 10);
    mlist.durationComplete = (node.find('durationcomplete').text() === 'true');
    return mlist;
  }

  MnApi.getQuery = function(mid, view, query, sortColumn, sortOrder, includeDisabled, msgHandler, onItems) {
    if (!query || query.length < 1) query = '*';
    var url = 'mlists/' + mid + '/query/' + encodeURIComponent(query) + '?';
    if (view) url += '&view=' + encodeURIComponent(view);
    if (sortColumn) url += "&column=" + sortColumn;
    if (sortOrder) url += "&order=" + sortOrder;
    if (includeDisabled) url += "&includedisabled=true";

    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Querying ' + mid + ' view=' + view + ' query=' + query + ' col=' + sortColumn + ' order=' + sortOrder + ' disabled=' + includeDisabled + ' ...');
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
      title: node.find('title').text(),
      duration: parseInt(node.find('duration').text(), 10),
      startCount: parseInt(node.find('startcount').text(), 10),
      endCount: parseInt(node.find('endcount').text(), 10),
      enabled: node.find('enabled').text() == "true",
      dateLastPlayed: node.find('datelastplayed').text(),
    };

    var href = node.find('link[rel="self"]').attr('href');
    var dhref = href ? decodeURIComponent(href) : null;
    if (dhref && dhref.startsWith('id=')) {
      item.remoteId = dhref;
    }
    else {
      item.relativeUrl = href;
      item.url = 'mlists/' + mid + '/items/' + item.relativeUrl;
    }

    item.tags = [];
    node.find('tag').each(function() {
      var node = $(this);
      if (node.attr('t') === '0') {
        item.tags.push(node.text());
      }
    });

    return item;
  }

  MnApi.getAlbums = function(mid, view, msgHandler, onAlbums) {
    var url = 'mlists/' + mid + '/albums';
    if (view) url += '?view=' + encodeURIComponent(view);
    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'xml',
      beforeSend : function() {
        msgHandler.onInfo('Fetching albums ' + mid + ' view=' + view + ' ...');
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
      trackCount: node.find('trackcount').text(),
      relativeUrl: node.find('link[rel="self"]').attr('href'),
      coverRelativeUrl: node.find('link[rel="cover"]').attr('href'),
    };

    album.url = 'mlists/' + mid + '/albums/' + album.relativeUrl;
    if (album.coverRelativeUrl) album.coverUrl = 'mlists/' + mid + '/items/' + album.coverRelativeUrl;
    if (album.coverUrl) album.resizedCoverUrl = album.coverUrl + '?resize=200'; // TODO make param?

    return album;
  }

  MnApi.getTags = function(mid, view, msgHandler, onTags) {
    var url = 'mlists/' + mid + '/tags?count=100';
    if (view) url += '&view=' + encodeURIComponent(view);
    $.ajax({
      type : 'GET',
      cache : false,
      url : url,
      dataType : 'json',
      beforeSend : function() {
        msgHandler.onInfo('Fetching tags ' + mid + ' view=' + view + ' ...');
      },
      success : function(json) {
        var tags = $.map(json, function(val, i) {
          return {
            mid: mid,
            view: view,
            title: val.label,
            value: val.value,
          };
        });
        onTags(tags);
        msgHandler.onInfo('');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error fetching tags ' + mid + ': ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    });
  }

  MnApi.enqueueItems = function(items, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    actionItem(items, 'queue', args, msgHandler, onComplete);
  };

  MnApi.enqueueItemsTop = function(items, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    if (items instanceof Set) {
      items = Array.from(items);
      items.reverse();  // A bit of a hack... could be odd if track ends mid enqueueing.
    }
    actionItem(items, 'queue_top', args, msgHandler, onComplete);
  };

  MnApi.enqueueView = function(mid, view, pid, msgHandler, onComplete) {
    var args = 'playerid=' + pid;
    if (view) args += '&view=' + encodeURIComponent(view);
    actionItem({url: 'mlists/' + mid}, 'queue', args, msgHandler, onComplete);
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
    console.log(item);
    var data = 'action=' + action;
    if (args) data += '&' + args;

    var params = {
      type : 'POST',
      cache : false,
      data : data,
      contentTypeString : 'application/x-www-form-urlencoded',
      dataType : 'text',
      //beforeSend : function() {
      //  msgHandler.onInfo(action + '-ing...');
      //},
      success : function(text) {
        msgHandler.onInfo('');
        onComplete(text);
      },
      error : function(jqXHR, textStatus, errorThrown) {
        msgHandler.onError('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
      }
    };

    if (item instanceof Set) {
      item = Array.from(item);
    }
    if ($.isArray(item)) {
      var queue = item.slice();
      function f() {
        if (queue.length === 0) return;
        var i = queue.shift()
        var p = jQuery.extend({}, params);
        p.url = i.url;
        p.success = function(text) {
          msgHandler.onInfo('');
          onComplete(text, i);
        }
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
