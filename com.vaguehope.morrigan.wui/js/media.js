(function() {

  var mlistsStatusBar;

  $(document).ready(function() {
    initStatusBars();
    var mlistsDiv = $('.mlists');
    var mid = UrlParams.params['mid'];
    var pid = UrlParams.params['pid'];
    if (mid) {
      initMlist(mlistsDiv, mid);
    }
    else if (!pid) {
      initMlists(mlistsDiv);
    }
  });

  function initStatusBars() {
    mlistsStatusBar = $('<span>');
    $('.statusbar').append(mlistsStatusBar);
  }

  function initMlists(mlistsDiv) {
    mlistsDiv.empty();
    updateMlists(mlistsDiv);
  }

  function initMlist(mlistsDiv, pid) {
    mlistsDiv.empty();
    updateMlist(mlistsDiv, pid);
  }

  function updateMlists(mlistsDiv) {
    getMlists(function(msg) {
      mlistsStatusBar.text(msg);
    }, function(mlists) {
      $.each(mlists, function(index, mlist) {
        displayMlist(mlistsDiv, mlist, false);
      });
    });
  }

  function updateMlist(mlistsDiv, pid) {
    getMlist(pid, function(msg) {
      mlistsStatusBar.text(msg);
    }, function(mlist) {
      displayMlist(mlistsDiv, mlist, true);
    });
  }

  function displayMlist(mlistsDiv, mlist, detailed) {
    var mlistDivId = 'mlist_' + mlist.id;
    var mlistDiv = $('#' + mlistDivId, mlistsDiv);
    if (mlistDiv.size() < 1) {
      mlistDiv = $('<div class="mlist">');
      mlistDiv.attr('id', mlistDivId);
      makeMlist(mlistDiv, mlist.mid, detailed);
      mlistsDiv.append(mlistDiv);
    }
    updateMlistDisplay(mlistDiv, mlist, detailed);
  }

  function updateMlistDisplay(mlistDiv, mlist, detailed) {
    $('.title', mlistDiv).text(mlist.title);
    if (detailed === true) {
      $('.stats', mlistDiv).text(mlist.count + ' items, ' + (mlist.durationComplete === true ? '' : 'more than ') + mlist.duration + ' seconds.');
    }
  }

  function makeMlist(mlistDiv, mid, detailed) {
    mlistDiv.empty();

    var textBlock = $('<div class="block text">');
    mlistDiv.append(textBlock);

    if (detailed === false) {
      mlistDiv.append($('<a class="details" href="?mid=' + mid + '">'));
    }

    var mainRow = $('<p class="mainrow">');
    textBlock.append(mainRow);
    mainRow.append($('<span class="title">'));
    
    
    if (detailed === true) {
      var statsRow =  $('<p class="statsrow">');
      textBlock.append(statsRow);
      statsRow.append($('<span class="stats">'));
    }
  }

  function getMlists(onStatus, onMlists) {
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists',
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading media lists...');
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
        onMlists(mlists);
        onStatus('Media lists updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching media lists: ' + textStatus);
      }
    });
  }

  function getMlist(mid, onStatus, onMlist) {
    var id = midToId(mid);
    $.ajax({
      type : 'GET',
      cache : false,
      url : 'mlists/' + mid,
      dataType : 'xml',
      beforeSend : function() {
        onStatus('Reading media list ' + id + '...');
      },
      success : function(xml) {
        var mlistNode = $(xml).find('mlist');
        var mlist = parseMlistNode(mlistNode);
        onMlist(mlist);
        onStatus('Media list ' + id + ' updated.');
      },
      error : function(jqXHR, textStatus, errorThrown) {
        onStatus('Error fetching media list ' + id + ': ' + textStatus);
      }
    });
  }

  function parseMlistNode(node) {
    var mlist = {};
    mlist.mid = node.find('link[rel="self"]').attr('href').replace('/mlists/', '');
    mlist.id = midToId(mlist.mid);
    mlist.title = node.find('title').text();
    mlist.count = parseInt(node.find('count').text());
    mlist.duration = parseInt(node.find('duration').text());
    mlist.durationComplete = (node.find('durationcomplete').text() === 'true');
    return mlist;
  }
  
  function midToId(mid) {
    return mid.match(/\/(.+?)\./)[1];
  }

})();
