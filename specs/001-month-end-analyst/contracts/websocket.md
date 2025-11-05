# WebSocket Protocol Specification

**Version**: 1.0.0
**Last Updated**: 2025-10-30

## Overview

The Month-End Market Analyst WebSocket API provides real-time bidirectional communication for:
1. **Run State Updates**: Live progress updates during recommendation run execution
2. **Notifications**: In-app notifications for T-3, T-1, T events, data staleness alerts, and system messages

WebSocket connections complement the REST API by eliminating the need for polling when monitoring long-running operations and receiving timely user notifications.

## Connection Endpoints

### Run Status Stream

```
wss://api.stockmonitor.example.com/v1/ws/runs/{runId}/status
```

**Purpose**: Real-time updates on recommendation run execution progress and state transitions.

**Lifecycle**: Connection remains open until run reaches terminal state (FINALIZED, FAILED, CANCELLED) or client disconnects.

**Use Case**: Display live progress bar and status updates while user waits for recommendations.

---

### Notification Stream

```
wss://api.stockmonitor.example.com/v1/ws/notifications
```

**Purpose**: Real-time delivery of user-specific notifications (month-end workflow events, data alerts, system messages).

**Lifecycle**: Long-lived connection that remains open throughout user session.

**Use Case**: Display notification bell/badge with real-time count and toast messages for important events.

---

## Authentication

All WebSocket connections require JWT authentication.

### Connection Handshake

Include JWT access token in one of the following ways:

**Option 1: Query Parameter (Recommended for browser clients)**

```
wss://api.stockmonitor.example.com/v1/ws/runs/{runId}/status?token={JWT_ACCESS_TOKEN}
```

**Option 2: Sec-WebSocket-Protocol Header**

```javascript
const ws = new WebSocket(
  'wss://api.stockmonitor.example.com/v1/ws/runs/{runId}/status',
  ['bearer', JWT_ACCESS_TOKEN]
);
```

**Authentication Failure**

If token is invalid, expired, or missing, server closes connection with:
- Close code: `1008` (Policy Violation)
- Close reason: `"Authentication failed"`

---

## Message Format

All messages use JSON format.

### Message Structure

```json
{
  "type": "string",           // Message type identifier
  "timestamp": "ISO8601",     // Server timestamp (UTC)
  "data": {}                  // Type-specific payload
}
```

---

## Run Status Messages

### Message Types

#### 1. Connection Acknowledgment

Sent immediately after successful connection to confirm subscription.

```json
{
  "type": "connection_ack",
  "timestamp": "2025-10-30T14:30:00.000Z",
  "data": {
    "runId": "650e8400-e29b-41d4-a716-446655440000",
    "currentStatus": "RUNNING",
    "progressPercent": 35
  }
}
```

---

#### 2. Progress Update

Sent periodically (every 5-10 seconds) during run execution to update progress percentage.

```json
{
  "type": "progress_update",
  "timestamp": "2025-10-30T14:30:15.000Z",
  "data": {
    "runId": "650e8400-e29b-41d4-a716-446655440000",
    "progressPercent": 42,
    "currentStage": "Calculating factor scores",
    "stageDetail": "Processing Value metrics (125/500 symbols)"
  }
}
```

**Fields**:
- `progressPercent`: Overall progress (0-100)
- `currentStage`: Human-readable stage description
- `stageDetail`: Optional detailed progress within stage

---

#### 3. State Transition

Sent when run transitions between states (QUEUED → RUNNING → PRE_COMPUTE → STAGED → FINALIZED).

```json
{
  "type": "state_transition",
  "timestamp": "2025-10-30T14:38:00.000Z",
  "data": {
    "runId": "650e8400-e29b-41d4-a716-446655440000",
    "previousState": "RUNNING",
    "newState": "FINALIZED",
    "message": "Recommendations are ready. 12 picks generated.",
    "recommendationCount": 12,
    "exclusionCount": 28
  }
}
```

**Terminal States**: When run reaches FINALIZED, FAILED, or CANCELLED, server sends final state transition and closes connection gracefully (close code `1000 - Normal Closure`).

---

#### 4. Error Event

Sent when run encounters errors (validation failures, data issues, constraint conflicts).

```json
{
  "type": "error",
  "timestamp": "2025-10-30T14:35:00.000Z",
  "data": {
    "runId": "650e8400-e29b-41d4-a716-446655440000",
    "errorCode": "CONSTRAINT_CONFLICT",
    "errorMessage": "Constraints prevent a feasible recommendation set. Try relaxing turnover or liquidity floor.",
    "recoverable": false
  }
}
```

