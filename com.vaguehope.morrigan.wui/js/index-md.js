(function() {

  var REFRESH_PLAYERS_SECONDS = 5;
  var HOST_NAME;

  var selectedPlayerPid;

  $(document).ready(function() {
    updatePageTitle();
    startPoller();
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

  function fetchAndDisplayPlayers() {
    MnApi.getPlayers(function(msg) {
      if (msg && msg.length > 0) console.log(msg);
    }, displayPlayers);
  }

  function displayPlayers(players) {
    $.each(players, function(index, player) {
      if (!selectedPlayerPid) setSelectedPlayer(player); // TODO also if player gone.

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

  function setSelectedPlayer(player) {
    selectedPlayerPid = player.pid;
    fetchAndDisplayPlayer();
  }

  function fetchAndDisplayPlayer() {
    if (!selectedPlayerPid) return;
    MnApi.getPlayer(selectedPlayerPid, function(msg) {
      if (msg && msg.length > 0) console.log(msg);
    }, displayPlayer);
  }

  function displayPlayer(player) {
    $('#player_name').text(player.name + ' (' + HOST_NAME + ')');
    $('#queue_tab_icon').text(player.stateIcon);
    //$('.order', playerDiv).text(player.playOrderTitle);
    //$('.list', playerDiv).text(player.listTitle);
    $('#track_title').text(player.trackTitle + ' (' + player.trackDuration + 's)');
    $('#track_tags').text(player.tags.length > 0 ? player.tags.join(', ') : '(no tags)');
    $('#queue_info').text(player.queueLength + ' items, ' + player.queueDuration + 's');

    fetchAndDisplayQueue(); // TODO only do this if queue in player has changed.
  }

  function fetchAndDisplayQueue() {
    if (!selectedPlayerPid) return;
    MnApi.getQueue(selectedPlayerPid, function(msg) {
      if (msg && msg.length > 0) console.log(msg);
    }, displayQueue);
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
      console.log('clicked', item);
    });
  }

})();
