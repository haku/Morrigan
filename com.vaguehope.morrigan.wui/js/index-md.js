(function() {

  var LONG_CLICK_MILLIS = 2000;
  var REFRESH_PLAYERS_SECONDS = 5;
  var HOST_NAME;

  var selectedPlayer;
  var lastQueuePid;
  var lastQueueVersion;
  var currentDbMid;
  var currentDbQuery;
  var currentDbResults;
  var tagEditorMid;

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
      try {
        fetchAndDisplayPlayers();
        fetchAndDisplayPlayer();
      }
      catch (e) {
        console.log('Update failed.', e);
      }
    }, REFRESH_PLAYERS_SECONDS * 1000);
  }

  function showToast(msg) {
    var bar = $('.mdl-js-snackbar');
    if (bar.length > 0) bar = bar.get(0).MaterialSnackbar;
    if (bar) bar.showSnackbar({message: msg, timeout: 10000});
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

      $('.name', playerElem).text(player.name);
      $('.material-icons', playerElem).text(player.stateIcon);
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
    $('#footer_search').click(footerSearchClicked);
    $('#footer_pause').click(footerPauseClicked);
    $('#footer_next').click(footerNextClicked);

    var pressTimer;
    $("#footer_pause").mouseup(function(){
      clearTimeout(pressTimer);
      return false;
    }).mousedown(function(){
      pressTimer = window.setTimeout(footerPauseLongClicked, LONG_CLICK_MILLIS);
      return false;
    });
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
    var tags = $('#mnu_tags');
    var stopAfter = $('#mnu_stop_after');
    var playbackOrder = $('#mnu_playback_order');
    var transcode = $('#mnu_transcode');
    var clearQueue = $('#mnu_clear_queue');
    var shuffleQueue = $('#mnu_shuffle_queue');
    var enqueueAll = $('#mnu_enqueue_all');
    var enqueueView = $('#mnu_enqueue_view');

    $('#fixed_tab_queue').click(function() {
      footer.show();
      tags.show();
      stopAfter.show();
      playbackOrder.show();
      transcode.show();
      clearQueue.show();
      shuffleQueue.show();
      enqueueAll.hide();
      enqueueView.hide();
    });

    $('#fixed_tab_db').click(function() {
      footer.hide();
      tags.hide();
      stopAfter.hide();
      playbackOrder.hide();
      transcode.hide();
      clearQueue.hide();
      shuffleQueue.hide();
      enqueueAll.show();
      enqueueView.show();
    });

    tags.click(tagsClicked);
    stopAfter.click(stopAfterClicked);
    clearQueue.click(clearQueueClicked);
    shuffleQueue.click(shuffleQueueClicked);
    enqueueAll.click(enqueueAllClicked);
    enqueueView.click(enqueueViewClicked);

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

    setupTagAutocomplete($('#db_query'), function(){return currentDbMid});
    setupTagAutocomplete($('#new_tag'), function(){return tagEditorMid});
  }

  function tagsClicked() {
    if (!selectedPlayer || !selectedPlayer.item) return;
    showTagEditor(selectedPlayer.item);
  }

  function stopAfterClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'add_stop_top', msgHandler, displayQueue);
  }

  function clearQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'clear', msgHandler, displayQueue);
  }

  function shuffleQueueClicked() {
    if (!selectedPlayer) return;
    MnApi.writeQueueItem(selectedPlayer.pid, null, 'shuffle', msgHandler, displayQueue);
  }

  function enqueueAllClicked() {
    if (!currentDbResults || !selectedPlayer) return;
    var enabledResults = jQuery.grep(currentDbResults, function(item){return item.enabled && item.url});
    MnApi.enqueueItems(enabledResults, selectedPlayer.listView, selectedPlayer.pid, msgHandler, function(msg) {
      console.log(msg);
    });
    showToast('Enqueueing items...');
  }

  function enqueueViewClicked() {
    if (!currentDbMid || !currentDbResults || !selectedPlayer) return;
    MnApi.enqueueView(currentDbMid, currentDbQuery, selectedPlayer.pid, msgHandler, function(msg) {
      console.log(msg);
    });
  }

  function setupTagAutocomplete(el, midSup) {
    var source = function(req, resp) {
      $.ajax({
        dataType: "json",
        url: '/mlists/' + midSup() + '/tags?term=' + encodeURIComponent(req.term),
        success: function(data) {
          if (!el.data('sent')) {
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

  function onAutocompleteKeyup(event) {
    if (event.keyCode == 13) {
      $(event.target).autocomplete('close').data('sent', true);
    }
    else {
      $(event.target).data('sent', false);
    }
  }

// Player tab.

  function setSelectedPlayer(player) {
    selectedPlayer = player;
    fetchAndDisplayPlayer();
  }

  function setSelectedPlayerByUser(player) {
    setSelectedPlayer(player);
    localStorage.selectedPlayerPid = player.pid;
  }

  function fetchAndDisplayPlayer() {
    if (!selectedPlayer) return;
    MnApi.getPlayer(selectedPlayer.pid, msgHandler, displayPlayer);
  }

  function displayPlayer(player) {
    if (selectedPlayer.pid === player.pid && player.item) {
      selectedPlayer = player; // Upgrade to add details.
    }

    $('#player_name').text(player.name + ' (' + HOST_NAME + ')');
    $('#queue_tab_icon').text(player.stateIcon);
    $('#subtitle_list_name').text(player.listTitle);
    $('#subtitle_playback_order').text(MnApi.PLAYBACK_ORDERS[player.playOrder]['title']);
    $('#subtitle_transcode').text(player.transcodeTitle);
    $('#track_title').text(player.trackTitle);
    $('#track_tags').text(player.tags.length > 0 ? player.tags.join(', ') : '(no tags)');
    $('#queue_info').text(player.queueLength + ' items, ' + MnApi.formatSeconds(player.queueDuration));

    var pos, dur, sldTxt, sldVal, sldMax;

    pos = player.position;
    if (!pos || pos < 0) pos = 0;

    if (player.item) dur = player.item.duration;
    if (!dur || dur <= 0) dur = player.duration;
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

    $('#track_progress').attr('max', sldMax).get(0).MaterialSlider.change(sldVal);
    $('#track_time').text(sldTxt);

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
    $('.title', itemEl).text(title);

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
    currentDbMid = null;
    currentDbQuery = null;
    currentDbResults = null;

    MnApi.getDbs(msgHandler, displayDbs);
    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function setDbTabToSearch(mid, view, query) {
    if (query) {
      $('#db_query').val(query).parent().addClass('is-dirty'); // FIXME https://github.com/google/material-design-lite/issues/903
    }
    else {
      query = $('#db_query').val();
    }

    var sortColumn = $('#db_sort_column').val();
    var sortOrder = $('#db_sort_order').val();

    currentDbMid = mid;
    currentDbQuery = query;
    currentDbResults = null;

    MnApi.getQuery(mid, view, query, sortColumn, sortOrder, msgHandler, displayResults);

    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.

    $('#db_go_back').unbind().click(function(){setDbTabToDbs()});
    $('#db_query').off('keyup').on('keyup', function(event) {
      onAutocompleteKeyup(event);
      if (event.keyCode == 13) {
        setDbTabToSearch(mid, view);
      }
    });
    $('#db_sort_column').unbind().change(function(){setDbTabToSearch(mid, view)});
    $('#db_sort_order').unbind().change(function(){setDbTabToSearch(mid, view)});
    $('#db_go_tags').unbind().click(function(){setDbTabToTags(mid, view)});
    $('#db_go_albums').unbind().click(function(){setDbTabToAlbums(mid, view)});
    $('#db_sort_options').show();
  }

  function setDbTabToTags(mid, view) {
    currentDbMid = mid;
    currentDbQuery = null;
    currentDbResults = null;

    MnApi.getTags(mid, view, msgHandler, displayTags);

    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function setDbTabToAlbums(mid, view) {
    currentDbMid = mid;
    currentDbQuery = null;
    currentDbResults = null;

    MnApi.getAlbums(mid, view, msgHandler, displayAlbums);

    $('#db_title').text('Fetching...');
    $('#db_list').empty();
    // TODO show spinner.
  }

  function displayDbs(dbs) {
    var dbList = $('#db_list');
    dbList.empty();
    var count = 0;
    $.each(dbs, function(index, db) {
      if (db.type === "remote") return true;
      dbList.append(makeDbItem(db));
      count += 1;
    });

    $('#db_sort_options').hide();
    $('#db_title').text(count + ' DBs');
  }

  function makeDbItem(db) {
    var a = $('<a class="clickable title" href="">');
    a.text(db.title);

    var el = $('<li class="item">');
    el.append(a);

    a.unbind().click(function(event) {
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
    var title = res.title;
    if (res.duration > 0) title += ' (' + MnApi.formatSeconds(res.duration) + ')';

    var a = $('<a class="clickable title" href="">');
    a.text(title);
    if (!res.enabled) a.addClass('disabled');

    var el = $('<li class="item">');
    el.append(a);

    a.unbind().click(function(event) {
      event.preventDefault();
      if (res.remoteId) {
        setDbTabToSearch(res.mid, res.view, res.remoteId);
      }
      else {
        showDbItemMenu(res, a);
      }
    });

    return el;
  }

  function displayTags(tags) {
    $('#db_title').text(tags.length + ' tags');

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
      setDbTabToSearch(tag.mid, tag.view, tag.value);
    });

    return el;
  }

  function displayAlbums(albums) {
    $('#db_title').text(albums.length + ' albums');

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
      showDbAlbumMenu(album, a);
    });

    return el;
  }

  function showDbItemMenu(item, anchorEl) {
    var menu = $('#db_item_menu');
    $('.title', menu).text(item.title);
    $('.stats', menu).text(item.startCount + '/' + item.endCount + ' ' + MnApi.formatSeconds(item.duration));
    $('.tags', menu).text(item.tags.join(', '));

    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(item, selectedPlayer.listView, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    $('.enqueue_top', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItemsTop(item, selectedPlayer.listView, selectedPlayer.pid, msgHandler, function(msg) {
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
        anchorEl.removeClass('disabled');
      });
      hidePopup(menu);
    });

    $('.set_disabled', menu).setVisibility(item.enabled).unbind().click(function(event) {
      MnApi.setEnabled(item, false, msgHandler, function(msg) {
        console.log(msg);
        item.enabled = false;
        anchorEl.addClass('disabled');
      });
      hidePopup(menu);
    });

    showPopup(menu);
  }

  function showDbAlbumMenu(album, anchorEl) {
    var menu = $('#db_album_menu');
    $('.title', menu).text(album.title);
    $('.stats', menu).text(album.trackCount + ' tracks');

    $('.enqueue', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItems(album, selectedPlayer.listView, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    $('.enqueue_top', menu).unbind().click(function(event) {
      if (!selectedPlayer) return;
      MnApi.enqueueItemsTop(album, selectedPlayer.listView, selectedPlayer.pid, msgHandler, function(msg) {
        console.log(msg);
        hidePopup(menu);
        fetchAndDisplayQueue();
      });
    });

    showPopup(menu);
  }

  function showTagEditor(item) {
    var dlg = $('#tag_editor');
    $('.title', dlg).text(item.title);

    var newTag = $('#new_tag', dlg);
    newTag.off('keyup').on('keyup', function(event) {
      onAutocompleteKeyup(event);
      if (event.keyCode == 13) {
        var tag = newTag.val();
        MnApi.addTag(item, tag, msgHandler, function(msg) {
          console.log(msg);
          item.tags.unshift(tag);
          showTagEditor(item); // TODO be less lazy.
          newTag.val('').focus();
        });
      }
    });

    tagEditorMid = item.mid;

    $('.row', dlg).remove();
    $.each(item.tags, function(index, tag) {
      var search = $('<button class="mdl-button mdl-js-button mdl-js-ripple-effect pri">');
      search.text(tag);
      search.unbind().click(function() {
        setDbTabToSearch(item.mid, item.view, tag);
        hidePopup(dlg);
        $('#fixed_tab_db span').click();
      });

      var remove = $('<button class="mdl-button mdl-js-button mdl-js-ripple-effect aux"><i class="material-icons">delete</i></button>');
      remove.unbind().click(function() {
        if (window.confirm("Tag: " + tag + "\n\nRemove?")) {
          MnApi.rmTag(item, tag, msgHandler, function(msg) {
            console.log(msg);
            item.tags = jQuery.grep(item.tags, function(val){return val != tag});
            showTagEditor(item); // TODO be less lazy.
            if (newTag.val().length < 1) newTag.val(tag).focus();
          });
        }
      });

      var row = $('<div class="row">');
      row.append(search);
      row.append(remove);

      dlg.append(row);
    });

    showPopup(dlg);
  }

})();