**Fields**:
- `errorCode`: Machine-readable error identifier
- `errorMessage`: Human-readable explanation
- `recoverable`: If `true`, run may retry; if `false`, run has failed permanently

---

#### 5. Heartbeat (Ping/Pong)

**Client → Server (Ping)**:
```json
{
  "type": "ping",
  "timestamp": "2025-10-30T14:30:00.000Z"
}
```

**Server → Client (Pong)**:
```json
{
  "type": "pong",
  "timestamp": "2025-10-30T14:30:00.500Z"
}
```

**Interval**: Client should send ping every 30 seconds. Server responds with pong.

**Timeout**: If server doesn't receive ping for 90 seconds or client doesn't receive pong for 60 seconds, connection is considered dead and should be closed/reconnected.

---

## Notification Stream Messages

### Message Types

#### 1. Connection Acknowledgment

```json
{
  "type": "connection_ack",
  "timestamp": "2025-10-30T14:30:00.000Z",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "unreadCount": 3
  }
}
```

---

#### 2. Notification Delivery

Sent when new notification is generated for user.

```json
{
  "type": "notification",
  "timestamp": "2025-10-30T14:30:00.000Z",
  "data": {
    "notificationId": "a50e8400-e29b-41d4-a716-446655440000",
    "category": "MONTH_END_T3",
    "priority": "MEDIUM",
    "title": "Month-End Scores Calculated",
    "message": "Factor scores calculated for Month-End. Review Analysis for any red flags.",
    "actionUrl": "/portfolios/650e8400-e29b-41d4-a716-446655440000/analysis",
    "actionLabel": "View Analysis",
    "expiresAt": "2025-11-05T14:30:00.000Z"
  }
}
```

**Notification Categories**:
- `MONTH_END_T3`: T-3 pre-compute notification
- `MONTH_END_T1`: T-1 staged recommendations notification
- `MONTH_END_T`: T finalized recommendations notification
- `DATA_STALE`: Data source staleness alert
- `RUN_COMPLETE`: Manual run completion
- `RUN_FAILED`: Run failure alert
- `SYSTEM`: System maintenance or important announcements

**Priority Levels**:
- `HIGH`: Urgent (data stale, run failed, security alerts)
- `MEDIUM`: Important (month-end events, run completions)
- `LOW`: Informational (tips, feature announcements)

---

#### 3. Notification Read Acknowledgment

**Client → Server** (mark notification as read):
```json
{
  "type": "mark_read",
  "timestamp": "2025-10-30T14:31:00.000Z",
  "data": {
    "notificationId": "a50e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Server → Client** (confirmation):
```json
{
  "type": "read_ack",
  "timestamp": "2025-10-30T14:31:00.100Z",
  "data": {
    "notificationId": "a50e8400-e29b-41d4-a716-446655440000",
    "unreadCount": 2
  }
}
```

---

#### 4. Bulk Mark All Read

**Client → Server**:
```json
{
  "type": "mark_all_read",
  "timestamp": "2025-10-30T14:32:00.000Z"
}
```

**Server → Client**:
```json
{
  "type": "all_read_ack",
  "timestamp": "2025-10-30T14:32:00.100Z",
  "data": {
    "markedCount": 3,
    "unreadCount": 0
  }
}
```

---

#### 5. Heartbeat (Ping/Pong)

Same as Run Status heartbeat protocol (30-second interval).

---

## Error Handling

### Connection Errors

| Close Code | Reason | Description | Action |
|------------|--------|-------------|--------|
| 1000 | Normal Closure | Clean connection close (run completed, user logout) | No action needed |
| 1001 | Going Away | Server shutting down for maintenance | Reconnect after delay |
| 1008 | Policy Violation | Authentication failed | Refresh JWT and reconnect |
| 1011 | Internal Error | Unexpected server error | Reconnect with exponential backoff |
| 4001 | Invalid Run ID | Run does not exist or user lacks access | Display error, do not reconnect |
| 4002 | Token Expired | JWT expired during connection | Refresh JWT and reconnect |
| 4003 | Rate Limit Exceeded | Too many connections from same user | Wait 60s before reconnecting |

---

### Message Parse Errors

If client sends malformed JSON or invalid message type:

```json
{
  "type": "error",
  "timestamp": "2025-10-30T14:30:00.000Z",
  "data": {
    "errorCode": "INVALID_MESSAGE",
    "errorMessage": "Message type 'invalid_type' is not recognized"
  }
}
```

Server does not close connection for parse errors; client can continue sending valid messages.

---

## Reconnection Strategy

### Exponential Backoff

When connection drops unexpectedly, client should implement exponential backoff:

1. **Initial Delay**: 1 second
2. **Max Delay**: 60 seconds
3. **Backoff Factor**: 2x
4. **Max Attempts**: 10

```javascript
let reconnectDelay = 1000; // Start at 1 second
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;
const MAX_DELAY = 60000; // 60 seconds

function reconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('Max reconnection attempts reached');
    return;
  }

  setTimeout(() => {
    console.log(`Reconnecting... Attempt ${reconnectAttempts + 1}`);
    connectWebSocket();
    reconnectAttempts++;
    reconnectDelay = Math.min(reconnectDelay * 2, MAX_DELAY);
  }, reconnectDelay);
}
```

---

### Reconnection Triggers

**Reconnect immediately** (no backoff) for:
- Token refresh (close code 4002)
- Server maintenance completion (close code 1001)

**Reconnect with backoff** for:
- Network errors
- Unexpected disconnections
- Internal server errors (close code 1011)

**Do not reconnect** for:
- Authentication failures (close code 1008)
- Invalid run ID (close code 4001)
- Normal closure (close code 1000) after run completes

---

## Rate Limiting

### Connection Limits

- **Per User**: Maximum 5 concurrent WebSocket connections
- **Per Run**: Maximum 3 concurrent connections to same run status endpoint
- **Violation**: Close connection with code `4003` (Rate Limit Exceeded)

### Message Rate Limits

- **Client Messages**: Maximum 10 messages per minute per connection
- **Server Messages**: No limit (server controls message frequency)
- **Violation**: Server sends error message and continues connection (does not close)

---

## Example Message Flows

### Flow 1: Manual Run Monitoring

**User triggers manual run via REST API**:
```
POST /api/portfolios/{portfolioId}/runs
→ 202 Accepted: {"runId": "650e8400...", "status": "QUEUED"}
```

**User opens WebSocket connection**:
```javascript
const ws = new WebSocket(
  'wss://api.stockmonitor.example.com/v1/ws/runs/650e8400.../status?token=...'
);
```

**Server sends connection acknowledgment**:
```json
{"type": "connection_ack", "data": {"runId": "650e8400...", "currentStatus": "QUEUED", "progressPercent": 0}}
```

**Run starts executing**:
```json
{"type": "state_transition", "data": {"previousState": "QUEUED", "newState": "RUNNING", "message": "Analysis started"}}
```

**Progress updates (every 10 seconds)**:
```json
{"type": "progress_update", "data": {"progressPercent": 15, "currentStage": "Fetching market data"}}
{"type": "progress_update", "data": {"progressPercent": 35, "currentStage": "Calculating factor scores"}}
{"type": "progress_update", "data": {"progressPercent": 68, "currentStage": "Applying constraints"}}
{"type": "progress_update", "data": {"progressPercent": 92, "currentStage": "Generating recommendations"}}
```

**Run completes**:
```json
{"type": "state_transition", "data": {"previousState": "RUNNING", "newState": "FINALIZED", "message": "Recommendations are ready. 12 picks generated.", "recommendationCount": 12}}
```

**Server closes connection** (close code 1000 - Normal Closure).

---

### Flow 2: Notification Delivery

**User connects to notification stream**:
```javascript
const ws = new WebSocket(
  'wss://api.stockmonitor.example.com/v1/ws/notifications?token=...'
);
```

**Server acknowledges connection**:
```json
{"type": "connection_ack", "data": {"userId": "550e8400...", "unreadCount": 2}}
```

**Month-end T-3 event occurs (scheduled job)**:
```json
{
  "type": "notification",
  "data": {
    "notificationId": "a50e8400...",
    "category": "MONTH_END_T3",
    "title": "Month-End Scores Calculated",
    "message": "Factor scores calculated for Month-End. Review Analysis for any red flags.",
    "actionUrl": "/portfolios/650e8400.../analysis",
    "actionLabel": "View Analysis"
  }
}
```

**User clicks notification, UI marks it read**:
```json
{"type": "mark_read", "data": {"notificationId": "a50e8400..."}}
```

**Server confirms**:
```json
{"type": "read_ack", "data": {"notificationId": "a50e8400...", "unreadCount": 2}}
```

**Data source becomes stale**:
```json
{
  "type": "notification",
  "data": {
    "notificationId": "b50e8400...",
    "category": "DATA_STALE",
    "priority": "HIGH",
    "title": "Data Source Stale",
    "message": "Fundamentals data is stale (last update 3 days ago). Monthly run blocked until refreshed.",
    "actionUrl": "/data-sources",
    "actionLabel": "Check Status"
  }
}
```

**Connection remains open** until user logs out or closes browser tab.

---

### Flow 3: Connection Interruption and Reconnection

**Network error causes disconnection**:
```javascript
ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = (event) => {
  if (event.code !== 1000) { // Not a normal closure
    console.log('Unexpected disconnection, reconnecting...');
    reconnect();
  }
};
```

**Client reconnects with exponential backoff**:
```
Attempt 1: Wait 1s → Connect
Attempt 2: Wait 2s → Connect
Attempt 3: Wait 4s → Connect (success)
```

**Server sends current state upon reconnection**:
```json
{"type": "connection_ack", "data": {"runId": "650e8400...", "currentStatus": "RUNNING", "progressPercent": 48}}
```

**Client resumes receiving progress updates**.

---

## Security Considerations

### JWT Token Management

- **Token in Query String**: Query parameter tokens are logged in server access logs. Use HTTPS to encrypt transport.
- **Token Expiry**: JWT access tokens expire after 1 hour. Client should refresh token before expiry and reconnect.
- **Token Revocation**: If user logs out or token is revoked, server closes all WebSocket connections with close code 1008.

### Connection Authorization

- **Run Status Endpoint**: User must have access to portfolio associated with run. Verified during connection handshake.
- **Notification Endpoint**: Only delivers notifications belonging to authenticated user. No filtering needed client-side.

### Message Validation

- **Client Messages**: All client-originated messages are validated. Invalid messages trigger error response but do not close connection.
- **Server Messages**: Always well-formed JSON. Client should validate message type before processing.

---

## Client Implementation Examples

### JavaScript (Browser)

```javascript
class RunStatusWebSocket {
  constructor(runId, accessToken) {
    this.runId = runId;
    this.accessToken = accessToken;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.reconnectDelay = 1000;
    this.heartbeatInterval = null;
  }

