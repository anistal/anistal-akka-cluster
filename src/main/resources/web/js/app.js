window.onload = function() {
  var socket = new WebSocket('ws://localhost:8080/ws');

  socket.onopen = function(event) {
    console.info("connected!")
  }

  socket.onmessage = function(event) {
    var message = event.data;
    console.info(message);
  };
};