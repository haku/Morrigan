(function() {

  var REFRESH_PLAYERS_SECONDS = 5;
  var HOST_NAME;

  var selectedPlayer;
  var currentDbMid;
  var currentDbQuery;
  var currentDbResults;

  $(document).ready(function() {
    updatePageTitle();
    startPoller();
    setDbTabToDbs();

    wireTabsAndMenus();
    wireFooter();
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
          HOST_NAME = hostName;
          document.title = hostName + "'s Morrigan";
          $('#drawer_title').text(hostName);
        }
      }
    });
  }

  function startPoller() {
    fetchAndDisplayPlayers();
    setInterval(function() {
      fetchAndDisplayPlayers();
      fetchAndDisplayPlayer();
    }, REFRESH_PLAYERS_SECONDS * 1000);
  }

  function onStatus(msg) {
    if (msg && msg.length > 0) console.log(msg);
  }

// Sidebar.

  function fetchAndDisplayPlayers() {
    MnApi.getPlayers(onStatus, displayPlayers);
  }

  function displayPlayers(players) {
    $.each(players, function(index, player) {
      if (!selectedPlayer) setSelectedPlayer(player); // TODO also if player gone.

      var playerElem = $('#player_' + player.pid);
      if (playerElem.size() < 1) {
        playerElem = $(
          '<a class="mdl-navigation__link" href="">'
          + '<span class="mdl-button mdl-js-button mdl-button--icon">'
          + '<i class="material-icons">stop</i>'
          + '</span>'
          + '<span class="name">Name</span>'
          + '</a>');
        playerElem.attr('id', 'player_' + player.pid);
        playerElem.unbind().click(function(event) {
          event.preventDefault();
          setSelectedPlayer(player);
          $('.mdl-layout__drawer').removeClass('is-visible');
          $('.mdl-layout__obfuscator').removeClass('is-visible');
        });
        $('#players_list').append(playerElem);
      }
      $('.name', playerElem).text(player.name);
      $('.material-icons', playerElem).text(player.stateIcon);
    });
    // TODO remove gone players.
  }

// Footer.

  function wireFooter() {
    $('#footer_search').click(footerSearchClicked);
    $('#footer_pause').click(footerPauseClicked);
    $('#footer_next').click(footerNextClicked);
  }

  function footerSearchClicked() {
    if (selectedPlayer && selectedPlayer.mid) {
      setDbTabToSearch(selectedPlayer.mid, selectedPlayer.listView);
      $('#fixed_tab_db span').click();
      $('#db_query').focus();
    }
  }

  function footerPauseClicked() {
    if (!selectedPlayer) return;
    MnApi.playerPause(selectedPlayer.pid, onStatus, displayPlayer);
  }

  function footerNextClicked() {
    if (!selectedPlayer) return;
     MnApi.playerNext(selectedPlayer.pid, onStatus, displayPlayer);
  }

// Tabs and menu.

  function wireTabsAndMenus() {
    var footer = $('#footer');
    var playbackOrder = $('#mnu_playback_order');
    var clearQueue = $('#mnu_clear_queue');
    var shuffleQueue = $('#mnu_shuffle_queue');
    var enqueueAll = $('#mnu_enqueue_all');
    var enqueueView = $('#mnu_enqueue_view');

    $('#fixed_tab_queue').click(function() {
      footer.show();
      playbackOrder.show();
      clearQueue.show();
      shuffleQueue.show();
      enqueueAll.hide();
      enqueueView.hide();
    });

    $('#fixed_tab_db').click(function() {
      footer.hide();
      playbackOrder.hide();
      clearQueue.hide();
      shuffleQueue.hide();
      enqueueAll.show();
      enqueueView.show();
    });

    clearQueue.click(clearQueueClicked);
    shuffleQueue.click(shuffleQueueClicked);
    enqueueAll.click(enqueueAllClicked);
    enqueueView.click(enqueueViewClicked);

    $.each(MnApi.PLAYBACK_ORDERS, function(index, order) {
      $('#mnu_playback_order_' + order.id.toLowerCase()).click(function() {
        if (!selectedPlayer) return;
        MnApi.playerPlaybackOrder(selectedPlayer.pid, order, onStatus, displayPlayer);
      });
    });
  }

  function clearQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'clear', onStatus, displayQueue);
  }

  function shuffleQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'shuffle', onStatus, displayQueue);
  }

  function enqueueAllClicked() {
    if (!currentDbResults || !selectedPlayer) return;
    MnApi.enqueueItems(currentDbResults, selectedPlayer.listView, selectedPlayer.pid, onStatus, function(msg) {
      console.log(msg);
    });
  }

  function enqueueViewClicked() {
    if (!currentDbMid || !currentDbQuery || !currentDbResults || !selectedPlayer) return;
    MnApi.enqueueView(currentDbMid, currentDbQuery, selectedPlayer.pid, onStatus, function(msg) {
      console.log(msg);
    });
  }