  connect() {
    const url = `wss://api.stockmonitor.example.com/v1/ws/runs/${this.runId}/status?token=${this.accessToken}`;
    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.reconnectDelay = 1000;
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = (event) => {
      console.log(`WebSocket closed: ${event.code} - ${event.reason}`);
      this.stopHeartbeat();

      if (event.code !== 1000 && event.code !== 4001) {
        this.reconnect();
      }
    };
  }

  handleMessage(message) {
    switch (message.type) {
      case 'connection_ack':
        console.log('Connection acknowledged:', message.data);
        break;
      case 'progress_update':
        this.updateProgress(message.data.progressPercent, message.data.currentStage);
        break;
      case 'state_transition':
        this.updateState(message.data.newState, message.data.message);
        break;
      case 'error':
        this.showError(message.data.errorMessage);
        break;
      case 'pong':
        // Heartbeat acknowledged
        break;
      default:
        console.warn('Unknown message type:', message.type);
    }
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'ping',
          timestamp: new Date().toISOString()
        }));
      }
    }, 30000); // 30 seconds
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  reconnect() {
    if (this.reconnectAttempts >= 10) {
      console.error('Max reconnection attempts reached');
      return;
    }

    setTimeout(() => {
      console.log(`Reconnecting... Attempt ${this.reconnectAttempts + 1}`);
      this.connect();
      this.reconnectAttempts++;
      this.reconnectDelay = Math.min(this.reconnectDelay * 2, 60000);
    }, this.reconnectDelay);
  }

  disconnect() {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
    }
  }

  updateProgress(percent, stage) {
    // Update UI progress bar and stage label
    document.getElementById('progress-bar').style.width = `${percent}%`;
    document.getElementById('stage-label').textContent = stage;
  }

  updateState(newState, message) {
    // Update UI run state badge
    document.getElementById('run-status').textContent = newState;
    if (newState === 'FINALIZED') {
      // Show success message, enable "View Recommendations" button
    }
  }

  showError(errorMessage) {
    // Display error toast/modal
    alert(errorMessage);
  }
}

