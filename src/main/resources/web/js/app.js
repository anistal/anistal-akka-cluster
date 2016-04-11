window.onload = function() {

  setupData(wsURL("subscribe/main"), "main");
  setupMenuHandler();

  function setupMenuHandler() {
    $("li#cluster").on("click", function(event) {
      setupData(wsURL("events"), "events");
    });

    $("li#home").on("click", function(event) {
      setupData(wsURL("subscribe/main"), "main");
    })
  };

  function setupData(endpoint, term) {
     $("div#content").empty();
    if(endpoint.indexOf("events") == -1) {
      $.ajax({
        type: 'GET',
        url: 'last10/' + term,
        dataType: 'json',
        success: function(data) {
          data.forEach(function(item) {
            console.log(item);
            createTwitterDiv(item);
          });
          setupWebSocket(endpoint, term);
        }
      });
    } else {
      setupWebSocket(endpoint, term);
    }
  };

  function setupWebSocket(endpoint) {
    if(window.ws) {
      window.ws.close();
    }

    var ws = new WebSocket(endpoint)

    ws.onopen = function(event) {
      console.info("Connected to the server")

      if(window.timeout) {
        clearTimeout(window.timeout)
      }

      window.timeout = setTimeout(function() {
        ws.send("ping")
        console.log("ping")
      }, 3000);
    };

    ws.onmessage = function(event) {
      var message = event.data;
      var obj = JSON.parse(message);

      if(this.url.indexOf("subscribe") != -1) {
        createTwitterDiv(obj);
      } else {
        createEventDiv(obj);
      }
    };

    ws.onclose = function() {
      console.info("Disconnected to the server");
      setupWebSocket(this.url)
    };

    window.ws = ws;
  }

  function wsURL(path) {
    var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    var url = protocol + location.host;
    if(location.hostname === 'localhost') {
      url += '/' + location.pathname.split('/')[1];
    } else {
      url += '/';
    }
    return url + path;
  };

  function createTwitterDiv(obj) {
    var newDiv = "";
    newDiv += '<div id="' + obj.id + '" class="alert alert-info" role="alert">';
    newDiv += '<a class="btn btn-default" href="#" role="button" id="button-' + obj.id + '">' + obj.keyword + '</a>';
    newDiv += '<p>' + obj.text + '</p>';
    newDiv += '</div>';
    $("div#content").prepend(newDiv);

    $('#button-' + obj.id).click(function(){
      var text = ($(this).text());
      setupData(wsURL("/subscribe/" + text), text);
    });
  };

  function createEventDiv(obj) {
    var newDiv = "";
    newDiv += '<div class="alert alert-info" role="alert">';
    newDiv += '<p>' + obj.message + '</p>';
    newDiv += "</div>";
    $("div#content").prepend(newDiv);
  };
};