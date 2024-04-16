## Web-socket routes documentation

### Join in the group chat

Message structure:

```
path=/chat/group/{chatId}/join
```

Response message example:

```json
{
  "action": "JOIN",
  "timestamp": 1710622097.792737500,
  "chatId": "65f605253b9b7c0d6dab5546",
  "senderId": 34
}
```

<br/>

### Leave from the group chat

Message structure:

```
path=/chat/group/{chatId}/leave
```

Response message is absent if you were a member of the chat.
In other case you will receive message with `ERROR` action:

```json
{
  "action": "ERROR",
  "timestamp": 1710622280.309209600,
  "error": "You are not member of chat",
  "path": "/chat/group/65fca83ea94fa31e8a726f9956/leave"
}
```

<br/>

### Send a text-message to the chat (`DUO` / `GROUP`)

Message structure is below. It is recommended to use UUID for temporary message id on the client.

```
path=/chat/{chatId}/send

{
  "tmpMessageId": "3ba88cab-8ca5-48e7-aff4-ba689fccee70",
  "text": "Hi everyone!"
}
```

At first, you receive the bind message:

```json
{
  "action": "BIND",
  "chatId": "65f605253b9b7c0d6dab5546",
  "tmpMessageId": "3ba88cab-8ca5-48e7-aff4-ba689fccee70",
  "messageId": "65f607523b9b7c0d6dab5547"
}
```

And then every member of the destination chat will receive your message:

```json
{
  "action": "MESSAGE",
  "timestamp": 1710622546.515123400,
  "id": "65f607523b9b7c0d6dab5547",
  "chatId": "65f605253b9b7c0d6dab5546",
  "senderId": 34,
  "text": "Hi everyone!"
}
```

But if you try to send message to the chat you are not member in,
you will get message with `ERROR` action:

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "You are not member of chat",
  "path": "/chat/65fca83ea94fa31e8a726f99/send"
}
```

<br/>

### Edit text-message (`DUO` / `GROUP`)

Message structure:

```
path=/chat/edit_text_message/{messageId}

[text]
```

Response message example:
```json
{
  "action": "UPD_MESSAGE",
  "timestamp": 1710622677.980591300,
  "senderId": 1,
  "chatId": "66054151e162397346b28554",
  "id": "66068a1a0f2b5c2763c23a67",
  "text": "update message from websockets"
}
```

Possible errors:

- Message not found

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "Requested message not found",
  "path": "/chat/edit_text_message/66068a1a0f2b5c2763c23a7"
}
```

- Not a member of chat or not a message sender

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "Do not have permissions to modify the message",
  "path": "/chat/edit_text_message/66068a1a0f2b5c2763c23a67"
}
```

<br/>

### Remove message (`DUO` / `GROUP`)

Message structure:

```
path=/chat/remove_message/{messageId}
```

Response message example:

```json
{
  "action": "REMOVE",
  "timestamp": 1712406700.330034700,
  "senderId": 1,
  "chatId": "66054151e162397346b28554",
  "id": "6611400eed7de1513ca11699"
}
```

<br/>

### Clear chat history (`DUO` / `GROUP`)

Message structure:

```
path=/chat/{chatId}/clear_history
```

Response message example:

```json
{
  "action": "CLEAR_HISTORY",
  "timestamp": 1712406700.330034700,
  "senderId": 1,
  "chatId": "66054151e162397346b28554"
}
```

<br/>

### Read messages (`DUO` / `GROUP`)

Message structure:

```
path=/chat/{chatId}/read

{
    "timestamp": 1712405998.170949000
}
```

Response message example:
```json
{
  "action": "READ",
  "timestamp": 1711664377.341578700,
  "senderId": 34,
  "chatId": "66054151e162397346b28554",
  "messages": [
    "6605e97a576f710ba44a3dd7",
    "6605ea09576f710ba44a3dd8",
    "6605ea0e576f710ba44a3dd9",
    "6605ea0e576f710ba44a3dda",
    "6605ea0f576f710ba44a3ddb"
  ]
}
```

Possible errors:

- Invalid chat ID

```json
{
  "action": "ERROR",
  "timestamp": 1711664377.341578700,
  "error": "You are not member of chat",
  "path": "/chat/6611450ced7de1513ca1169d/read"
}
```

<br/>

### Get unread messages (`DUO` / `GROUP`)

Message structure:

```
path=/chat/unread_messages
```

Response message example:

```json
{
  "action": "UNREAD_MESSAGES",
  "timestamp": 1711704048.542412300,
  "chats": {
    "66054151e162397346b28554": 3,
    "6605584bb6c9df73337beba3": 0
  }
}
```

<br/>