// Usage
const runStatusWs = new RunStatusWebSocket('650e8400-e29b-41d4-a716-446655440000', jwtAccessToken);
runStatusWs.connect();
```

---

### React Hook Example

```javascript
import { useEffect, useState, useRef } from 'react';

export function useRunStatusWebSocket(runId, accessToken) {
  const [status, setStatus] = useState(null);
  const [progress, setProgress] = useState(0);
  const [stage, setStage] = useState('');
  const [error, setError] = useState(null);
  const wsRef = useRef(null);

  useEffect(() => {
    if (!runId || !accessToken) return;

    const url = `wss://api.stockmonitor.example.com/v1/ws/runs/${runId}/status?token=${accessToken}`;
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);

      switch (message.type) {
        case 'connection_ack':
          setStatus(message.data.currentStatus);
          setProgress(message.data.progressPercent);
          break;
        case 'progress_update':
          setProgress(message.data.progressPercent);
          setStage(message.data.currentStage);
          break;
        case 'state_transition':
          setStatus(message.data.newState);
          break;
        case 'error':
          setError(message.data.errorMessage);
          break;
      }
    };

    // Heartbeat
    const heartbeat = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping', timestamp: new Date().toISOString() }));
      }
    }, 30000);

    return () => {
      clearInterval(heartbeat);
      ws.close();
    };
  }, [runId, accessToken]);

  return { status, progress, stage, error };
}

// Usage in component
function RunMonitor({ runId }) {
  const { status, progress, stage, error } = useRunStatusWebSocket(runId, getAccessToken());

  return (
    <div>
      <div>Status: {status}</div>
      <div>Progress: {progress}%</div>
      <div>Stage: {stage}</div>
      {error && <div className="error">{error}</div>}
    </div>
  );
}
```

---

## Testing and Debugging

### Testing Tools

**wscat** (command-line WebSocket client):
```bash
npm install -g wscat
wscat -c "wss://api.stockmonitor.example.com/v1/ws/runs/650e8400.../status?token=YOUR_JWT"
```

**Browser DevTools**:
- Chrome/Edge: Network tab → WS filter → Click connection → Messages tab
- Firefox: Network tab → WS → Click connection → Response tab

### Debug Logging

Enable verbose WebSocket logging:
```javascript
ws.addEventListener('open', () => console.log('WS Open'));
ws.addEventListener('close', (e) => console.log('WS Close', e.code, e.reason));
ws.addEventListener('error', (e) => console.error('WS Error', e));
ws.addEventListener('message', (e) => console.log('WS Message', e.data));
```

---

## Versioning and Compatibility

- **Protocol Version**: Included in connection acknowledgment message for future compatibility checks
- **Backward Compatibility**: New message types are additive. Clients should ignore unknown message types.
- **Breaking Changes**: Will be introduced in new WebSocket endpoint path (e.g., `/v2/ws/...`)

---

## Monitoring and Observability

### Server-Side Metrics

- Active WebSocket connections (by endpoint, by user)
- Message throughput (messages/second)
- Connection duration (average, p95, p99)
- Reconnection rate
- Error rate by close code

### Client-Side Metrics

Recommended client telemetry:
- Connection success/failure rate
- Average time to connection acknowledgment
- Message delivery latency (server timestamp → client receive time)
- Reconnection attempts and delays

---

## FAQ

**Q: Should I use WebSocket or polling for run status?**

A: Use WebSocket for active monitoring (user watching progress). Use REST polling for background checks (e.g., checking if run completed while user was on different page).

---

**Q: What happens if I lose connection during a run?**

A: Run continues executing on server. Reconnect to resume receiving updates. Connection acknowledgment includes current state.

---

**Q: Can I have multiple tabs open with same WebSocket connection?**

A: Yes, each tab can open independent connection. Limited to 5 concurrent connections per user.

---

**Q: Do WebSocket messages guarantee delivery?**

A: WebSocket provides reliable delivery over TCP. However, if client disconnects, messages sent during disconnection are lost. Reconnect and use connection acknowledgment to get current state.

---

**Q: How do I handle token expiry during long-lived notification connection?**

A: Refresh token before expiry (tokens expire after 1 hour). When close code 4002 (Token Expired) is received, refresh token and reconnect immediately.

---

## Related Documentation

- REST API Specification: `rest-api.yaml`
- Data Feeds Integration: `data-feeds.md`
- Authentication Guide: See `/auth` endpoints in REST API spec
