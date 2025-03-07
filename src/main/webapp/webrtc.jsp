<!DOCTYPE html>
<html>
<head>
  <title>WebRTC Data Channel Example</title>
</head>
<body>
<h1>WebRTC Data Channel Example</h1>

<textarea id="messageInput" placeholder="Enter message"></textarea>
<button id="sendButton" disabled>Send</button>
<div id="messages"></div>

<script>
  const messageInput = document.getElementById('messageInput');
  const sendButton = document.getElementById('sendButton');
  const messagesDiv = document.getElementById('messages');

  let peerConnection;
  let dataChannel;

  // --- Signaling (WebSocket) ---
  const websocket = new WebSocket("ws://localhost:8080/ws/webrtc"); // Adjust URL

  websocket.onopen = () => {
    console.log("WebSocket connected");
  };

  websocket.onclose = () => {
    console.log("WebSocket closed");
  };

  websocket.onerror = (error) => {
    console.error("WebSocket error:", error);
  };

  websocket.onmessage = async (event) => {
    const message = JSON.parse(event.data);

    if (message.type === 'offer') {
      if (!peerConnection) {
        createPeerConnection();
      }
      await peerConnection.setRemoteDescription(new RTCSessionDescription(message.offer));
      const answer = await peerConnection.createAnswer();
      await peerConnection.setLocalDescription(answer);
      sendAnswer(answer);
    } else if (message.type === 'answer') {
      await peerConnection.setRemoteDescription(new RTCSessionDescription(message.answer));
    } else if (message.type === 'iceCandidate') {
      try {
        await peerConnection.addIceCandidate(message.candidate);
      } catch (e) {
        console.error("Error adding ice candidate: ", e);
      }
    }
  };

  function sendOffer(offer) {
    websocket.send(JSON.stringify({ type: 'offer', offer: offer }));
  }

  function sendAnswer(answer) {
    websocket.send(JSON.stringify({ type: 'answer', answer: answer }));
  }

  function sendIceCandidate(candidate) {
    websocket.send(JSON.stringify({ type: 'iceCandidate', candidate: candidate }));
  }

  function createPeerConnection() {
    peerConnection = new RTCPeerConnection();

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        sendIceCandidate(event.candidate);
      }
    };

    peerConnection.ondatachannel = (event) => {
      dataChannel = event.channel;
      setupDataChannel();
    };
  }

  function createDataChannel() {
    dataChannel = peerConnection.createDataChannel('chat');
    setupDataChannel();
  }

  function setupDataChannel() {
    dataChannel.onopen = () => {
      console.log('Data channel opened');
      console.log("dataChannel.readyState: ", dataChannel.readyState);
      sendButton.disabled = false; // Enable the send button after the channel opens
    };

    dataChannel.onmessage = (event) => {
      const receivedMessage = event.data;
      messagesDiv.innerHTML += `<p>Received: ${receivedMessage}</p>`;
    };

    dataChannel.onclose = () => {
      console.log('Data channel closed');
      sendButton.disabled = true; // Disable button when channel is closed
    };

    dataChannel.onerror = (error) => {
      console.error('Data channel error: ', error);
    };
  }

  sendButton.addEventListener('click', () => {
    const message = messageInput.value;
    console.log("dataChannel.readyState: ", dataChannel.readyState);
    if (dataChannel && dataChannel.readyState === 'open') {
      try {
        dataChannel.send(message);
        messagesDiv.innerHTML += `<p>Sent: ${message}</p>`;
        messageInput.value = '';
      } catch (error) {
        console.error("Error sending message:", error);
      }
    } else {
      console.error('Data channel not open');
    }
  });

  // Initiate call and create data channel
  async function startCall() {
    createPeerConnection();

    // Check if the peer connection is established before creating the data channel
    if (peerConnection) {
      createDataChannel();  // This will now create a data channel if the connection exists
    }

    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    sendOffer(offer);
  }

  startCall(); // Start the call immediately.

</script>
</body>
</html>

<%--import Ember from 'ember';--%>

<%--export default Ember.Component.extend({--%>
<%--didInsertElement() {--%>
<%--this._super(...arguments);--%>
<%--document.title = this.get('title');--%>
<%--}--%>
<%--});--%>
