Players = {};
(function() {

  Players.getPlayers = getPlayers;

  var REFRESH_PLAYERS_SECONDS = 5;
  var REFRESH_QUEUE_SECONDS = 15;

  var PLAYBACK_ORDERS = [
    {id: "SEQUENTIAL",   title: "sequential"},
    {id: "RANDOM",       title: "random"},
    {id: "BYSTARTCOUNT", title: "by start-count"},
    {id: "BYLASTPLAYED", title: "by last-played"},
    {id: "FOLLOWTAGS",   title: "follow tags"},
    {id: "MANUAL",       title: "manual"}
  ];

  var playersStatusBar;
  var queueStatusBar;

  $(document).ready(function() {
    initStatusBars();
    var playersDiv = $('.players');
    var mid = UrlParams.params['mid'];
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
    else if (!mid) {
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
      $.each(players, function(index, player) {
        displayPlayer(playersDiv, player, false);
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
    $('.order', playerDiv).text(player.playOrderTitle);
    $('.list', playerDiv).text(player.listTitle);

    if (detailed === true) {
      $('.title', playerDiv).text(player.trackTitle + ' (' + player.trackDuration + 's)');
      $('.tagsrow .tags', playerDiv).text(player.tags.length > 0 ? player.tags.join(', ') : '(no tags)');

      var menuLink = $('.clickable', playerDiv);
      menuLink.unbind();
      menuLink.click(function(event) {
        event.preventDefault();
        showPlayerMenu(playerDiv, player);
      });
    }
    else {
      $('.title', playerDiv).text(player.trackTitle);
    }
  }

  function makePlayer(playerDiv, pid, detailed) {
    playerDiv.empty();

    var textBlock = $('<div class="block text">');
    playerDiv.append(textBlock);

    if (detailed === true) {
      playerDiv.append($('<a class="clickable" href="">'));
    }
    else {
      playerDiv.append($('<a class="clickable" href="?pid=' + pid + '">'));
    }

    var topRow = $('<p class="toprow">');
    var mainRow = $('<p class="mainrow">');
    textBlock.append(topRow);
    textBlock.append(mainRow);
    topRow.append($('<span class="name">'));
    topRow.append($('<span class="state">'));
    topRow.append($('<span class="order">'));
    topRow.append($('<span class="list">'));
    topRow.append($('<span class="status">'));
    mainRow.append($('<span class="title">'));

    if (detailed === true) {
      var tagsRow = $('<p class="tagsrow">');
      textBlock.append(tagsRow);
      tagsRow.append($('<span class="tags">'));

      makeToolbar(playerDiv, pid);
    }
  }

  function makeToolbar(playerDiv, pid) {
    var toolbar = $('<div class="toolbar">');
    $('body').append(toolbar);

    var onStatus = function(msg) {
      $('.status', playerDiv).text(msg);
    };
    var btnPause = $('<button class="pause">||</button>');
    var btnNext = $('<button class="next">&gt;&gt;|</button>');
    var btnSearch = $('<button class="search">search</button>');
    btnPause.click(function() {
      playerPause(pid, playerDiv, onStatus);
    });
    btnNext.click(function() {
      playerNext(pid, playerDiv, onStatus);
    });
    btnSearch.click(function() {
      showSearch(pid);
    });
    toolbar.append(btnPause);
    toolbar.append(btnNext);
    toolbar.append(btnSearch);
  }

  function getPlayers(onStatus, onPlayers) {
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

  function getPlayer(pid, onStatus, onPlayer) {
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

  function parsePlayerNode(node) {
    var player = {};
    player.pid = node.find('playerid').text();
    player.name = node.find('playername').text();
    player.title = node.find('title').text();
    player.state = node.find('playstate').text();
    player.stateName = playerStateToLabel(parseInt(player.state));

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

  function playerPlaybackOrder(pid, order, playerDiv, onStatus, onComplete) {
    writePlayerState(pid, 'playbackorder&order=' + order.id, onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player, true);
      onComplete();
    });
  }

  function playerFullscreen(pid, monitor, playerDiv, onStatus, onComplete) {
    writePlayerState(pid, 'fullscreen&monitor=' + monitor.id, onStatus, function(player) {
      updatePlayerDisplay(playerDiv, player, true);
      onComplete();
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
        onStatus('Error: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
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
      updateQueueItemDisplay(queueDiv, queue, itemDiv, item);
      allItemIds[item.id] = true;
    });

    $('.item', queueDiv).each(function() {
      var item = $(this);
      var itemId = item.data('id');
      if (!allItemIds[itemId]) {
        item.remove();
      }
    });

    // FIXME I am sure there must be a more efficient way to do this.
    var currentElements = {};
    $('.item', queueDiv).each(function() {
      var item = $(this);
      currentElements[item.attr('id')] = item;
    });
    var newElements = [];
    $.each(queue.items, function(index, item) {
      newElements.push(currentElements['qitem' + item.id]);
    });
    queueDiv.append(newElements);
  }

  function makeQueueItem(itemDiv, id) {
    itemDiv.empty();
    itemDiv.data('id', id);
    var title = $('<p class="title">');
    var a = $('<a class="clickable" href="">');
    a.append(title);
    itemDiv.append(a);
  }

  function updateQueueItemDisplay(queueDiv, queue, itemDiv, item) {
    $('.title', itemDiv).text(item.title);
    var clickable = $('.clickable', itemDiv);
    clickable.unbind();
    clickable.click(function(event) {
      event.preventDefault();
      queueItemClicked(queueDiv, queue, item);
    });
  }

  function getQueue(pid, onStatus, onQueue) {
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

  function writeQueueItem(pid, item, action, onStatus, onQueue) {
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

  function showPlayerMenu(playerDiv, player) {
    var existingMenu = $('.playermenu');
    if (existingMenu.size() > 0) {
      existingMenu.remove();
    }
    else {
      $('body').append(makePlayerMenu(playerDiv, player));
    }
  }

  function makePlayerMenu(playerDiv, player) {
    var menu = $('<div class="popup playermenu">');
    var closeAction = function() {
      menu.remove();
    };

    var title = $('<p class="title">');
    title.text(player.name);
    menu.append(title);

    var queueBtns = [];
    var fullscreenBtns = [];

    // Add play order button.
    var orderBtn = $('<button>playback order</button>');
    orderBtn.click(function() {
      orderBtn.attr('disabled', 'true');
      removeAll(queueBtns);
      removeAll(fullscreenBtns);
      $.each(PLAYBACK_ORDERS, function(index, order) {
        var btn = $('<button>');
        btn.text(order.title);
        btn.click(function() {
          var status = $('<p>');
          btn.after(status);
          playerPlaybackOrder(player.pid, order, playerDiv, function(msg) {
            status.text(msg);
          }, closeAction);
        });
        orderBtn.after(btn);
      });
    });
    menu.append(orderBtn);

    // Add queue buttons.
    var queueActions = ['clear', 'shuffle'];
    $.each(queueActions, function(index, action) {
      var btn = $('<button>' + action + ' queue</button>');
      btn.click(function() {
        var status = $('<p>');
        btn.after(status);
        var queueDiv = $('.queue'); // TODO avoid this.
        queueMenuItemAction(queueDiv, player.pid, null, action, status, closeAction);
      });
      menu.append(btn);
      queueBtns.push(btn);
    });

    // Add full screen buttons.
    $.each(player.monitors, function(index, monitor) {
      var btn = $('<button>');
      btn.text('fullscreen ' + monitor.id + ' (' + monitor.name + ')');
      btn.click(function() {
        var status = $('<p>');
        btn.after(status);
        playerFullscreen(player.pid, monitor, playerDiv, function(msg) {
          status.text(msg);
        }, closeAction);
      });
      menu.append(btn);
      fullscreenBtns.push(btn);
    });

    var close = $('<button class="close">close</button>');
    menu.append(close);
    close.click(closeAction);

    return menu;
  }

  function showSearch(pid) {
    var dlg = $('<div class="popup searchdlg">');

    var title = $('<p class="title">');
    title.text('Search...');
    dlg.append(title);

    var txtSearch = $('<input type="text">');
    txtSearch.keyup(function(event) {
      if (event.keyCode === 13) {
        btnSearch.click();
      }
    });
    dlg.append(txtSearch);

    var sortColumn = $('<select>');
    $.each(['date_last_played', 'path', 'date_added', 'start_count', 'end_count', 'duration'], function(index, column) {
      var opt = $('<option>');
      opt.text(column);
      opt.attr('value', column)
      sortColumn.append(opt);
    });
    dlg.append(sortColumn);

    var sortOrder = $('<select>');
    $.each(['desc', 'asc'], function(index, order) {
      var opt = $('<option>');
      opt.text(order);
      opt.attr('value', order)
      sortOrder.append(opt);
    });
    dlg.append(sortOrder);

    var btnSearch = $('<button>search</button>');
    dlg.append(btnSearch);

    var status = $('<p>');
    dlg.append(status);

    var close = $('<button class="close">close</button>');
    dlg.append(close);

    close.click(function() {
      dlg.remove();
    });

    getPlayer(pid, function(msg) {
      status.text(msg);
    }, function(player) {
      title.text('Search ' + player.listTitle);
      btnSearch.click(function() {
        if (!player.mid) {
          alert("No list selected.");
          return;
        }

        var href = '/?mid=' + player.mid + '&tpid=' + player.pid + '&search=' + encodeURIComponent(txtSearch.val());
        if (player.listView) href += '&listview=' + encodeURIComponent(player.listView);
        href += "&search_column=" + sortColumn.val();
        href += "&search_order=" + sortOrder.val();
        window.location.href = href;
      });
    });

    $('body').append(dlg);
    txtSearch.focus();
  }

  function queueItemClicked(queueDiv, queue, item) {
    var existingMenu = $('.itemmenu');
    if (existingMenu.size() > 0) {
      existingMenu.remove();
    }
    else {
      $('body').append(makeQueueItemMenu(queueDiv, queue, item));
    }
  }

  function makeQueueItemMenu(queueDiv, queue, item) {
    var menu = $('<div class="popup itemmenu">');
    var closeAction = function() {
      menu.remove();
    };

    var title = $('<p class="title">');
    title.text(item.title);
    menu.append(title);

    var actions = ['top', 'up', 'remove', 'down', 'bottom'];
    $.each(actions, function(index, action) {
      var btn = $('<button>');
      btn.text(action);
      btn.click(function() {
        var status = $('<p>');
        btn.after(status);
        queueMenuItemAction(queueDiv, queue.pid, item, action, status, closeAction);
      });
      menu.append(btn);
    });

    var close = $('<button class="close">close</button>');
    menu.append(close);
    close.click(closeAction);

    return menu;
  }

  function queueMenuItemAction(queueDiv, pid, item, action, statusElem, onComplete) {
    writeQueueItem(pid, item, action, function(msg) {
      statusElem.text(msg);
    }, function(queue) {
      displayQueue(queueDiv, queue);
      onComplete();
    });
  }

  function removeAll(elems) {
      $.each(elems, function(index, elem) {
        elem.remove();
      });
  }

})();