// Player tab.

  function setSelectedPlayer(player) {
    selectedPlayer = player;
    fetchAndDisplayPlayer();
  }

  function fetchAndDisplayPlayer() {
    if (!selectedPlayer) return;
    MnApi.getPlayer(selectedPlayer.pid, onStatus, displayPlayer);
  }

  function displayPlayer(player) {
    $('#player_name').text(player.name + ' (' + HOST_NAME + ')');
    $('#queue_tab_icon').text(player.stateIcon);
    $('#subtitle_list_name').text(player.listTitle);
    $('#subtitle_playback_order').text(MnApi.PLAYBACK_ORDERS[player.playOrder]['title']);
    $('#track_title').text(player.trackTitle + ' (' + player.trackDuration + 's)');
    $('#track_tags').text(player.tags.length > 0 ? player.tags.join(', ') : '(no tags)');
    $('#queue_info').text(player.queueLength + ' items, ' + player.queueDuration + 's');

    fetchAndDisplayQueue(); // TODO only do this if queue in player has changed.
  }

  function fetchAndDisplayQueue() {
    if (!selectedPlayer) return;
    MnApi.getQueue(selectedPlayer.pid, onStatus, displayQueue);
  }

  function displayQueue(queue) {
    var queueList = $('#queue_list');
    var allItemIds = {};

    $.each(queue.items, function(index, item) {
      var domId = 'qitem' + item.id;
      var itemEl = $('#' + domId, queueList);
      if (itemEl.size() < 1) {
        itemEl = makeQueueItem(item.id, domId);
        queueList.append(itemEl);
      }
      updateQueueItem(item, itemEl);
      allItemIds[item.id] = true;
    });

    $('.item', queueList).each(function() {
      var item = $(this);
      var itemId = item.data('id');
      if (!allItemIds[itemId]) {
        item.remove();
      }
    });

    // For order of elements by appending them again in the right order.
    // FIXME I am sure there must be a more efficient way to do this.
    var currentElements = {};
    $('.item', queueList).each(function() {
      var item = $(this);
      currentElements[item.attr('id')] = item;
    });
    var newElements = [];
    $.each(queue.items, function(index, item) {
      newElements.push(currentElements['qitem' + item.id]);
    });
    queueList.append(newElements);
  }

  function makeQueueItem(itemId, domId) {
    var itemEl = $('<li class="item">');
    itemEl.attr('id', domId);
    itemEl.data('id', itemId);
    itemEl.append($('<a class="clickable title" href="">'));
    return itemEl;
  }

  function updateQueueItem(item, itemEl) {
    $('.title', itemEl).text(item.title);
    var clickable = $('.clickable', itemEl);
    clickable.unbind();
    clickable.click(function(event) {
      event.preventDefault();
      showQueueItemMenu(item);
    });
  }

  function showQueueItemMenu(item) {
    var menu = $('#queue_item_menu');
    $('.title', menu).text(item.title);
    $.each(['top', 'up', 'remove', 'down', 'bottom'], function(index, action) {
      $('.' + action, menu).unbind().click(function() {
        if (!selectedPlayer) return;
        MnApi.writeQueueItem(selectedPlayer.pid, item, action, onStatus, displayQueue);
        menu.hide();
      });
    });
    $('.close', menu).unbind().click(function(event) {
      menu.hide();
    });
    menu.show();
  }

// DB tab.

  function setDbTabToDbs() {
    currentDbMid = null;
    currentDbQuery = null;
    currentDbResults = null;

    MnApi.getDbs(onStatus, displayDbs);
    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function setDbTabToSearch(mid, view, query, sortColumn, sortOrder) {
    query ? $('#db_query').val(query) : query = $('#db_query').val();
    sortColumn ? $('#db_sort_column').val(sortColumn) : sortColumn = $('#db_sort_column').val();
    sortOrder ? $('#db_sort_order').val(sortOrder) : sortOrder = $('#db_sort_order').val();

    currentDbMid = mid;
    currentDbQuery = query;
    currentDbResults = null;

    MnApi.getQuery(mid, view, query, sortColumn, sortOrder, onStatus, displayResults);

    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.

    $('#db_go_back').unbind().click(function(){setDbTabToDbs()});
    $('#db_query').unbind().keyup(function(event){if (event.keyCode == 13) {setDbTabToSearch(mid, view)}});
    $('#db_sort_column').unbind().change(function(){setDbTabToSearch(mid, view)});
    $('#db_sort_order').unbind().change(function(){setDbTabToSearch(mid, view)});
    $('#db_sort_options').show();
  }

  function displayDbs(dbs) {
    $('#db_title').text(dbs.length + ' DBs');
    $('#db_sort_options').hide();

    var dbList = $('#db_list');
    dbList.empty();
    $.each(dbs, function(index, db) {
      if (db.type === "remote") return true;
      dbList.append(makeDbItem(db));
    });
  }

  function makeDbItem(db) {
    var a = $('<a class="clickable title" href="">');
    a.text(db.title);

    var el = $('<li class="item">');
    el.append(a);

    a.unbind();
    a.click(function(event) {
      event.preventDefault();
      setDbTabToSearch(db.mid);
    });

    return el;
  }

  function displayResults(results) {
    currentDbResults = results;

    $('#db_title').text(results.length + ' items');

    var dbList = $('#db_list');
    dbList.empty();
    $.each(results, function(index, result) {
      dbList.append(makeResultItem(result));
    });
  }

  function makeResultItem(res) {
    var a = $('<a class="clickable title" href="">');
    a.text(res.title + ' (' + res.duration + 's)');

    var el = $('<li class="item">');
    el.append(a);

    a.unbind();
    a.click(function(event) {
      event.preventDefault();
      showDbItemMenu(res);
    });

    return el;
  }

  function showDbItemMenu(item) {
    var menu = $('#db_item_menu');
    $('.title', menu).text(item.title);
    $('.stats', menu).text(item.startCount + '/' + item.endCount + ' ' + item.duration + 's');
    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(item, selectedPlayer.listView, selectedPlayer.pid, onStatus, function(msg) {
        console.log(msg);
        menu.hide();
      });
    });
    $('.close', menu).unbind().click(function(event) {
      menu.hide();
    });
    menu.show();
  }

})();
