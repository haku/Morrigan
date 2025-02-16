(function() {

  var REFRESH_PLAYERS_SECONDS = 3;
  var SNACKBAR_SHOW_MILLIS = 10000;
  var HOST_NAME;

  var selectedPlayer;
  var lastQueuePid;
  var lastQueueVersion;
  var currentDbListRef;
  var currentDbNodeId;
  var currentDbQuery;
  var currentDbResults;
  var tagEditorListRef;

  $(document).ready(function() {
    updatePageTitle();
    startPoller();
    setDbTabToDbs();

    wireTabsAndMenus();
    wirePlayerTab();
    wireFooter();
    populateQueueAddViewMenu();
  });

  function updatePageTitle() {
    $.ajax({
      type: 'GET',
      cache: false,
      url: 'hostinfo',
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

  function updatePlayers() {
    try {
      fetchAndDisplayPlayers();
      return fetchAndDisplayPlayer();
    }
    catch (e) {
      console.log('Update failed.', e);
      return null;
    }
  }

  function startPoller() {
    var p = updatePlayers();
    if (p) {
      p.always(function() {
        setTimeout(startPoller, REFRESH_PLAYERS_SECONDS * 1000);
      });
    }
    else {
      setTimeout(startPoller, REFRESH_PLAYERS_SECONDS * 1000);
    }
  }

  var shackbarVisible = false;
  function showToast(msg) {
    if (shackbarVisible === true) return;

    var bar = $('.mdl-js-snackbar');
    if (bar.length > 0) bar = bar.get(0).MaterialSnackbar;
    if (bar) bar.showSnackbar({message: msg, timeout: SNACKBAR_SHOW_MILLIS});

    shackbarVisible = true;
    setTimeout(function(){
      shackbarVisible = false;
    }, SNACKBAR_SHOW_MILLIS);
  }

  var msgHandler = {
    onInfo: function(msg) {
      if (msg && msg.length > 0) {
        console.log(msg);
      }
    },
    onError: function(msg) {
      if (msg && msg.length > 0) {
        console.log(msg);
        showToast(msg);
      }
    }
  };

  function showPopup(popup) {
    $('#popup-obfuscator').addClass('is-visible').one('click', function(event) {
      hidePopup(popup);
      return false;
    });
    popup.show();
  }

  function hidePopup(popup) {
    popup.hide();
    $('#popup-obfuscator').removeClass('is-visible');
  }

  function showProgress(done, total) {
    var prg = $('#progress');
    var msg = $('.msg', prg);
    msg.text(done + '/' + total);
    if (done === total) {
      prg.addClass('fadeout');
    }
    else {
      prg.removeClass('fadeout').show();
    }
  }

// Sidebar.

  function fetchAndDisplayPlayers() {
    MnApi.getPlayers(msgHandler, displayPlayers);
  }

  function displayPlayers(players) {
    var allPlayerIds = [];
    var allPlayerDomIds = [];
    var playersList = $('#players_list');

    $.each(players, function(index, player) {
      allPlayerIds.push(player.pid);
      var domId = 'player_' + player.pid;
      allPlayerDomIds.push(domId);

      var playerElem = $('#' + domId);
      if (playerElem.size() < 1) {
        playerElem = $(
          '<a class="mdl-navigation__link player" href="">'
          + '<span class="mdl-button mdl-js-button mdl-button--icon">'
          + '<i class="material-icons">stop</i>'
          + '</span>'
          + '<span class="name">Name</span>'
          + '</a>');
        playerElem.attr('id', domId);
        playerElem.unbind().click(function(event) {
          event.preventDefault();
          setSelectedPlayerByUser(player);
          $('.mdl-layout__drawer').removeClass('is-visible');
          $('.mdl-layout__obfuscator').removeClass('is-visible');
        });
        playersList.append(playerElem);
      }

      $('.name', playerElem).updateText(player.name);
      $('.material-icons', playerElem).updateText(player.stateIcon);
    });

    $('.player', playersList).each(function(index, player) {
      if ($.inArray(player.id, allPlayerDomIds) < 0) player.remove();
    });

    if (selectedPlayer && $.inArray(selectedPlayer.pid, allPlayerIds) < 0) selectedPlayer = null;
    if (!selectedPlayer && players.length > 1) {
      var pid = localStorage.selectedPlayerPid;
      if (pid) {
        $.each(players, function(index, player) {
          if (pid === player.pid) {
            setSelectedPlayer(player);
            return false;
          }
        });
      }
    }
    if (!selectedPlayer && players.length > 1) {
      var sortedPlayers = players.sort();
      var PRIORITY = [3,0,2,1] // stopped, playing, paused, loading.
      sortedPlayers.sort(function(a, b) {return PRIORITY[a.state] - PRIORITY[b.state]});
      setSelectedPlayer(sortedPlayers[0]);
    }
    if (!selectedPlayer && players.length > 0) setSelectedPlayer(players[0]);
  }

// Footer.

  function wireFooter() {
    if (new URLSearchParams(location.search).has('nofooter')) {
      $('body').addClass('nofooter');
    }

    $('#footer_search').click(footerSearchClicked);
    ClickHelper.setupLongClick($("#footer_pause"), footerPauseClicked, footerPauseLongClicked);
    $('#footer_next').click(footerNextClicked);
  }

  function footerSearchClicked() {
    if (selectedPlayer && selectedPlayer.listRef) {
      restoreSavedSearch(selectedPlayer.listRef);
      setDbTabToSearch(selectedPlayer.listRef, selectedPlayer.listView);
      $('#fixed_tab_db span').click();
      $('#db_query').select();
      $('#db_query').focus();
    }
  }

  function footerPauseClicked() {
    if (!selectedPlayer) return;
    MnApi.playerPause(selectedPlayer.pid, msgHandler, displayPlayer);
  }

  function footerPauseLongClicked() {
    if (!selectedPlayer) return;
    MnApi.playerStop(selectedPlayer.pid, msgHandler, displayPlayer);
    showToast('Stopping...');
  }

  function footerNextClicked() {
    if (!selectedPlayer) return;
     MnApi.playerNext(selectedPlayer.pid, msgHandler, displayPlayer);
  }

// Tabs and menu.

  function wireTabsAndMenus() {
    var footer = $('#footer');
    var mnuItemsQueue = $('.mnu_item_queue');
    var mnuItemsDb = $('.mnu_item_db');

    $('#fixed_tab_queue').click(function() {
      mnuItemsQueue.show();
      mnuItemsDb.hide();
      footer.removeClass('maybehide');
    });

    $('#fixed_tab_db').click(function() {
      mnuItemsQueue.hide();
      mnuItemsDb.show();
      footer.addClass('maybehide');
    });

    $('#mnu_tags').click(tagsClicked);
    $('#mnu_clear_queue').click(clearQueueClicked);
    $('#mnu_shuffle_queue').click(shuffleQueueClicked);
    $('#mnu_enqueue_view').click(enqueueViewClicked);

    $.each(MnApi.PLAYBACK_ORDERS, function(index, order) {
      $('#mnu_playback_order_' + order.id.toLowerCase()).click(function() {
        if (!selectedPlayer) return;
        MnApi.playerPlaybackOrder(selectedPlayer.pid, order, msgHandler, displayPlayer);
      });
    });

    $.each(MnApi.TRANSCODES, function(index, tc) {
      var li = $('<li class="mdl-menu__item">');
      li.text(tc.title);
      li.click(function() {
        if (!selectedPlayer) return;
        MnApi.playerTranscode(selectedPlayer.pid, tc, msgHandler, displayPlayer);
      });
      $('#submnu_transcode').append(li);
    });

    $('#queue_head_add').click(queueAddHeadViewClicked);
    $('#queue_tail_add').click(queueAddTailViewClicked);

    $('#volume_down').click(function() { setRelativeVolume(-3); });
    $('#volume_up').click(function() { setRelativeVolume(3); });

    setupTagAutocomplete($('#db_query'), true, function(){return currentDbListRef});
    setupTagAutocomplete($('#new_tag'), false, function(){return tagEditorListRef});
  }

  function tagsClicked() {
    if (!selectedPlayer || !selectedPlayer.item) return;
    showTagEditor(selectedPlayer.item);
  }

  function clearQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'clear', msgHandler, displayQueue);
  }

  function shuffleQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'shuffle', msgHandler, displayQueue);
  }

  function setRelativeVolume(offset) {
    if (!selectedPlayer) return;
    var newVolume = selectedPlayer.volume + offset;
    newVolume = Math.min(newVolume, selectedPlayer.volumemaxvalue);
    newVolume = Math.max(newVolume, 0);
    MnApi.playerSetVolume(selectedPlayer.pid, newVolume, msgHandler, displayPlayer);
  }

  function populateQueueAddViewMenu() {
    MnApi.getSavedViews(msgHandler, addSavedViewsToMenu);
  }

  function addSavedViewsToMenu(savedViews) {
    var menu = $('#queue_tail_add_menu');
    $.each(savedViews, function(index, item) {
      var addToQueue = $('<button class="mdl-button mdl-js-button mdl-js-ripple-effect pri">');
      if (item.name && item.listref && (item.query || item.query === '')) {
        addToQueue.text(item.name);
        addToQueue.unbind().click(function() {
          enqueueView(item.listref, null, item.query);
          hidePopup(menu);
        });
      }
      else {
        addToQueue.text('Invalid:' + JSON.stringify(item));
      }
      menu.append(addToQueue);
    });
  }

  function queueAddHeadViewClicked() {
    const menu = $('#queue_head_add_menu');
    $('.add_stop', menu).unbind().click(() => {
      if (!selectedPlayer) return;
      MnApi.writeQueueItem(selectedPlayer.pid, null, 'add_stop_top', msgHandler, displayQueue);
      hidePopup(menu);
    });
    showPopup(menu);
  }
  function queueAddTailViewClicked() {
    showPopup($('#queue_tail_add_menu'));
  }

  function enqueueViewClicked() {
    if (!currentDbListRef || !currentDbResults) return;
    enqueueView(currentDbListRef, currentDbNodeId, currentDbQuery);
  }

  function enqueueView(listRef, nodeId, query) {
    if (!selectedPlayer) return;
    MnApi.enqueueView(listRef, nodeId, query, selectedPlayer.pid, msgHandler, function(msg) {
      console.log(msg);
      fetchAndDisplayQueue();
    });
  }

  function setupTagAutocomplete(el, isSearch, listRefSup) {
    var source = function(req, resp) {
      $.ajax({
        dataType: "json",
        url: 'mlists/' + listRefSup() + '/tags?term=' + encodeURIComponent(req.term),
        success: function(data) {
          if (!el.data('sent')) {
            if (isSearch) fillInTagSearches(data);
            resp(data);
          }
          else {
            resp();
          }
        },
        error: function(jqXHR, textStatus, errorThrown) {
          msgHandler.onError('Error fetching tags: ' + ErrorHelper.summarise(jqXHR, textStatus, errorThrown));
          resp();
        }
      });
    }
    el.autocomplete({source: source, minLength: 1});
  }

  function fillInTagSearches(data) {
    $.each(data, function(index, item) {
      var val = item['value'];
      var quote;
      if (val.indexOf(' ') >= 0) {
        if (val.indexOf('"') >= 0) {
          if (val.indexOf("'") >= 0) {
            val = val.replace("'", "\\'");
          }
          quote = "'";
        }
        else {
          quote = '"';
        }
      }
      else {
        quote = '';
      }
      item['value'] = 't=' + quote + val + quote;
    });
  }

  function onAutocompleteKeyup(event) {
    if (event.keyCode == 13) {
      $(event.target).autocomplete('close').data('sent', true);
    }
    else {
      $(event.target).data('sent', false);
    }
  }

// Player tab.

  function wirePlayerTab() {
    var sldJ = $('#track_progress');
    var sld = sldJ.get(0);
    var dlg = $('#seek_dlg');
    var dlgSldJ = $('.seek_slider', dlg);
    var dlgSld = dlgSldJ.get(0);

    var time = $('.seek_time', dlg);
    var dlgSldOnChange = function () {
      time.text(MnApi.formatSeconds(dlgSld.value));
    }
    dlgSld.addEventListener('input', dlgSldOnChange);

    var sldOnClick = function() {
      dlgSldJ.attr('max', sldJ.attr('max'));
      dlgSld.MaterialSlider.change(sld.value);
      dlgSldOnChange();
      showPopup(dlg);
    }
    sldJ.click(sldOnClick);
    sld.addEventListener('touchend', sldOnClick);
    sld.addEventListener('mdl-componentupgraded', function() {
      sld.parentElement.click(sldOnClick);
    });

    $('button.goto', dlg).unbind().click(function() {
      var position = dlgSld.value;
      MnApi.playerSeek(selectedPlayer.pid, position, msgHandler, displayPlayer);
      hidePopup(dlg);
    });
  }

  function setSelectedPlayer(player) {
    selectedPlayer = player;
    fetchAndDisplayPlayer();
  }

  function setSelectedPlayerByUser(player) {
    setSelectedPlayer(player);
    localStorage.selectedPlayerPid = player.pid;
  }

  function fetchAndDisplayPlayer() {
    if (!selectedPlayer) return null;
    return MnApi.getPlayer(selectedPlayer.pid, msgHandler, displayPlayer);
  }

  function displayPlayer(player) {
    // Keep selectedPlayer up to date.
    if (selectedPlayer.pid === player.pid) {
      if (!player.item) {
        player.item = selectedPlayer.item;
      }
      selectedPlayer = player;
    }

    var playorder = MnApi.playbackOrderFromId(player.playOrderId)['title'];
    if (player.playOrderOverrideId) playorder += ' (' +
      MnApi.playbackOrderFromId(player.playOrderOverrideId)['title'] + ')';
    $('#subtitle_playback_order').updateText(playorder);

    $('#player_name').updateText(player.name + ' (' + HOST_NAME + ')');
    $('#queue_tab_icon').updateText(player.stateIcon);
    $('#subtitle_list_name').updateText(player.listTitle);
    $('#subtitle_transcode').updateText(player.transcodeTitle);
    $('#track_title').updateText(player.trackTitle);
    if (player.listTitle != player.trackListTitle) {
      $('#track_list_title').updateText(player.trackListTitle);
    }
    else {
      $('#track_list_title').updateText('');
    }
    $('#track_tags').updateText(player.tags.length > 0 ? player.tags.join(', ') : '(no tags)');
    $('#queue_info').updateText(player.queueLength + ' items, ' + MnApi.formatSeconds(player.queueDuration));

    var pos, dur, sldTxt, sldVal, sldMax;

    pos = player.position;
    if (!pos || pos < 0) pos = 0;

    dur = player.duration;
    if ((!dur || dur <= 0) && player.item) dur = player.item.duration;
    if (!dur || dur < 0) dur = 0;

    if (pos > 0) {
      sldTxt = MnApi.formatSeconds(pos);
      if (dur > 0) sldTxt += ' / ' +  MnApi.formatSeconds(dur);
    }
    else {
      sldTxt = '-:--';
    }

    if (pos > 0 && dur > 0) {
      sldVal = pos;
      sldMax = dur;
    }
    else {
      sldVal = 0;
      sldMax = 100;
    }

    var sld = $('#track_progress').attr('max', sldMax).get(0).MaterialSlider;
    if (sld) sld.change(sldVal);
    $('#track_time').updateText(sldTxt);

    if (player.volume) {
      var vol = player.volume;
      if (player.volumemaxvalue === 100) {
        vol += '%';
      }
      else {
        vol += '/' + player.volumemaxvalue;
      }
      $('#volume_level').updateText(vol);
      $('#volume_controls').show();
    }
    else {
      $('#volume_controls').hide();
    }

    if (lastQueuePid !== player.pid || lastQueueVersion !== player.queueVersion) fetchAndDisplayQueue();
  }

  function fetchAndDisplayQueue() {
    if (!selectedPlayer) return;
    MnApi.getQueue(selectedPlayer.pid, msgHandler, displayQueue);
  }

  function displayQueue(queue) {
    var queueList = $('#queue_list');
    var allItemIds = {};

    $.each(queue.items, function(index, item) {
      var domId = 'q' + item.id;
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

    // Force order of queue elements.
    var idToIndex = {};
    $.each(queue.items, function(index, item) {
      idToIndex['q' + item.id] = index;
    });
    $('.item', queueList).sort(function(a, b) {
      return idToIndex[a.id] - idToIndex[b.id];
    }).appendTo(queueList);

    // Remember.
    lastQueuePid = queue.pid;
    lastQueueVersion = queue.version;
  }

  function makeQueueItem(itemId, domId) {
    var itemEl = $('<li class="item">');
    itemEl.attr('id', domId);
    itemEl.data('id', itemId);
    itemEl.append($('<a class="clickable title" href="">'));
    return itemEl;
  }

  function updateQueueItem(item, itemEl) {
    var title = item.title;
    if (item.duration > 0) title += ' (' + MnApi.formatSeconds(item.duration) + ')';
    $('.title', itemEl).updateText(title);

    var clickable = $('.clickable', itemEl);
    clickable.unbind().click(function(event) {
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
        MnApi.writeQueueItem(selectedPlayer.pid, item, action, msgHandler, displayQueue);
        hidePopup(menu);
      });
    });
    showPopup(menu);
  }

// DB tab.

  function setDbTabToDbs() {
    currentDbListRef = null;
    currentDbNodeId = null;
    currentDbQuery = null;
    currentDbResults = null;
    $('#db_fab').hide();

    MnApi.getDbs(msgHandler, displayDbs);
    $('#db_title').text('Fetching...');
    $('#db_subtitle').text('');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function restoreSavedSearch(listRef) {
    var prevQuery = JSON.parse(localStorage['query:' + listRef] || '{}');

    var query = prevQuery.query;
    if (typeof(query) == "undefined") query = "";
    $('#db_query').val(query).parent().addClass('is-dirty'); // FIXME https://github.com/google/material-design-lite/issues/903

    var sortColumn = prevQuery.sortColumn;
    if (sortColumn) $('#db_sort_column').val(sortColumn);

    var sortOrder = prevQuery.sortOrder;
    if (sortOrder) $('#db_sort_order').val(sortOrder);
  }

  function writeSavedSearch() {
    localStorage['query:' + currentDbListRef] = JSON.stringify({
      query: $('#db_query').val(),
      sortColumn: $('#db_sort_column').val(),
      sortOrder: $('#db_sort_order').val(),
    });
  }

  function setDbTabToNode(listRef, nodeId) {
    currentDbListRef = listRef;
    currentDbNodeId = nodeId;
    currentDbQuery = null;
    currentDbResults = null;
    $('#db_fab').hide();

    $('#db_title').text('Fetching...');
    $('#db_subtitle').text('');
    $('#db_list').empty();
    MnApi.getNode(listRef, nodeId, msgHandler, displayNode);

    $('#db_go_back').unbind().click(function(){setDbTabToDbs()});
    $('#db_query').off('keyup').on('keyup', function(event) {
      onAutocompleteKeyup(event);
      if (event.keyCode == 13) {
        setDbTabToSearch(listRef);
      }
    });
    $('#db_sort_column').unbind();
    $('#db_sort_order').unbind();
    $('#db_include_disabled').unbind().click(function(event){
      var current_icon = $('#db_include_disabled .material-icons').text();
      var new_icon = 'deleted';
      if (current_icon === 'deleted') new_icon = 'delete_outline';
      $('#db_include_disabled .material-icons').text(new_icon);
    });
    $('#db_go_tags').unbind().click(function(){setDbTabToTags(listRef, view)});
    $('#db_go_albums').unbind().click(function(){setDbTabToAlbums(listRef, view)});
    $('#db_sort_options').show();
  }

  function setDbTabToSearch(listRef, view, query) {
    if (query) {
      $('#db_query').val(query).parent().addClass('is-dirty'); // FIXME https://github.com/google/material-design-lite/issues/903
    }
    else {
      query = $('#db_query').val();
    }

    var sortColumn = $('#db_sort_column').val();
    var sortOrder = $('#db_sort_order').val();
    var includeDisabled = $('#db_include_disabled .material-icons').text() === 'deleted';

    currentDbListRef = listRef;
    currentDbNodeId = null;
    currentDbQuery = query;
    currentDbResults = null;
    $('#db_fab').hide();
    writeSavedSearch();

    MnApi.getQuery(listRef, view, query, sortColumn, sortOrder, includeDisabled, msgHandler, displayResults);

    $('#db_title').text('Fetching...');
    $('#db_subtitle').text('');
    $('#db_list').empty();
    $('#db_breadcrumbs').empty();
    // TODO show spinner.

    $('#db_go_back').unbind().click(function(){setDbTabToDbs()});
    $('#db_query').off('keyup').on('keyup', function(event) {
      onAutocompleteKeyup(event);
      if (event.keyCode == 13) {
        setDbTabToSearch(listRef, view);
      }
    });
    $('#db_sort_column').unbind().change(function(){setDbTabToSearch(listRef, view)});
    $('#db_sort_order').unbind().change(function(){setDbTabToSearch(listRef, view)});
    $('#db_include_disabled').unbind().click(function(event){
      var current_icon = $('#db_include_disabled .material-icons').text();
      var new_icon = 'deleted';
      if (current_icon === 'deleted') new_icon = 'delete_outline';
      $('#db_include_disabled .material-icons').text(new_icon);
      setDbTabToSearch(listRef, view);
    });
    $('#db_go_tags').unbind().click(function(){setDbTabToTags(listRef, view)});
    $('#db_go_albums').unbind().click(function(){setDbTabToAlbums(listRef, view)});
    $('#db_sort_options').show();
  }

  function setDbTabToTags(listRef, view) {
    currentDbListRef = listRef;
    currentDbNodeId = null;
    currentDbQuery = null;
    currentDbResults = null;
    $('#db_fab').hide();

    MnApi.getTags(listRef, view, msgHandler, displayTags);

    $('#db_title').text('Fetching...');
    $('#db_subtitle').text('');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function setDbTabToAlbums(listRef, view) {
    currentDbListRef = listRef;
    currentDbNodeId = null;
    currentDbQuery = null;
    currentDbResults = null;
    $('#db_fab').hide();

    MnApi.getAlbums(listRef, view, msgHandler, displayAlbums);

    $('#db_title').text('Fetching...');
    $('#db_subtitle').text('');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function displayDbs(dbs) {
    var dbList = $('#db_list');
    dbList.empty();
    var count = 0;
    $.each(dbs, function(index, db) {
      dbList.append(makeDbItem(db));
      count += 1;
    });

    $('#db_sort_options').hide();
    $('#db_title').text(count + ' Collections');
    $('#db_subtitle').text('');
    $('#db_breadcrumbs').empty();
  }

  function makeDbItem(db) {
    var a = $('<a class="clickable title" href="">');
    a.text(db.title);

    var el = $('<li class="item bigger">');
    el.append(a);

    if (db.hasRootNodes) {
      a.unbind().click(function(event) {
        event.preventDefault();
        restoreSavedSearch(db.listRef);
        setDbTabToNode(db.listRef, "0");
      });
    }
    else {
      a.unbind().click(function(event) {
        event.preventDefault();
        restoreSavedSearch(db.listRef);
        setDbTabToSearch(db.listRef);
      });
    }

    return el;
  }

  function displayNode(node) {
    $('#db_title').text(node.title);
    $('#db_subtitle').text('');

    var dbList = $('#db_list');
    dbList.empty();

    node.nodes.forEach(n => {
      dbList.append(makeNodeItem(n, node));
    });
    displayItems(node.items);
  }

  function makeNodeItem(node, parentNode) {
    var a = $('<a class="clickable title" href="">');
    a.text(node.title);

    var row = $('<li class="item">');
    row.append(a);

    a.unbind().click(function(event) {
      event.preventDefault();
      setDbTabToNode(node.listRef, node.id);

      var crumb = $('<a class="crumb" href="">');
      crumb.text(parentNode.title);
      crumb.unbind().click(function(event) {
        event.preventDefault();
        setDbTabToNode(parentNode.listRef, parentNode.nodeId);
        crumb.nextAll().remove();
        crumb.remove();
      });
      $('#db_breadcrumbs').append(crumb);
    });

    return row;
  }

  function displayResults(results, queryDurMillis) {
    $('#db_title').text(results.length + ' items');
    $('#db_subtitle').text(queryDurMillis + ' ms');

    var dbList = $('#db_list');
    dbList.empty();

    displayItems(results);
  }

  function displayItems(items) {
    currentDbResults = items;

    var dbList = $('#db_list');

    var selectedItems = new Set();
    var dbFab = $('#db_fab');
    $('button', dbFab).unbind().click(function(event) {
      event.preventDefault();
      showSelectionMenu(selectedItems);
    });

    var onSelectionChange = function() {
      if (selectedItems.size > 0) {
        dbFab.attr('data-badge', selectedItems.size);
        dbFab.show();
      }
      else {
        dbFab.hide();
      }
    };

    var invertSelection = function(item, row) {
      if (selectedItems.has(item)) {
        selectedItems.delete(item);
        row.removeClass('selected');
      }
      else {
        selectedItems.add(item);
        row.addClass('selected');
      }
      onSelectionChange();
    }

    var itemToRow = new Map();
    $.each(items, function(index, result) {
      var row = makeResultItem(result, selectedItems, invertSelection);
      dbList.append(row);
      itemToRow.set(result, row);
    });

    $('#mnu_select_all').unbind().click(function () {
      if (!currentDbResults) return;

      $('.item', dbList).addClass('selected');
      items.forEach(function(item) {
        selectedItems.add(item);
      });
      onSelectionChange();
    });

    $('#mnu_select_invert').unbind().click(function () {
      if (!currentDbResults) return;

      itemToRow.forEach(function(row, item) {
        invertSelection(item, row);
      });
    });
  }

  function makeResultItem(res, selectedItems, invertSelection) {
    var title = res.title;
    if (res.duration > 0) title += ' (' + MnApi.formatSeconds(res.duration) + ')';

    var a = $('<a class="clickable title" href="">');
    a.text(title);

    var row = $('<li class="item">');
    if (!res.enabled) row.addClass('disabled');
    row.append(a);

    var onClick = function(event) {
      event.preventDefault();
      if (selectedItems.size > 0) {
        invertSelection(res, row);
      }
      else if (res.remoteId) {
        setDbTabToSearch(res.listRef, res.view, res.remoteId);
      }
      else {
        showDbItemMenu(res, row);
      }
    }

    var onLongClick = function(event) {
      event.preventDefault();
      invertSelection(res, row);
    }

    a.unbind()
    ClickHelper.setupLongClick(a, onClick, onLongClick);

    return row;
  }

  function displayTags(tags) {
    $('#db_title').text(tags.length + ' tags');
    $('#db_subtitle').text('');

    var dbList = $('#db_list');
    dbList.empty();
    $.each(tags, function(index, tag) {
      dbList.append(makeTagItem(tag));
    });
  }

  function makeTagItem(tag) {
    var a = $('<a class="clickable title" href="">');
    a.text(tag.title);

    var el = $('<li class="item">');
    el.append(a);

    a.unbind().click(function(event) {
      event.preventDefault();
      setDbTabToSearch(tag.listRef, tag.view, tag.value);
    });

    return el;
  }

  function displayAlbums(albums) {
    $('#db_title').text(albums.length + ' albums');
    $('#db_subtitle').text('');

    var dbList = $('#db_list');
    dbList.empty();
    $.each(albums, function(index, album) {
      dbList.append(makeAlbumItem(album));
    });
  }

  function makeAlbumItem(album) {
    var a = $('<a class="clickable" href="">');

    var pic = $('<img class="cover">');
    if (album.resizedCoverUrl) {
      pic.attr('src', album.resizedCoverUrl);
    }
    else {
      pic.addClass('nocover');
    }
    a.append(pic);

    var title = $('<p class="title">');
    title.text(album.title);
    a.append(title);

    var el = $('<li class="album">');
    el.append(a);

    a.unbind().click(function(event) {
      event.preventDefault();
      showDbAlbumMenu(album);
    });

    return el;
  }

  function showDbItemMenu(item, row) {
    var menu = $('#db_item_menu');
    $('.title', menu).text(item.title);
    $('.title_link', menu).attr('href', item.url);
    $('.stats.l0', menu).text(item.startCount + '/' + item.endCount + ' ' + MnApi.formatSeconds(item.duration));
    $('.stats.l1', menu).text(item.dateLastPlayed);
    $('.tags', menu).text(item.tags.join(', '));

    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(item, null, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    $('.enqueue_top', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItemsTop(item, null, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    $('.edit_tags', menu).unbind().click(function(event) {
      hidePopup(menu);
      showTagEditor(item);
    });

    $('.set_enabled', menu).setVisibility(!item.enabled).unbind().click(function(event) {
      MnApi.setEnabled(item, true, msgHandler, function(msg) {
        console.log(msg);
        item.enabled = true;
        row.removeClass('disabled');
      });
      hidePopup(menu);
    });

    $('.set_disabled', menu).setVisibility(item.enabled).unbind().click(function(event) {
      MnApi.setEnabled(item, false, msgHandler, function(msg) {
        console.log(msg);
        item.enabled = false;
        row.addClass('disabled');
      });
      hidePopup(menu);
    });

    showPopup(menu);
  }

  function showSelectionMenu(selectedItems) {
    selectedItems = new Set(selectedItems);  // Copy to avoid later midifications.

    var menu = $('#db_selection_menu');
    $('.title', menu).text(selectedItems.size + ' Selected Items');

    var calls = 0;
    var enqueueCb = function(msg) {
      console.log(msg);
      calls += 1;
      showProgress(calls, selectedItems.size);
      if (calls === 1) {
        hidePopup(menu);
      }
      if (calls === selectedItems.size) {
        fetchAndDisplayQueue();
      }
    }

    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(selectedItems, null, selectedPlayer.pid, msgHandler, enqueueCb);
      showProgress(0, selectedItems.size);
    });

    $('.enqueue_top', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItemsTop(selectedItems, null, selectedPlayer.pid, msgHandler, enqueueCb);
      showProgress(0, selectedItems.size);
    });

    $('.edit_tags', menu).unbind().click(function(event) {
      hidePopup(menu);
      showMultiTagEditor(selectedItems);
    });

    showPopup(menu);
  }

  function showDbAlbumMenu(album) {
    var menu = $('#db_album_menu');
    $('.title', menu).text(album.title);
    $('.stats', menu).text(album.trackCount + ' tracks');

    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(album, null, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    $('.enqueue_top', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItemsTop(album, null, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    showPopup(menu);
  }

  function showTagEditor(item) {
    showMultiTagEditor(new Set([item]));
  }

  function showMultiTagEditor(selectedItems) {
    // They should all have the same listRef and view.
    var firstItem = selectedItems.values().next().value;
    var listRef = firstItem.listRef;
    var view = firstItem.view;
    tagEditorListRef = listRef;

    var tags = new Map();  // tag => [items]
    selectedItems.forEach(function(item, alsoItem, set) {
      item.tags.forEach(function(tag) {
        var a = tags.get(tag);
        if (!a) {
          a = [];
          tags.set(tag, a);
        }
        a.push(item);
      });
    });
    tags = new Map([...tags.entries()].sort((a, b) => b[1].length - a[1].length));

    var dlg = $('#tag_editor');
    var title = $('.title', dlg);
    if (selectedItems.size > 1) {
      title.text('Tags for ' + selectedItems.size + ' Items');
    }
    else {
      title.text(firstItem.title);
    }


    var makeWriteProgressCb = function(total, onComplete) {
      var calls = 0;
      showProgress(0, total);
      return function(msg) {
        console.log(msg);
        calls += 1;
        showProgress(calls, total);
        if (calls === total) onComplete();
      }
    }

    var updateRowText = function(searchBtn, tag, items) {
      if (selectedItems.size > 1) {
        searchBtn.text(tag + ' (' + items.length + ')');
      }
      else {
        searchBtn.text(tag);
      }
    }

    var makeTagRow = function(items, tag) {
      var row = $('<div class="row">');

      var search = $('<button class="mdl-button mdl-js-button mdl-js-ripple-effect pri">');
      updateRowText(search, tag, items);
      search.unbind().click(function() {
        setDbTabToSearch(listRef, view, 't=' + tag);
        hidePopup(dlg);
        $('#fixed_tab_db span').click();
      });

      var remove = $('<button class="mdl-button mdl-js-button mdl-js-ripple-effect aux"><i class="material-icons">delete</i></button>');
      remove.unbind().click(function() {
        if (!window.confirm("Tag: " + tag + "\n\nRemove?")) {
          return;
        }

        var writeCb = makeWriteProgressCb(items.length, function() {
          row.on('animationend', function() {row.remove();});
          row.addClass('fadeout');
          if (newTag.val().length < 1) {
            newTag.val(tag).parent().addClass('is-dirty'); // FIXME https://github.com/google/material-design-lite/issues/903;
            newTag.focus();
          }
        });
        MnApi.rmTag(items, tag, msgHandler, function(msg, item) {
          writeCb(msg);
          item.tags = jQuery.grep(item.tags, function(val){return val != tag});
          items = jQuery.grep(items, function(val) {return val != item});
          updateRowText(search, tag, items);
        });
      });

      row.append(search);
      row.append(remove);
      return row;
    }

    var newTag = $('#new_tag', dlg);
    newTag.off('keyup').on('keyup', function(event) {
      onAutocompleteKeyup(event);
      if (event.keyCode == 13) {
        var tag = newTag.val();
        var items = [];

        var row = makeTagRow(items, tag);
        // TODO remove row if there is an error.
        // TODO check for existing row for this tag.
        var existingTopRow = $('.row', dlg).first();
        if (existingTopRow.length > 0) {
          existingTopRow.before(row);
        }
        else {
          dlg.append(row);
        }

        var writeCb = makeWriteProgressCb(selectedItems.size, function() {
          newTag.val('').focus();
        });

        MnApi.addTag(selectedItems, tag, msgHandler, function(msg, item) {
          writeCb(msg);
          item.tags.unshift(tag);
          items.push(item);
          updateRowText($('.pri', row), tag, items);
        });
      }
    });

    $('.row', dlg).remove();
    tags.forEach(function(items, tag) {
      var row = makeTagRow(items, tag);
      dlg.append(row);
    });

    showPopup(dlg);
  }

})();
